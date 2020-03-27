package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.web.HttpManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PoeNinjaParser {
    private static final String currencyURL = "https://poe.ninja/api/data/currencyoverview?type=Currency";
    private final ObjectMapper objectMapper;
    private String cacheFileName = "_rates.dat";

    private boolean useOfflineCache = true;
    private long updateDelay = 6 * 60 * 60 * 1000; // 6 hours

    private PoeNinjaListener listener;

    // Currency - (CurrencyID<>Chaos Value)
    private HashMap<CurrencyID, Float> currentRates = new HashMap<>();
    private boolean noRatesAvailable = false;

    PoeNinjaParser() {
        objectMapper = new ObjectMapper();
    }

    public PoeNinjaParser(PoeNinjaListener listener) {
        this();
        this.listener = listener;
    }

    void fetchRates(String league, boolean forceUpdate) {
        // If there is a file  and it is recent
        File cacheFile = new File(league + cacheFileName);
        if (!forceUpdate && (useOfflineCache && cacheFile.exists() &&
                cacheFile.lastModified() + updateDelay > System.currentTimeMillis())) {

            // If no current rates have been loaded before, load from cache
            if (currentRates == null || currentRates.isEmpty()) {
                //TypeReference<HashMap<CurrencyID, Float>> typeRef = new TypeReference<HashMap<CurrencyID, Float>>() {};
                try {
                    LogManager.getInstance().log(getClass(), "Loading poe.ninja currency for league "+league+" values from cache.");
                    parseJson(objectMapper.readValue(cacheFile, JSONObject.class));
                } catch (Exception e) {
                    LogManager.getInstance().log(getClass(), "Error loading poe.ninja values from cache.");
                    fetchRates(league, true);
                }
            }
        } else {
            if(noRatesAvailable){
                return;
            }
            // Fetch online
            LogManager.getInstance().log(getClass(), "Fetching new currency values for league "+league+" from poe.ninja.");
            JSONObject json;
            try {
                json = HttpManager.getInstance().getJson(currencyURL, "&league=" + league);

                if (json == null || json.length() == 0) {
                    LogManager.getInstance().log(getClass(), "Invalid response from PoeNinja! API has been changed.");
                    noRatesAvailable = true;
                    return;
                }

                try {
                    objectMapper.writeValue(cacheFile, json.toString());
                } catch (Exception e) {
                    LogManager.getInstance().log(getClass(), "Writing ninja cache failed!\n" + e);
                }

                parseJson(json);
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Fetching rates from PoeNinja failed. No internet connection?");
                e.printStackTrace();
            } catch (JSONException j){
                LogManager.getInstance().log(getClass(), currencyURL + "&league=" + league + " returned no valid JSON!\n");
                j.printStackTrace();
                noRatesAvailable = true;
            }
        }
    }

    private void parseJson(JSONObject json) {
        JSONArray idArray = json.getJSONArray("currencyDetails");
        for (Object currencyDetailsObject : idArray) {
            if (currencyDetailsObject instanceof JSONObject) {
                JSONObject currencyDetails = (JSONObject) currencyDetailsObject;
                if(currencyDetails.get("tradeId") != null && currencyDetails.getInt("poeTradeId") > 0 && !currencyDetails.get("tradeId").equals(JSONObject.NULL)) {
                    new CurrencyID(currencyDetails).store();
                }
            }
        }

        JSONArray array = json.getJSONArray("lines");
        for (Object currencyObject : array) {
            if (currencyObject instanceof JSONObject) {
                JSONObject currency = (JSONObject) currencyObject;
                String currencyName = currency.getString("currencyTypeName");
                CurrencyID id = CurrencyID.getByDisplayName(currencyName);
                if (id != null) {
                    float chaosValue = currency.getFloat("chaosEquivalent");
                    currentRates.put(id, chaosValue);
                }
            }
        }

        if(listener != null){
            listener.onRatesFetched();
        }
    }

    HashMap<CurrencyID, Float> getCurrentRates() {
        if(currentRates == null || currentRates.isEmpty()){
            fetchRates(PropertyManager.getInstance().getCurrentLeague(), false);
        }
        return currentRates;
    }

    Float getCurrentCValueFor(CurrencyID id) {
        if (id.getTradeID().equals("chaos")) {
            return 1f;
        }
        Float value = getCurrentRates().get(id);

        return value == null ? 0 : value;
    }

    Float getCurrentValue(CurrencyID what, CurrencyID inWhat) {
        Float whatValue = getCurrentCValueFor(what);
        Float inWhatValue = getCurrentCValueFor(inWhat);

        if (inWhatValue == null || inWhatValue == 0) {
            return 0f;
        }

        return whatValue / inWhatValue;
    }

    void registerListener(PoeNinjaListener ninjaListener) {
        listener = ninjaListener;
    }

    void reset() {
        currentRates.clear();
    }

    public interface PoeNinjaListener{
        void onRatesFetched();
    }
}
