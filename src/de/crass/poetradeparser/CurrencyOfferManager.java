package de.crass.poetradeparser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Versuch currency rates aus item suche zu generieren... keine gute idee
@Deprecated
public class CurrencyOfferManager {

    private final boolean alwaysUpdate = false;
    private HashMap<String,List<String>> currencyOffers;

    private final String[] currency = {"Exalted Orb", "Orb of Alteration"};
    ObjectMapper objectMapper = new ObjectMapper();
    File saveFile = new File("data.dat");

    CurrencyOfferManager() {

        if (!alwaysUpdate && saveFile.exists()) {
            Main.log(getClass(), "Loading database from file...");
            try {
                currencyOffers = objectMapper.readValue(saveFile, new TypeReference<HashMap<String,List<String>>>() {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            updateCurrencyOffers();
        }
    }

    void updateCurrencyOffers() {
        currencyOffers = new HashMap<>();
        PoeTradeSearcher poeTradeSearcher = new PoeTradeSearcher();
        for (String type : currency) {
            List<String> result = poeTradeSearcher.searchForItemType(Main.currentLeague, type);
            if(result != null) {
                currencyOffers.put(type, result);
            }
        }

        if(currencyOffers.size() > 0) {
            try {
                objectMapper.writeValue(saveFile, currencyOffers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public HashMap<String,Integer> getRates(String currency){
        HashMap<String,Integer> rates = new HashMap<>();
        List<String> offers = currencyOffers.get(currency);
        for(String object : offers){
            ItemOffer offer = new ItemOffer(object);
            if(offer != null) {
                int minPrice = rates.getOrDefault(offer.getCurrency(),-1);
                if (minPrice == -1 || minPrice > offer.getPrice()) {
                    rates.put(offer.getCurrency(), offer.getPrice());
                }
            }
        }
        return rates;
    }

    public void printRates(String currency) {
        for(Map.Entry<String,Integer> entry : getRates(currency).entrySet()){
            Main.log(getClass(), entry.getKey() + ": " + entry.getValue());
        }
    }

    public void printAllRates() {
        for(String currency : currency){
            Main.log(getClass(), currency + " rates:");
            printRates(currency);
        }
    }
}
