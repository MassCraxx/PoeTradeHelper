package de.crass.poetradeparser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

public class PoeNinjaParser {
    public static final String currencyURL = "http://poe.ninja/api/Data/GetCurrencyOverview";

    // Currency - (Pay<>Sell)
    private HashMap<PoeTradeWebParser.CurrencyID, Float> currentRates = new HashMap<>();

    PoeNinjaParser() {
    }

    private void parseCurrency(String league) {
        Main.log(getClass(), "Fetching current currency values from poe.ninja.");
        JSONObject json = null;
        try {
            json = HttpManager.getInstance().getJson(currencyURL, "?league=" + league);
        } catch (IOException e) {
            Main.log(getClass(), "IOException!\n" + e);
        }

        if(json == null || json.length() == 0){
            Main.log(getClass(), "Invalid response");
            return;
        }

        HashMap<String,Integer> ninjaToTradeIdMap = new HashMap<>();
        JSONArray idArray = json.getJSONArray("currencyDetails");
        for(Object currencyDetailsObject : idArray){
            if(currencyDetailsObject instanceof JSONObject) {
                JSONObject currencyDetails = (JSONObject) currencyDetailsObject;
                ninjaToTradeIdMap.put(currencyDetails.getString("name"), currencyDetails.getInt("poeTradeId"));
            }
        }

        JSONArray array = json.getJSONArray("lines");
        for(Object currencyObject : array){
            if(currencyObject instanceof JSONObject){
                JSONObject currency = (JSONObject) currencyObject;
                String currencyName = currency.getString("currencyTypeName");
                PoeTradeWebParser.CurrencyID id = PoeTradeWebParser.CurrencyID.get(ninjaToTradeIdMap.get(currencyName));
                float chaosValue = currency.getFloat("chaosEquivalent");
                currentRates.put(id, chaosValue);
            }
        }
    }

    public HashMap<PoeTradeWebParser.CurrencyID, Float> getCurrentRates() {
        if(currentRates == null || currentRates.isEmpty()){
            parseCurrency(Main.currentLeague);
            if(currentRates == null){
                currentRates = new HashMap<>();
            }
        }
        return currentRates;
    }
}
