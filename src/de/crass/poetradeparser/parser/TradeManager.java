package de.crass.poetradeparser.parser;

import de.crass.poetradeparser.Main;
import de.crass.poetradeparser.PropertyManager;
import de.crass.poetradeparser.model.CurrencyDeal;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.model.CurrencyOffer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mcrass on 19.07.2018.
 */
public class TradeManager implements ParseListener{
    private PoeTradeWebParser webParser;
    private ObservableList<CurrencyDeal> currentDeals;
    private boolean filterStockOffers = true;
    private boolean filterValidStockOffers = true;
    private PoeNinjaParser poeNinjaParser;

    public TradeManager(){
        webParser = new PoeTradeWebParser();
        webParser.setParseListener(this);
        poeNinjaParser = new PoeNinjaParser();

        currentDeals = FXCollections.observableArrayList();
    }

    public void updateOffers(){
        webParser.update();
    }

    public void parseDeals() {
        CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
        currentDeals.clear();
        HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offers = webParser.getCurrentOffers();
//        HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers = webParser.getPlayerOffers();

        if(offers == null || offers.isEmpty()){
            Main.log(getClass(), "No offers to parse.");
            return;
        }
        Main.log(getClass(), "Parsing offers...");

        for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : offers.entrySet()) {
            Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
            if (key.getKey() == primaryCurrency) {
                continue;
            }
                CurrencyOffer bestOffer = null;
                for (CurrencyOffer offer : offerMap.getValue()) {
                    if (!filterStockOffers || (!filterValidStockOffers && offer.getStock() >= 0) ||
                            filterValidStockOffers && offer.getStock() > offer.getSellValue()) {
                        bestOffer = offer;
                        break;
                    }
                }

                String currency = String.valueOf(key.getKey());

                float buy = 0;
                if (bestOffer == null) {
                    Main.log(getClass(), "All buy offers filtered for " + currency);

                } else {
                    buy = bestOffer.getBuyValue();
                    if (bestOffer.getSellValue() != 1) {
                        Main.log(getClass(), "Currency rate for " + currency + " was not normalized!");
                        buy /= bestOffer.getSellValue();
                    }
                }

                Float cValue = poeNinjaParser.getCurrentRates().get(key.getKey());
                if (cValue == null) {
                    Main.log(getClass(), "Could not get rate for " + currency);
                    cValue = 0f;
                }

                Pair invertedKey = new Pair<>(key.getValue(), key.getKey());
                List<CurrencyOffer> invertedOfferMap = offers.get(invertedKey);
                CurrencyOffer bestInvertedOffer = null;
                for (CurrencyOffer offer : invertedOfferMap) {
                    if (!filterStockOffers || (!filterValidStockOffers && offer.getStock() >= 0) ||
                            filterValidStockOffers && offer.getStock() > offer.getSellValue()) {
                        bestInvertedOffer = offer;
                        break;
                    }
                }

                float sell = 0;
                if (bestInvertedOffer == null) {
                    Main.log(getClass(), "All sell offers filtered for " + currency);
                } else {

                    sell = bestInvertedOffer.getSellValue();
                    if (bestInvertedOffer.getBuyValue() != 1) {
                        Main.log(getClass(), "Currency rate for " + currency + " was not normalized!");
                        sell /= bestInvertedOffer.getBuyValue();
                    }
                }

//                float diff = buy - sell;
//                if (buy == 0 || sell == 0) {
//                    diff = 0;
//                }
//                float diffCValue = cValue * diff;
//
//                float playerBuyPrice = 0;
//                float playerSellPrice = 0;
//
//                if (playerOffers != null && !playerOffers.isEmpty()) {
//                    if (playerOffers.get(invertedKey) != null) {
//                        CurrencyOffer playerSellOffer = playerOffers.get(invertedKey).get(0);
//                        if (playerSellOffer != null) {
//                            playerSellPrice = playerSellOffer.getSellValue();
//                        }
//                    }
//
//                    if (playerOffers.get(key) != null) {
//                        CurrencyOffer playerBuyOffer = playerOffers.get(key).get(0);
//                        if (playerBuyOffer != null) {
//                            playerBuyPrice = playerBuyOffer.getBuyValue();
//                        }
//                    }
//
//                }

//                buildRow(currency, cValue, buy, sell, diff, diffCValue, playerBuyPrice, playerSellPrice));

            int totalOffers = offerMap.getValue().size() + invertedOfferMap.size();

            currentDeals.add(new CurrencyDeal(primaryCurrency, key.getKey(), cValue, totalOffers, buy, sell));
        }
    }

    public ObservableList<CurrencyDeal> getCurrentDeals() {
        return currentDeals;
    }

    @Override
    public void onParsingFinished() {
        parseDeals();
    }
}
