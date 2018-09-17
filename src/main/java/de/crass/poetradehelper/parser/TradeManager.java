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
    private static TradeManager instance
            ;
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

            if(o1Value == o2Value){
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
        poeApiParser = new PoeApiParser();

        currentDeals = FXCollections.observableArrayList();
        playerDeals = FXCollections.observableArrayList();
    }

    public static TradeManager getInstance() {
        if (instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }

    public void updateOffers() {
        webParser.updateCurrencies(PropertyManager.getInstance().getFilterList(), true);
    }

    public void updatePlayerOffers(){
        List<CurrencyID> list = new LinkedList<>();
        for(CurrencyDeal deal : playerDeals){
            list.add(deal.getSecondaryCurrencyID());
        }
        webParser.updateCurrencies(list, false);
    }

    public void updateOffersForCurrency(CurrencyID secondaryCurrencyID) {
        webParser.updateCurrency(secondaryCurrencyID);
    }

    public void parseDeals(boolean async){
        if(async){
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    parseDeals();
                }
            });
        } else{
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
                //Buy offer -> Sell offer
                key = invertedKey;
                invertedKey = offerMap.getKey();
            }

            CurrencyID secondaryCurrency = key.getValue();
            float cValue = poeNinjaParser.getCurrentValueFor(secondaryCurrency);

            bestMarketSellOffer = webParser.getBestOffer(marketOffers.get(key));

            bestMarketBuyOffer = webParser.getBestOffer(marketOffers.get(invertedKey));

            int totalOffers = (marketOffers.get(key) == null ? 0 : marketOffers.get(key).size())
                    + (marketOffers.get(invertedKey) == null ? 0 : marketOffers.get(invertedKey).size());

            float marketSellPrice = 0;
            if (bestMarketSellOffer != null) {
                marketSellPrice = bestMarketSellOffer.getSellValue();
                marketSellPrice /= bestMarketSellOffer.getBuyValue();
            }

            float marketBuyPrice = 0;
            if (bestMarketBuyOffer != null) {
                marketBuyPrice = bestMarketBuyOffer.getBuyValue();
                marketBuyPrice /= bestMarketBuyOffer.getSellValue();
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
                //Buy offer -> Sell offer
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

            bestMarketSellOffer = webParser.getBestOffer(marketOffers.get(key));

            bestMarketBuyOffer = webParser.getBestOffer(marketOffers.get(invertedKey));

            float playerSellPrice = 0;
            float playerBuyPrice = 0;
            int playerSellStock = 0;
            int playerBuyStock = 0;
            float marketSellPrice = 0;
            float marketBuyPrice = 0;

            if (bestMarketSellOffer != null) {
                marketSellPrice = bestMarketSellOffer.getSellValue();
                marketSellPrice /= bestMarketSellOffer.getBuyValue();
            }

            if (bestMarketBuyOffer != null) {
                marketBuyPrice = bestMarketBuyOffer.getBuyValue();
                marketBuyPrice /= bestMarketBuyOffer.getSellValue();
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

            float cValue = poeNinjaParser.getCurrentValueFor(secondaryCurrency);
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

    public ObservableList<CurrencyDeal> getCurrentDeals() {
        return currentDeals;
    }

    public ObservableList<CurrencyDeal> getPlayerDeals() {
        return playerDeals;
    }

    @Override
    public void onParsingFinished() {
        parseDeals(true);
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

    public ObservableList<String> getLeagueList() {
        return poeApiParser.getCurrentLeagues();
    }
    
    public void updateCurrencyValues(){
        poeNinjaParser.parseCurrency(PropertyManager.getInstance().getCurrentLeague(), true);
    }
}
