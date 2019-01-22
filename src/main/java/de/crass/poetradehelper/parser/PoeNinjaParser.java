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
    private static final String currencyURL = "http://poe.ninja/api/Data/GetCurrencyOverview";
    private final ObjectMapper objectMapper;
    private final File file = new File("poeninja.dat");
    private boolean useOfflineCache = true;
    private long updateDelay = 6 * 60 * 60 * 1000; // 6 hours

    private PoeNinjaListener listener;

    // Currency - (CurrencyID<>Chaos Value)
    private HashMap<CurrencyID, Float> currentRates;

    PoeNinjaParser() {
        objectMapper = new ObjectMapper();
    }

    public PoeNinjaParser(PoeNinjaListener listener) {
        this();
        this.listener = listener;
    }

    void fetchRates(String league, boolean forceUpdate) {
        // If there is a file  and it is recent
        if (!forceUpdate && (useOfflineCache && file.exists() &&
                file.lastModified() + updateDelay > System.currentTimeMillis())) {

            // If no current rates have been loaded before, load from cache
            if (currentRates == null || currentRates.isEmpty()) {
                TypeReference<HashMap<CurrencyID, Float>> typeRef = new TypeReference<HashMap<CurrencyID, Float>>() {};
                try {
                    LogManager.getInstance().log(getClass(), "Loading poe.ninja currency values from cache.");
                    currentRates = objectMapper.readValue(file, typeRef);
                    if(listener != null){
                        listener.onRatesFetched();
                    }
                } catch (Exception e) {
                    LogManager.getInstance().log(getClass(), "Error loading poe.ninja values from cache.");
                    fetchRates(league, true);
                }
            }
        } else {
            // Fetch online
            LogManager.getInstance().log(getClass(), "Fetching new currency values from poe.ninja.");
            JSONObject json = null;
            try {
                json = HttpManager.getInstance().getJson(currencyURL, "?league=" + league);
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "IOException!\n" + e);
            } catch (JSONException j){
                LogManager.getInstance().log(getClass(), "JSONException!\n" + j);
            }

            if (json == null || json.length() == 0) {
                LogManager.getInstance().log(getClass(), "Invalid response from PoeNinja! API has been changed.");
                return;
            }

            if(currentRates == null){
                currentRates = new HashMap<>();
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

            if (!currentRates.isEmpty() && file.canWrite()) {
                try {
                    objectMapper.writeValue(file, currentRates);
                } catch (Exception e) {
                    LogManager.getInstance().log(getClass(), "Writing ninja cache failed!\n" + e);
                }
            }

            if(listener != null){
                listener.onRatesFetched();
            }
        }
    }

    HashMap<CurrencyID, Float> getCurrentRates() {
        if(currentRates == null){
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

    public interface PoeNinjaListener{
        void onRatesFetched();
    }
}
