package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.LogManager;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.model.CurrencyOffer;
import de.crass.poetradeparser.parser.PoeNinjaParser;
import javafx.util.Pair;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AWT Swing UI
@Deprecated
public class CurrencyTableModel extends DefaultTableModel {

    private final PoeNinjaParser poeNinjaParser;
    private boolean filterStockOffers = true;
    private boolean filterValidStockOffers = true;

    public CurrencyTableModel() {
        addColumn("Currency");
        addColumn("C/unit");
        addColumn("Buy");
        addColumn("Sell");
        addColumn("Diff");
        addColumn("C/Trade");
        addColumn("P Buy");
        addColumn("P Sell");

        poeNinjaParser = new PoeNinjaParser();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                // Currency
                return String.class;
            case 1:
                // CValue
                return Float.class;
            case 2:
                // Buy
                return Float.class;
            case 3:
                // Sell
                return Float.class;
            case 4:
                // Diff
                return Float.class;
            case 5:
                // DiffCValue
                return Float.class;
            case 6:
                // P Buy
                return Float.class;
            case 7:
                // P Sell
                return Float.class;
            default:
                return String.class;
        }
    }

    public void update(CurrencyID primaryCurrency,
                HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offers,
                HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers) {
        setRowCount(0);
        for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : offers.entrySet()) {
            Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
            if (key.getValue() == primaryCurrency) {
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

                Pair invertedKey = new Pair<>(primaryCurrency, key.getKey());
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
                    LogManager.getInstance().log(getClass(), "All sell offers filtered for " + currency);
                } else {

                    sell = bestInvertedOffer.getSellValue();
                    if (bestInvertedOffer.getBuyValue() != 1) {
                        LogManager.getInstance().log(getClass(), "Currency rate for " + currency + " was not normalized!");
                        sell /= bestInvertedOffer.getBuyValue();
                    }
                }

                float diff = buy - sell;
                if (buy == 0 || sell == 0) {
                    diff = 0;
                }
                float diffCValue = cValue * diff;

                float playerBuyPrice = 0;
                float playerSellPrice = 0;

                if (playerOffers != null && !playerOffers.isEmpty()) {
                    if (playerOffers.get(invertedKey) != null) {
                        CurrencyOffer playerSellOffer = playerOffers.get(invertedKey).get(0);
                        if (playerSellOffer != null) {
                            playerSellPrice = playerSellOffer.getSellValue();
                        }
                    }

                    if (playerOffers.get(key) != null) {
                        CurrencyOffer playerBuyOffer = playerOffers.get(key).get(0);
                        if (playerBuyOffer != null) {
                            playerBuyPrice = playerBuyOffer.getBuyValue();
                        }
                    }

                }

                addRow(buildRow(currency, cValue, buy, sell, diff, diffCValue, playerBuyPrice, playerSellPrice));
            }
        }
    }

    Object[] buildRow(String currency, float cValue, float buy, float sell, float diff, float diffCValue, float playerBuyPrice, float playerSellPrice) {
        return new Object[]{currency, cValue, buy, sell, diff, diffCValue, playerBuyPrice, playerSellPrice};
    }
}
