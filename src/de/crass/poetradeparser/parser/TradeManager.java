package de.crass.poetradeparser.parser;

import de.crass.poetradeparser.LogManager;
import de.crass.poetradeparser.PropertyManager;
import de.crass.poetradeparser.model.CurrencyDeal;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.model.CurrencyOffer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.*;

/**
 * Created by mcrass on 19.07.2018.
 */
public class TradeManager implements ParseListener {
    private PoeTradeWebParser webParser;
    private ObservableList<CurrencyDeal> currentDeals;
    private ObservableList<CurrencyDeal> playerDeals;
    private PoeNinjaParser poeNinjaParser;
    private Comparator<? super CurrencyDeal> diffValueSorter = new Comparator<CurrencyDeal>() {
        @Override
        public int compare(CurrencyDeal o1, CurrencyDeal o2) {
            float o1Value = o1.getDiffValue();
            float o2Value = o2.getDiffValue();

            if(o1Value == o2Value){
                return 0;
            }
            return o1Value > o2Value ? -1 : 1;
        }
    };
    private ParseListener listener;

    public TradeManager() {
        webParser = new PoeTradeWebParser();
        webParser.setParseListener(this);
        poeNinjaParser = new PoeNinjaParser();

        currentDeals = FXCollections.observableArrayList();
        playerDeals = FXCollections.observableArrayList();
    }

    public void updateOffers() {
        webParser.update();
    }

    public void parseDeals() {
        CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
        currentDeals.clear();
        playerDeals.clear();
        HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> marketOffers = webParser.getCurrentOffers();

        if (marketOffers == null || marketOffers.isEmpty()) {
            LogManager.getInstance().log(getClass(), "No offers to parse.");
            return;
        }
        LogManager.getInstance().log(getClass(), "Parsing offers...");

        boolean filterStockOffers = PropertyManager.filterStockOffers;
        boolean filterValidStockOffers = PropertyManager.filterValidStockOffers;

        HashSet<Pair<CurrencyID, CurrencyID>> processedKeys = new HashSet<>();
        for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : marketOffers.entrySet()) {
            Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
            if (key.getKey() == primaryCurrency) {
                continue;
            }
            String currency = String.valueOf(key.getKey());

            CurrencyOffer bestOffer = webParser.getBestOfferForKey(offerMap.getValue(), filterStockOffers, filterValidStockOffers);

            float buy = 0;
            if (bestOffer == null) {
                LogManager.getInstance().log(getClass(), "All buy offers filtered for " + currency);

            } else {
                buy = bestOffer.getBuyValue();
                if (bestOffer.getSellValue() != 1) {
                    LogManager.getInstance().log(getClass(), "Currency rate for " + currency + " was not normalized!");
                    buy /= bestOffer.getSellValue();
                }
            }

            Float cValue = poeNinjaParser.getCurrentRates().get(key.getKey());
            if (cValue == null) {
                LogManager.getInstance().log(getClass(), "Could not get rate for " + currency);
                cValue = 0f;
            }

            Pair invertedKey = new Pair<>(key.getValue(), key.getKey());
            List<CurrencyOffer> invertedOfferMap = marketOffers.get(invertedKey);
            CurrencyOffer bestInvertedOffer = null;
            if(invertedOfferMap != null) {
                for (CurrencyOffer offer : invertedOfferMap) {
                    if (!filterStockOffers || (!filterValidStockOffers && offer.getStock() >= 0) ||
                            filterValidStockOffers && offer.getStock() > offer.getSellValue()) {
                        bestInvertedOffer = offer;
                        break;
                    }
                }
            }

            float sell = 0;
            if (bestInvertedOffer == null) {
                LogManager.getInstance().log(getClass(), "All sell offers filtered for " + currency);
            } else {
                sell = bestInvertedOffer.getSellValue();
                if (bestInvertedOffer.getBuyValue() != 1) {
                    LogManager.getInstance().log(getClass(), "Currency rate for " + currency + " was not normalized!");
                    sell /= bestInvertedOffer.getBuyValue();
                }
            }


            int totalOffers = offerMap.getValue().size() + (invertedOfferMap == null ? 0 : invertedOfferMap.size());

            currentDeals.add(new CurrencyDeal(primaryCurrency, key.getKey(), cValue, totalOffers, buy, sell));
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
                //Buy offer
                key = invertedKey;
                invertedKey = offerMap.getKey();
            }

            CurrencyID secondaryCurrency = key.getValue();

            if(playerOffers.get(key) != null) {
                playerSellOffer = playerOffers.get(key).get(0);
            }

            List<CurrencyOffer> playerBuyOfferList = playerOffers.get(invertedKey);
            if (playerBuyOfferList != null) {
                playerBuyOffer = playerBuyOfferList.get(0);

            }

            bestMarketSellOffer = webParser.getBestOfferForKey(marketOffers.get(key),
                    filterStockOffers,
                    filterValidStockOffers);


            bestMarketBuyOffer = webParser.getBestOfferForKey(marketOffers.get(invertedKey),
                    filterStockOffers,
                    filterValidStockOffers);

            float playerSellPrice = 0;
            float playerBuyPrice = 0;
            int playerSellStock = 0;
            int playerBuyStock = 0;
            float marketSellPrice = 0;
            float marketBuyPrice = 0;

            if (bestMarketSellOffer != null) {
                marketSellPrice = bestMarketSellOffer.getSellValue();
            }

            if (bestMarketBuyOffer != null) {
                marketBuyPrice = bestMarketBuyOffer.getBuyValue();
            }

            if (playerSellOffer != null) {
                playerSellPrice = playerSellOffer.getSellValue();
                playerSellPrice /= playerSellOffer.getBuyValue();

                playerSellStock = playerSellOffer.getStock();
            }

            if (playerBuyOffer != null) {
                playerBuyPrice = playerBuyOffer.getBuyValue();
                playerBuyPrice /= playerBuyOffer.getSellValue();

                playerBuyStock = playerBuyOffer.getStock();
            }

            float cValue = poeNinjaParser.getCurrentRates().get(key.getValue());
            int totalOffers = -1;

            if (playerBuyPrice > 0 || playerSellPrice > 0) {
                playerDeals.add(new CurrencyDeal(primaryCurrency, secondaryCurrency, cValue, totalOffers, marketBuyPrice,
                        marketSellPrice, playerBuyPrice, playerSellPrice, playerBuyStock, playerSellStock));
            } else {
                LogManager.getInstance().log(getClass(), "Player offer didnt contain shit!");
            }
        }
        playerDeals.sort(diffValueSorter);
        LogManager.getInstance().log(getClass(), "Finished");
    }

    public ObservableList<CurrencyDeal> getCurrentDeals() {
        return currentDeals;
    }

    public ObservableList<CurrencyDeal> getPlayerDeals() {
        return playerDeals;
    }

    @Override
    public void onParsingFinished() {
        parseDeals();
        if(listener != null){
            listener.onParsingFinished();
        }
    }

    public boolean isUpdating() {
        return webParser.isUpdating();
    }

    public void registerListener(ParseListener listener) {
        this.listener = listener;
    }

    public void cancelUpdate(){
        webParser.cancel();
    }
}
