package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.*;

/**
 * Created by mcrass on 19.07.2018.
 */
public class TradeManager implements ParseListener {
    private static TradeManager instance;

    private PoeApiParser poeApiParser;
    private PoeTradeWebParser webParser;
    private ObservableList<CurrencyDeal> currentDeals;
    private ObservableList<CurrencyDeal> playerDeals;
    private PoeNinjaParser poeNinjaParser;

    private Comparator<? super CurrencyDeal> diffValueSorter = new Comparator<CurrencyDeal>() {
        @Override
        public int compare(CurrencyDeal o1, CurrencyDeal o2) {
            float o1Value = o1.getDiffValue();
            float o2Value = o2.getDiffValue();

            if (o1Value == o2Value) {
                return 0;
            }
            return o1Value > o2Value ? -1 : 1;
        }
    };
    private Comparator<? super CurrencyDeal> playerDiffValueSorter = new Comparator<CurrencyDeal>() {
        @Override
        public int compare(CurrencyDeal o1, CurrencyDeal o2) {
            float o1Value = o1.getPlayerDiffValue();
            float o2Value = o2.getPlayerDiffValue();

            if (o1Value == o2Value) {
                return 0;
            }
            return o1Value > o2Value ? -1 : 1;
        }
    };
    private ParseListener listener;

    public TradeManager() {
        poeApiParser = new PoeApiParser();
        poeNinjaParser = new PoeNinjaParser();
        webParser = new PoeTradeWebParser(this);

        currentDeals = FXCollections.observableArrayList();
        playerDeals = FXCollections.observableArrayList();
    }

    public static TradeManager getInstance() {
        if (instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }

    public void updateOffers(boolean clear) {
        updateOffers(clear, true);
    }

    public void updateOffers(boolean clear, boolean async) {
        webParser.updateCurrencies(PropertyManager.getInstance().getFilterList(), clear, async);
    }

    public void updatePlayerOffers() {
        List<CurrencyID> list = new LinkedList<>();
        for (CurrencyDeal deal : playerDeals) {
            list.add(deal.getSecondaryCurrencyID());
        }
        webParser.updateCurrencies(list, false);
    }

    public void updateOffersForCurrency(CurrencyID secondaryCurrencyID) {
        webParser.updateCurrency(secondaryCurrencyID, true);
    }

    public void parseDeals(boolean async) {
        // Cannot really be async, since working on observable lists
        if (async) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    parseDeals();
                }
            });
        } else {
            parseDeals();
        }
    }

    private void parseDeals() {
        CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
        currentDeals.clear();
        playerDeals.clear();

        HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> marketOffers = webParser.getCurrentOffers();
        HashSet<Pair<CurrencyID, CurrencyID>> processedKeys = new HashSet<>();

        if (marketOffers == null || marketOffers.isEmpty()) {
            LogManager.getInstance().log(getClass(), "No offers to parse.");
            return;
        }

        LogManager.getInstance().log(getClass(), "Parsing offers...");

        // Iterate through all combinations, check inverted offer, but every pair only once
        for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : marketOffers.entrySet()) {
            Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
            Pair<CurrencyID, CurrencyID> invertedKey = new Pair<>(key.getValue(), key.getKey());

            // If other way already processed
            if (processedKeys.contains(invertedKey)) {
                continue;
            }

            processedKeys.add(key);

            CurrencyOffer bestMarketSellOffer;
            CurrencyOffer bestMarketBuyOffer;

            if (key.getKey() != primaryCurrency) {
                // Check if offer actually contains primary
                if (key.getValue() != primaryCurrency) {
                    continue;
                }

                //Buy offer -> Sell offer
                key = invertedKey;
                invertedKey = offerMap.getKey();
            }

            CurrencyID secondaryCurrency = key.getValue();
            float cValue = poeNinjaParser.getCurrentCValueFor(secondaryCurrency);

            bestMarketSellOffer = getBestOffer(marketOffers.get(key));

            bestMarketBuyOffer = getBestOffer(marketOffers.get(invertedKey));

            int totalOffers = (marketOffers.get(key) == null ? 0 : marketOffers.get(key).size())
                    + (marketOffers.get(invertedKey) == null ? 0 : marketOffers.get(invertedKey).size());

            float marketSellPrice = 0;
            if (bestMarketSellOffer != null) {
                marketSellPrice = bestMarketSellOffer.getSellAmount();
                marketSellPrice /= bestMarketSellOffer.getBuyAmount();
            }

            float marketBuyPrice = 0;
            if (bestMarketBuyOffer != null) {
                marketBuyPrice = bestMarketBuyOffer.getBuyAmount();
                marketBuyPrice /= bestMarketBuyOffer.getSellAmount();
            }

            currentDeals.add(new CurrencyDeal(primaryCurrency, secondaryCurrency, cValue, totalOffers,
                    marketBuyPrice,
                    marketSellPrice));
        }
        currentDeals.sort(diffValueSorter);

        // Parse Players
        HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers = webParser.getPlayerOffers();
        HashSet<Pair<CurrencyID, CurrencyID>> processedPlayerKeys = new HashSet<>();
        for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : playerOffers.entrySet()) {
            Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
            Pair<CurrencyID, CurrencyID> invertedKey = new Pair<>(key.getValue(), key.getKey());

            // If other way already processed
            if (processedPlayerKeys.contains(invertedKey)) {
                continue;
            }

            processedPlayerKeys.add(key);

            CurrencyOffer bestMarketSellOffer;
            CurrencyOffer bestMarketBuyOffer;
            CurrencyOffer playerSellOffer = null;
            CurrencyOffer playerBuyOffer = null;

            if (key.getKey() != primaryCurrency) {
                // Check if offer actually contains primary
                if (key.getValue() != primaryCurrency) {
                    continue;
                }
                //Buy offer -> Sell offer
                key = invertedKey;
                invertedKey = offerMap.getKey();
            }

            CurrencyID secondaryCurrency = key.getValue();

            if (playerOffers.get(key) != null) {
                playerSellOffer = playerOffers.get(key).get(0);
            }

            List<CurrencyOffer> playerBuyOfferList = playerOffers.get(invertedKey);
            if (playerBuyOfferList != null) {
                playerBuyOffer = playerBuyOfferList.get(0);
            }

            bestMarketSellOffer = getBestOffer(marketOffers.get(key));

            bestMarketBuyOffer = getBestOffer(marketOffers.get(invertedKey));

            float playerSellPrice = 0;
            float playerBuyPrice = 0;
            int playerSellStock = 0;
            int playerBuyStock = 0;
            float marketSellPrice = 0;
            float marketBuyPrice = 0;

            if (bestMarketSellOffer != null) {
                marketSellPrice = bestMarketSellOffer.getSellAmount();
                marketSellPrice /= bestMarketSellOffer.getBuyAmount();
            }

            if (bestMarketBuyOffer != null) {
                marketBuyPrice = bestMarketBuyOffer.getBuyAmount();
                marketBuyPrice /= bestMarketBuyOffer.getSellAmount();
            }

            if (playerSellOffer != null) {
                playerSellPrice = playerSellOffer.getSellAmount();
                playerSellPrice /= playerSellOffer.getBuyAmount();

                playerSellStock = playerSellOffer.getStock();
            }

            if (playerBuyOffer != null) {
                playerBuyPrice = playerBuyOffer.getBuyAmount();
                playerBuyPrice /= playerBuyOffer.getSellAmount();

                playerBuyStock = playerBuyOffer.getStock();
            }

            float cValue = poeNinjaParser.getCurrentCValueFor(secondaryCurrency);
            int totalOffers = -1;

            if (playerBuyPrice > 0 || playerSellPrice > 0) {
                playerDeals.add(new CurrencyDeal(primaryCurrency, secondaryCurrency, cValue, totalOffers, marketBuyPrice,
                        marketSellPrice, playerBuyPrice, playerSellPrice, playerBuyStock, playerSellStock));
            } else {
                LogManager.getInstance().log(getClass(), "Player offer didnt contain shit!");
            }
        }
        playerDeals.sort(playerDiffValueSorter);
        LogManager.getInstance().log(getClass(), "Finished");
    }

    public CurrencyOffer getBestOffer(List<CurrencyOffer> list) {
        return getBestOffer(list,
                PropertyManager.getInstance().getFilterNoApi(),
                PropertyManager.getInstance().getFilterOutOfStock(),
                PropertyManager.getInstance().getFilterExcessive());
    }

    public CurrencyOffer getBestOffer(List<CurrencyOffer> list,
                                      boolean filterStockOffers,
                                      boolean filterValidStockOffers,
                                      boolean filterExcessiveOffers) {
        CurrencyOffer bestOffer = null;
        if (list == null) {
            return null;
        }
        for (CurrencyOffer offer : list) {
            // Return most top offer that meets filter requirements

            if (filterStockOffers && offer.getStock() < 0) {
                continue;
            }

            if (filterValidStockOffers && (offer.getStock() >= 0 && offer.getStock() < offer.getSellAmount())) {
                continue;
            }

            if (filterExcessiveOffers) {
                float buyValue = poeNinjaParser.getCurrentCValueFor(offer.getBuyID()) * offer.getBuyAmount();
                float sellValue = poeNinjaParser.getCurrentCValueFor(offer.getSellID()) * offer.getSellAmount();
                float higherValue = (buyValue > sellValue ? buyValue : sellValue);

                // If sell and buy value differ too much from each other, filter
                if (Math.abs(buyValue - sellValue) > 0.75 * higherValue) {
                    LogManager.getInstance().log(getClass(), "Filtered excessive offer for " + offer.getBuyID() + " - " + offer.getSellID() + ": " + buyValue + " - " + sellValue);
                    continue;
                }
            }

            bestOffer = offer;
            break;
        }
        return bestOffer;
    }

    public ObservableList<CurrencyDeal> getCurrentDeals() {
        return currentDeals;
    }

    public ObservableList<CurrencyDeal> getPlayerDeals() {
        return playerDeals;
    }

    @Override
    public void onParsingStarted() {
        if (listener != null) {
            listener.onParsingStarted();
        }
    }

    @Override
    public void onParsingFinished() {
        parseDeals(true);
        if (listener != null) {
            listener.onParsingFinished();
        }
    }

    public boolean isUpdating() {
        return webParser.isUpdating();
    }

    public void registerListener(ParseListener listener, PoeNinjaParser.PoeNinjaListener ninjaListener) {
        this.listener = listener;
        poeNinjaParser.registerListener(ninjaListener);
    }

    public void cancelUpdate() {
        webParser.cancel();
    }

    public ObservableList<String> getLeagueList() {
        return poeApiParser.getCurrentLeagues();
    }

    public void updateCurrencyValues() {
        poeNinjaParser.fetchRates(PropertyManager.getInstance().getCurrentLeague(), true);
    }

    public Float getCurrencyValue(CurrencyID what, CurrencyID inWhat) {
        return poeNinjaParser.getCurrentValue(what, inWhat);
    }

    public ObservableList<Map.Entry<CurrencyID, Float>> getCurrencyValues() {
        return FXCollections.observableArrayList(poeNinjaParser.getCurrentRates().entrySet());
    }

    public ObservableList<CurrencyOffer> getBuyOffers(CurrencyID secondary) {
        return webParser.getOffersFor(secondary, false);
//        return (ObservableList<CurrencyOffer>) webParser.getCurrentOffers().get(new Pair<>(secondary, PropertyManager.getInstance().getPrimaryCurrency()));
    }

    public ObservableList<CurrencyOffer> getSellOffers(CurrencyID secondary) {
        return webParser.getOffersFor(secondary, true);
//        return (ObservableList<CurrencyOffer>) webParser.getCurrentOffers().get(new Pair<>(PropertyManager.getInstance().getPrimaryCurrency(), secondary));
    }
}
