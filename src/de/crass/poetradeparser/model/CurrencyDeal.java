package de.crass.poetradeparser.model;

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

    public CurrencyDeal(CurrencyID primaryCurrencyID,
                 CurrencyID secondaryCurrencyID,
                 float cValue,
                 int offers,
                 float buyAmount,
                 float sellAmount){

        this.primaryCurrencyID = primaryCurrencyID;
        this.secondaryCurrencyID = secondaryCurrencyID;
        this.cValue = cValue;
        this.offers = offers;
        this.buyAmount = buyAmount;
        this.sellAmount = sellAmount;
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
}
