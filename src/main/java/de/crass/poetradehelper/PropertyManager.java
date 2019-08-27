package de.crass.poetradehelper;

import de.crass.poetradehelper.model.CurrencyID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by mcrass on 19.07.2018.
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal", "unused"})
public class PropertyManager {
    private static PropertyManager instance;
    // DEBUG
    public final static boolean offlineMode = false;

    private Properties appProps;
    private String propFilename = "app.properties";

    // KEYS
    public static final String LEAGUE_KEY = "league";
    public static final String PRIMARY_CURRENCY = "primary_currency";
    public static final String FILTER_NOAPI = "filter_noapi";
    public static final String FILTER_OUTOFSTOCK = "filter_outofstock";
    public static final String FILTER_EXCESSIVE = "filter_excessive";
    public static final String EXCESSIVE_TRESHOLD = "excessive_treshold";
    public static final String CURRENCY_LIST = "currency_list";
    public static final String PRIMARY_CURRENCY_LIST = "primary_currency_list";
    public static final String PLAYER_LIST = "player_list";
    public static final String POE_PATH = "poe_path";
    public static final String VOICE_VOLUME = "voice_volume";
    public static final String VOICE_CHAT = "voice_read_chat";
    public static final String VOICE_TRADE = "voice_read_trade_offers";
    public static final String VOICE_CURRENCY = "voice_read_currency_offers";
    public static final String VOICE_SPEAKER = "voice_speaker";
    public static final String VOICE_AFK = "voice_read_afk";
    public static final String VOICE_RANDOMIZE = "voice_randomize_messages";
    public static final String UPDATE_DELAY_MINUTES = "auto_update_delay_minutes";
    public static final String FILTER_MULTIPLE_TRANSACTIONS = "filter_multiple_transactions";

    // DEFAULTS
    private final String defaultLeague = "Standard";
    private final String defaultPrimary = "EXALTED";
    private final String defaultPoePath = "C:\\Program Files (x86)\\Grinding Gear Games\\Path of Exile\\";

    private final String defaultFilterStockOffers = "false";
    private final String defaultFilterInvalidStockOffers = "true";
    private final String defaultFilterExcessive = "true";
    private final String defaultExcessiveTreshold = "75";

    private final String defaultCurrencyFilterString = "ALCHEMY,SCOURING,ALTERATION,REGAL,CHROMATIC,GCP,CHISEL,JEWELLER,REGRET,FUSING";
    private final String defaultPrimaryCurrencyString = "ANCIENT,ANNULMENT,DIVINE,EXALTED,HARBINGER,MASTER";

    // Current Values
    private ObservableList<CurrencyID> currencyFilterList;
    private ObservableList<CurrencyID> primaryCurrencyList;
    private ObservableList<String> playerList;

    private CurrencyID primaryCurrency;
    private boolean filterNoApi;
    private boolean filterOutOfStock;
    private boolean filterExcessive;
    private int updateDelay;
    private int excessiveTreshold;
    private String currentLeague;
    private boolean filterMultipleTransactionDeals;

    private PropertyManager() {
        loadProperties();
    }

    private void loadProperties() {
        appProps = new Properties();

        // Try load from disk
        File propFile = new File(propFilename);
        if(propFile.exists() && propFile.canRead()) {
            try {
                appProps.load(new FileInputStream(propFile));
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Could not read properties from disk! File may be corrupted...");
            }
        }

        // Only converting on load and store
        currentLeague = appProps.getProperty(LEAGUE_KEY, defaultLeague);
        currencyFilterList = FXCollections.observableArrayList(stringToCurrencyList(appProps.getProperty(CURRENCY_LIST, defaultCurrencyFilterString)));
        primaryCurrencyList = FXCollections.observableArrayList(stringToCurrencyList(appProps.getProperty(PRIMARY_CURRENCY_LIST, defaultPrimaryCurrencyString)));
        playerList = FXCollections.observableArrayList(stringToList(appProps.getProperty(PLAYER_LIST, null)));
        primaryCurrency = CurrencyID.valueOf(appProps.getProperty(PRIMARY_CURRENCY, defaultPrimary));

        filterNoApi = Boolean.parseBoolean(appProps.getProperty(FILTER_NOAPI, defaultFilterStockOffers));
        filterOutOfStock = Boolean.parseBoolean(appProps.getProperty(FILTER_OUTOFSTOCK, defaultFilterInvalidStockOffers));
        filterExcessive = Boolean.parseBoolean(appProps.getProperty(FILTER_EXCESSIVE, defaultFilterExcessive));
        excessiveTreshold = Integer.parseInt(appProps.getProperty(EXCESSIVE_TRESHOLD, defaultExcessiveTreshold));

        filterMultipleTransactionDeals = Boolean.parseBoolean(appProps.getProperty(FILTER_MULTIPLE_TRANSACTIONS, "false"));

        updateDelay = Integer.parseInt(appProps.getProperty(UPDATE_DELAY_MINUTES, "5"));
    }

    void storeProperties() {
        appProps.setProperty(LEAGUE_KEY, getCurrentLeague());
        appProps.setProperty(CURRENCY_LIST, currencyListToString(currencyFilterList));
        appProps.setProperty(PRIMARY_CURRENCY_LIST, currencyListToString(primaryCurrencyList));
        appProps.setProperty(PLAYER_LIST, listToString(playerList));
        appProps.setProperty(PRIMARY_CURRENCY, primaryCurrency.name());

        appProps.setProperty(FILTER_NOAPI, String.valueOf(filterNoApi));
        appProps.setProperty(FILTER_OUTOFSTOCK, String.valueOf(filterOutOfStock));
        appProps.setProperty(FILTER_EXCESSIVE, String.valueOf(filterExcessive));
        appProps.setProperty(EXCESSIVE_TRESHOLD, String.valueOf(excessiveTreshold));

        appProps.setProperty(UPDATE_DELAY_MINUTES, String.valueOf(updateDelay));

        appProps.setProperty(FILTER_MULTIPLE_TRANSACTIONS, String.valueOf(filterMultipleTransactionDeals));

        try {
            appProps.store(new FileWriter(propFilename), "PoeTradeHelper Properties");
        } catch (IOException e) {
            LogManager.getInstance().log(this.getClass(), "Could not write properties to disk!");
        }
    }

    public static PropertyManager getInstance() {
        if (instance == null) {
            instance = new PropertyManager();
        }
        return instance;
    }

    public CurrencyID getPrimaryCurrency() {
        return primaryCurrency;
    }

    public String getCurrentLeague() {
        return currentLeague;
    }

    public ObservableList<CurrencyID> getFilterList() {
        return currencyFilterList;
    }

    public void setPrimaryCurrency(CurrencyID primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }

    public ObservableList<String> getPlayerList() {
        return playerList;
    }

    public boolean getFilterNoApi() {
        return filterNoApi;
    }

    public boolean getFilterOutOfStock() {
        return filterOutOfStock;
    }

    public boolean getFilterExcessive() { return filterExcessive;}

    public void setFilterNoApi(boolean filterNoApi) {
        this.filterNoApi = filterNoApi;
    }

    public void setFilterOutOfStock(boolean filterOutOfStock) {
        this.filterOutOfStock = filterOutOfStock;
    }

    public void setFilterExcessive(boolean filterExcessive) {
        this.filterExcessive = filterExcessive;
    }

    public void setLeague(String league) {
        if(league == null){
            LogManager.getInstance().log(getClass(), "Prevented setting null as league.");
            return;
        }
        LogManager.getInstance().log(getClass(), "Setting " + league + " as new league.");
        currentLeague = league;
    }

    public void setProp(String key, String value) {
        appProps.setProperty(key, value);
    }

    public String getProp(String key) {
        return appProps.getProperty(key);
    }

    public String getProp(String key, String defaultValue) {
        String result = getProp(key);
        if (defaultValue != null && (result == null || result.isEmpty())) {
            result = defaultValue;
            setProp(key, result);
        }
        return result;
    }

    public String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return Main.join(list);
    }

    public List<String> stringToList(String s) {
        if (s == null || s.isEmpty()) {
            return new LinkedList<>();
        }
        return Arrays.asList(s.split(","));
    }

    public String currencyListToString(List<CurrencyID> list) {
        if(list == null || list.isEmpty()){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CurrencyID currencyID : list) {
            sb.append(currencyID.name());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public List<CurrencyID> stringToCurrencyList(String s) {
        List<CurrencyID> list = new LinkedList<>();
        if(s != null && !s.isEmpty()) {
            for (String currency : s.split(",")) {
                try {
                    CurrencyID id = CurrencyID.valueOf(currency.toUpperCase());
                    list.add(id);
                } catch (IllegalArgumentException e) {
                    LogManager.getInstance().log(getClass(), "Error parsing currencyID: " + currency);

                }
            }
        }
        return list;
    }

    public String getPathOfExilePath() {
        String path = getProp(POE_PATH, defaultPoePath);
        if(path.isEmpty()){
            path = defaultPoePath;
            setPathOfExilePath(path);
        }
        return path;
    }

    public void setPathOfExilePath(String path) {
        appProps.setProperty(POE_PATH, path);
    }

    public int getVoiceVolume() {
        return Integer.parseInt(getProp(VOICE_VOLUME, "100"));
    }

    public void setVoiceVolume(String volume) {
        appProps.setProperty(VOICE_VOLUME, volume);
    }

    public void resetFilterList() {
        currencyFilterList.clear();
        currencyFilterList.addAll(stringToCurrencyList(defaultCurrencyFilterString));
    }

    public int getUpdateDelay() {
        return updateDelay;
    }

    public void setUpdateDelay(int updateDelay) {
        this.updateDelay = updateDelay;
    }

    public void resetLeague() {
        JOptionPane.showMessageDialog(null, "League ended - resetting to " + defaultLeague+".");
        setLeague(defaultLeague);
    }

    public int getExcessiveTreshold() {
        return excessiveTreshold;
    }

    public void setExcessiveTreshold(int excessiveTreshold) {
        this.excessiveTreshold = excessiveTreshold;
    }

    public boolean getFilterMultipleTransactionDeals() {
        return filterMultipleTransactionDeals;
    }

    public void setFilterMultipleTransactionDeals(boolean filterMultipleTransactionDeals) {
        this.filterMultipleTransactionDeals = filterMultipleTransactionDeals;
    }

    ObservableList<CurrencyID> getPrimaryCurrencyList() {
        return primaryCurrencyList;
    }
}
