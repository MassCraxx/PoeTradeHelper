package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.core.type.TypeReference;
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
//    private final File file = new File("poeninja.dat");

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
                TypeReference<HashMap<CurrencyID, Float>> typeRef = new TypeReference<HashMap<CurrencyID, Float>>() {};
                try {
                    LogManager.getInstance().log(getClass(), "Loading poe.ninja currency for league "+league+" values from cache.");
                    currentRates = objectMapper.readValue(cacheFile, typeRef);
                    if(listener != null){
                        listener.onRatesFetched();
                    }
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

                HashMap<String, Integer> ninjaToTradeIdMap = new HashMap<>();
                JSONArray idArray = json.getJSONArray("currencyDetails");
                for (Object currencyDetailsObject : idArray) {
                    if (currencyDetailsObject instanceof JSONObject) {
                        JSONObject currencyDetails = (JSONObject) currencyDetailsObject;
                        ninjaToTradeIdMap.put(currencyDetails.getString("name"), currencyDetails.getInt("poeTradeId"));
                    }
                }

                JSONArray array = json.getJSONArray("lines");
                for (Object currencyObject : array) {
                    if (currencyObject instanceof JSONObject) {
                        JSONObject currency = (JSONObject) currencyObject;
                        String currencyName = currency.getString("currencyTypeName");
                        CurrencyID id = CurrencyID.get(ninjaToTradeIdMap.get(currencyName));
                        if (id != null) {
                            float chaosValue = currency.getFloat("chaosEquivalent");
                            currentRates.put(id, chaosValue);
                        }
                    }
                }

                if (!currentRates.isEmpty()) {
                    try {
                        objectMapper.writeValue(cacheFile, currentRates);
                    } catch (Exception e) {
                        LogManager.getInstance().log(getClass(), "Writing ninja cache failed!\n" + e);
                    }
                }

                if(listener != null){
                    listener.onRatesFetched();
                }
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Fetching rates from PoeNinja failed. No internet connection?");
                e.printStackTrace();
            } catch (JSONException j){
                LogManager.getInstance().log(getClass(), currencyURL + " returned no valid JSON!\n");
                noRatesAvailable = true;
            }
        }
    }

    HashMap<CurrencyID, Float> getCurrentRates() {
        if(currentRates == null || currentRates.isEmpty()){
            fetchRates(PropertyManager.getInstance().getCurrentLeague(), false);
        }
        return currentRates;
    }

    Float getCurrentCValueFor(CurrencyID id) {
        if (id == CurrencyID.CHAOS) {
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
