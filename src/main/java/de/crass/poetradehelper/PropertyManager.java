package de.crass.poetradehelper;

import de.crass.poetradehelper.model.CurrencyID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static de.crass.poetradehelper.model.CurrencyID.*;

/**
 * Created by mcrass on 19.07.2018.
 */
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
    public static final String CURRENCY_LIST = "currency_list";
    public static final String PLAYER_LIST = "player_list";
    public static final String POE_PATH = "poe_path";
    public static final String VOICE_VOLUME = "voice_volume";
    public static final String VOICE_CHAT = "voice_read_chat";
    public static final String VOICE_TRADE = "voice_read_trade_offers";
    public static final String VOICE_CURRENCY = "voice_read_currency_offers";
    public static final String VOICE_SPEAKER = "voice_speaker";
    public static final String VOICE_AFK = "voice_read_afk";
    public static final String VOICE_RANDOMIZE = "voice_randomize_messages";

    // DEFAULTS
    private final String defaultLeague = "Standard";
    private final CurrencyID defaultPrimary = EXALTED;
    private final String defaultPoePath = "G:\\Steam\\SteamApps\\common\\Path of Exile\\logs";

    private final boolean defaultFilterStockOffers = false;
    public final boolean defaultFilterInvalidStockOffers = true;

    private final List<CurrencyID> defaultCurrencyFilterList = Arrays.asList(
            ALCHEMY,
            SCOURING,
            ALTERATION,
            REGAL,
            CHROMATIC,
            CHANCE,
            GCP,
            CHISEL,
            JEWELLER);

    // Current Values
    private ObservableList<CurrencyID> currencyFilterList;
    private ObservableList<String> playerList;

    private CurrencyID primaryCurrency;
    private boolean filterNoApi;
    private boolean filterOutOfStock;

    private PropertyManager() {
        loadProperties();
    }

    private void loadProperties() {
        appProps = new Properties();
        try {
            appProps.load(new FileInputStream(propFilename));

            if (appProps.isEmpty()) {
                loadDefaults();
                return;
            }

            // Only converting on load and store
            currencyFilterList = FXCollections.observableArrayList(stringToCurrencyList(appProps.getProperty(CURRENCY_LIST)));
            playerList = FXCollections.observableArrayList(stringToList(appProps.getProperty(PLAYER_LIST)));
            primaryCurrency = CurrencyID.valueOf(appProps.getProperty(PRIMARY_CURRENCY));

            filterNoApi = Boolean.parseBoolean(appProps.getProperty(FILTER_NOAPI));
            filterOutOfStock = Boolean.parseBoolean(appProps.getProperty(FILTER_OUTOFSTOCK));

        } catch (IOException e) {
            loadDefaults();
        }
    }

    private void loadDefaults() {
        appProps.setProperty(LEAGUE_KEY, defaultLeague);
        appProps.setProperty(FILTER_NOAPI, String.valueOf(defaultFilterStockOffers));
        appProps.setProperty(FILTER_OUTOFSTOCK, String.valueOf(defaultFilterInvalidStockOffers));

        // Following are not queried from props, will be stored on quit
        primaryCurrency = defaultPrimary;
        currencyFilterList = FXCollections.observableArrayList(defaultCurrencyFilterList);
        playerList = FXCollections.observableArrayList();

        storeProperties();
    }

    public void storeProperties() {
        appProps.setProperty(CURRENCY_LIST, currencyListToString(currencyFilterList));
        appProps.setProperty(PLAYER_LIST, listToString(playerList));
        appProps.setProperty(PRIMARY_CURRENCY, primaryCurrency.name());

        appProps.setProperty(FILTER_NOAPI, String.valueOf(filterNoApi));
        appProps.setProperty(FILTER_OUTOFSTOCK, String.valueOf(filterOutOfStock));

        try {
            appProps.store(new FileWriter(propFilename), "PoeTradeHelper Properties");
        } catch (IOException e) {
            e.printStackTrace();
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
        return appProps.getProperty(LEAGUE_KEY);
    }

    public ObservableList<CurrencyID> getFilterList() {
        return currencyFilterList;
    }

    public void setPrimaryCurrency(CurrencyID primaryCurrency) {
        appProps.setProperty(PRIMARY_CURRENCY, String.valueOf(primaryCurrency));
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

    public void setFilterNoApi(boolean filterNoApi) {
        this.filterNoApi = filterNoApi;
    }

    public void setFilterOutOfStock(boolean filterOutOfStock) {
        this.filterOutOfStock = filterOutOfStock;
    }

    public void setLeague(String league) {
        appProps.setProperty(LEAGUE_KEY, league);
    }

    public Properties getAppProps() {
        return appProps;
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
        return Main.join(list, ",");
    }

    public List<String> stringToList(String s) {
        if (s == null || s.isEmpty()) {
            return new LinkedList<>();
        }
        return Arrays.asList(s.split(","));
    }

    public String currencyListToString(List<CurrencyID> list) {
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
        for (String currency : s.split(",")) {
            try {
                CurrencyID id = CurrencyID.valueOf(currency.toUpperCase());
                list.add(id);
            } catch (IllegalArgumentException e) {
                LogManager.getInstance().log(getClass(), "Error parsing currencyID: " + currency);

            }
        }
        return list;
    }

    public Path getPathOfExilePath() {
        return Paths.get(getProp(POE_PATH, defaultPoePath));
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
        currencyFilterList.addAll(defaultCurrencyFilterList);
    }
}
