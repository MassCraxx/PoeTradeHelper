package de.crass.poetradehelper.model;

import de.crass.poetradehelper.PropertyManager;

/**
 * Created by mcrass on 19.07.2018.
 */
public class CurrencyDeal {


    private final CurrencyID primaryCurrencyID;
    private final CurrencyID secondaryCurrencyID;
    private final float cValue;
    private final int offers;
    private final float buyAmount;
    private final float sellAmount;

    private final float playerBuyAmount;
    private final float playerSellAmount;
    private int playerBuyStock;
    private int playerSellStock;
    private String league;

    private String buyQueryID;
    private String sellQueryID;

    private long timestamp;

    public CurrencyDeal(CurrencyID primaryCurrencyID,
                        CurrencyID secondaryCurrencyID,
                        float cValue,
                        int offers,
                        float buyAmount,
                        float sellAmount, long timestamp) {
        this(primaryCurrencyID, secondaryCurrencyID, cValue, offers, buyAmount, sellAmount, 0, 0, 0, 0, timestamp);
    }

    public CurrencyDeal(CurrencyID primaryCurrencyID,
                        CurrencyID secondaryCurrencyID,
                        float cValue,
                        int offers,
                        float buyAmount,
                        float sellAmount,
                        float playerBuyAmount,
                        float playerSellAmount,
                        int playerBuyStock,
                        int playerSellStock, long timestamp) {

        this.primaryCurrencyID = primaryCurrencyID;
        this.secondaryCurrencyID = secondaryCurrencyID;
        this.cValue = cValue;
        this.offers = offers;
        this.buyAmount = buyAmount;
        this.sellAmount = sellAmount;
        this.playerBuyAmount = playerBuyAmount;
        this.playerSellAmount = playerSellAmount;
        this.playerBuyStock = playerBuyStock;
        this.playerSellStock = playerSellStock;
        this.timestamp = timestamp;

        this.league = PropertyManager.getInstance().getCurrentLeague();
    }

    public CurrencyID getPrimaryCurrencyID() {
        return primaryCurrencyID;
    }

    public CurrencyID getSecondaryCurrencyID() {
        return secondaryCurrencyID;
    }

    public float getcValue() {
        return cValue;
    }

    public int getOffers() {
        return offers;
    }

    public float getBuyAmount() {
        return buyAmount;
    }

    public float getSellAmount() {
        return sellAmount;
    }

    public float getPlayerBuyAmount() {
        return playerBuyAmount;
    }

    public float getPlayerSellAmount() {
        return playerSellAmount;
    }

    public float getDiff() {
        if (buyAmount != 0 && sellAmount != 0) {
            return buyAmount - sellAmount;
        }
        return 0;
    }

    public float getPlayerDiff() {
        if (playerBuyAmount != 0 && playerSellAmount != 0) {
            return playerBuyAmount - playerSellAmount;
        }
        return 0;
    }

    public float getDiffValue() {
        float diff = getDiff();
        return diff * getcValue();
    }

    public float getPlayerDiffValue() {
        float diff = getPlayerDiff();
        if (diff > 0) {
            return diff * getcValue();
        }
        return 0;
    }

    public int getPlayerBuyStock() {
        return playerBuyStock;
    }

    public int getPlayerSellStock() {
        return playerSellStock;
    }

    public String getLeague() {
        return league;
    }

    public String getBuyQueryID() {
        return buyQueryID;
    }

    public void setBuyQueryID(String buyQueryID) {
        this.buyQueryID = buyQueryID;
    }

    public String getSellQueryID() {
        return sellQueryID;
    }

    public void setSellQueryID(String sellQueryID) {
        this.sellQueryID = sellQueryID;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
