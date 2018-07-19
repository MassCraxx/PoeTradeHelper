package de.crass.poetradeparser.model;

public class CurrencyOffer {
    private String playerName;
    private String accountName;
    private CurrencyID sellID;
    private float sellValue;
    private CurrencyID buyID;
    private float buyValue;
    private int stock;

    public CurrencyOffer(String username, String accountName, CurrencyID sellID, float sellValue, CurrencyID buyID, float buyValue, int stock) {
        this.playerName = username;
        this.accountName = accountName;
        this.sellID = sellID;
        this.sellValue = sellValue;
        this.buyID = buyID;
        this.buyValue = buyValue;
        this.stock = stock;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAccountName() {
        return accountName;
    }

    public CurrencyID getSellID() {
        return sellID;
    }

    public float getSellValue() {
        return sellValue;
    }

    public CurrencyID getBuyID() {
        return buyID;
    }

    public float getBuyValue() {
        return buyValue;
    }

    public int getStock() {
        return stock;
    }
}
