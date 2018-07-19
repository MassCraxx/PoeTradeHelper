package de.crass.poetradeparser;

public class CurrencyOffer {
    private String playerName;
    private String accountName;
    private PoeTradeWebParser.CurrencyID sellID;
    private float sellValue;
    private PoeTradeWebParser.CurrencyID buyID;
    private float buyValue;
    private int stock;

    public CurrencyOffer(String username, String accountName, PoeTradeWebParser.CurrencyID sellID, float sellValue, PoeTradeWebParser.CurrencyID buyID, float buyValue, int stock) {
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

    public PoeTradeWebParser.CurrencyID getSellID() {
        return sellID;
    }

    public float getSellValue() {
        return sellValue;
    }

    public PoeTradeWebParser.CurrencyID getBuyID() {
        return buyID;
    }

    public float getBuyValue() {
        return buyValue;
    }

    public int getStock() {
        return stock;
    }
}
