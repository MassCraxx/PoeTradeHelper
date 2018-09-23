package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.web.HttpManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PoeNinjaParser {
    public static final String currencyURL = "http://poe.ninja/api/Data/GetCurrencyOverview";
    private final ObjectMapper objectMapper;
    private final File file = new File("poeninja.dat");
    private boolean useOfflineCache = true;
    private long updateDelay = 6 * 60 * 60 * 1000; // 6 hours

    // Currency - (CurrencyID<>Chaos Value)
    private HashMap<CurrencyID, Float> currentRates;

    public PoeNinjaParser() {
        objectMapper = new ObjectMapper();
    }

    public void fetchRates(String league, boolean forceUpdate) {
        // If there is a file  and it is recent
        if (!forceUpdate && (useOfflineCache && file.exists() &&
                file.lastModified() + updateDelay > System.currentTimeMillis())) {

            // If no current rates have been loaded before, load from cache
            if (currentRates == null || currentRates.isEmpty()) {
                TypeReference<HashMap<CurrencyID, Float>> typeRef = new TypeReference<HashMap<CurrencyID, Float>>() {};
                try {
                    LogManager.getInstance().log(getClass(), "Loading poe.ninja currency values from cache.");
                    currentRates = objectMapper.readValue(file, typeRef);
                } catch (IOException e) {
                    e.printStackTrace();
                    currentRates = new HashMap<>();
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
            }

            if (json == null || json.length() == 0) {
                LogManager.getInstance().log(getClass(), "Invalid response");
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

            if (!currentRates.isEmpty()) {
                try {
                    objectMapper.writeValue(file, currentRates);
                } catch (IOException e) {
                    LogManager.getInstance().log(getClass(), "Writing ninja cache failed.\n" + e);
                }
            }
        }
    }

    public HashMap<CurrencyID, Float> getCurrentRates() {
        fetchRates(PropertyManager.getInstance().getCurrentLeague(), false);
        if (currentRates == null) {
            currentRates = new HashMap<>();
        }
        return currentRates;
    }

    public Float getCurrentCValueFor(CurrencyID id) {
        if (id == CurrencyID.CHAOS) {
            return 1f;
        }
        Float value = getCurrentRates().get(id);

        return value == null ? 0 : value;
    }

    public Float getCurrentValue(CurrencyID what, CurrencyID inWhat) {
        Float whatValue = getCurrentCValueFor(what);
        Float inWhatValue = getCurrentCValueFor(inWhat);

        if (inWhatValue == null || inWhatValue == 0) {
            return 0f;
        }

        return whatValue / inWhatValue;
    }
}
