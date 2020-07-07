package de.crass.poetradehelper;

import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.parser.PoeTradeApiParser;
import javafx.application.Platform;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
    public static final String EXCESSIVE_TRESHOLD = "filter_excessive_treshold";
    public static final String CURRENCY_LIST = "secondary_currency_list";
    public static final String PRIMARY_CURRENCY_LIST = "primary_currency_list";
    private static final String PLAYER_ACCOUNT = "player_account";
    private static final String PLAYER_CHARACTER = "player_character";
    public static final String POE_PATH = "poe_path";
    public static final String VOICE_VOLUME = "voice_volume";
    public static final String VOICE_SPEAKER = "voice_speaker";
    public static final String UPDATE_DELAY_MINUTES = "auto_update_delay_minutes";
    public static final String FILTER_MULTIPLE_TRANSACTIONS = "filter_multiple_transactions";
    private static final String WINDOW_SIZE = "window_size";
    public static final String NOTIFY_INCOMING = "overlay_notify_incoming";
    public static final String NOTIFY_OUTGOING = "overlay_notify_outgoing";

    // DEFAULTS
    public static final Double defaultWindowWidth = 670.0;
    public static final Double defaultWindowHeight = 640.0;
    public static final String defaultLeague = null;

    private final String defaultPrimary = "exalted";
    private final String defaultPoePath = "C:\\Program Files (x86)\\Grinding Gear Games\\Path of Exile\\";

    private final String defaultFilterStockOffers = "false";
    private final String defaultFilterInvalidStockOffers = "false";
    private final String defaultFilterExcessive = "true";
    private final String defaultExcessiveTreshold = "65";

    private final String defaultCurrencyFilterString = "fusing,vaal,chisel,gcp,scour,alch,alt";
    private final String defaultPrimaryCurrencyString = "mirror,mirror-shard,exalted,blessing-chayula,divine,exalted-shard";

    // Current Values
    private ObservableList<CurrencyID> currencyFilterList;
    private ObservableList<CurrencyID> primaryCurrencyList;
    private ObservableList<String> ignoredPlayers;

    private CurrencyID primaryCurrency;
    private boolean filterNoStockInfo;
    private boolean filterOutOfStock;
    private boolean filterExcessive;
    private int excessiveTreshold;
    private String currentLeague;
    private ObservableStringValue test;
    private boolean filterMultipleTransactionDeals;

    private UICallback uiCallback;

    private double windowWidth;
    private double windowHeight;

    private PropertyManager() {
        loadProperties();
    }

    private void loadProperties() {
        appProps = new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<>(super.keySet()));
            }
        };

        // Try load from disk
        File propFile = new File(propFilename);
        if (propFile.exists() && propFile.canRead()) {
            try {
                appProps.load(new FileInputStream(propFile));
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Could not read properties from disk! File may be corrupted...");
            }
        }

        // Only converting on load and store
        currentLeague = appProps.getProperty(LEAGUE_KEY, defaultLeague);
        currencyFilterList = FXCollections.observableArrayList(stringToCurrencyList(appProps.getProperty(CURRENCY_LIST, appProps.getProperty("currency_list",defaultCurrencyFilterString))));
        primaryCurrencyList = FXCollections.observableArrayList(stringToCurrencyList(appProps.getProperty(PRIMARY_CURRENCY_LIST, defaultPrimaryCurrencyString)));
        primaryCurrency = CurrencyID.getByTradeID(appProps.getProperty(PRIMARY_CURRENCY, defaultPrimary));

        filterNoStockInfo = Boolean.parseBoolean(appProps.getProperty(FILTER_NOAPI, defaultFilterStockOffers));
        filterOutOfStock = Boolean.parseBoolean(appProps.getProperty(FILTER_OUTOFSTOCK, defaultFilterInvalidStockOffers));
        filterExcessive = Boolean.parseBoolean(appProps.getProperty(FILTER_EXCESSIVE, defaultFilterExcessive));
        excessiveTreshold = Integer.parseInt(appProps.getProperty(EXCESSIVE_TRESHOLD, defaultExcessiveTreshold));

        filterMultipleTransactionDeals = Boolean.parseBoolean(appProps.getProperty(FILTER_MULTIPLE_TRANSACTIONS, "false"));

        String[] windowSize = appProps.getProperty(WINDOW_SIZE, defaultWindowWidth + "," + defaultWindowHeight).split(",");
        windowWidth = Double.parseDouble(windowSize[0]);
        windowHeight = Double.parseDouble(windowSize[1]);
    }

    void storeProperties() {
        appProps.setProperty(LEAGUE_KEY, getCurrentLeague());
        appProps.setProperty(CURRENCY_LIST, currencyListToString(currencyFilterList));
        appProps.setProperty(PRIMARY_CURRENCY_LIST, currencyListToString(primaryCurrencyList));
        appProps.setProperty(PRIMARY_CURRENCY, primaryCurrency.getTradeID());

        appProps.setProperty(FILTER_NOAPI, String.valueOf(filterNoStockInfo));
        appProps.setProperty(FILTER_OUTOFSTOCK, String.valueOf(filterOutOfStock));
        appProps.setProperty(FILTER_EXCESSIVE, String.valueOf(filterExcessive));
        appProps.setProperty(EXCESSIVE_TRESHOLD, String.valueOf(excessiveTreshold));

        appProps.setProperty(FILTER_MULTIPLE_TRANSACTIONS, String.valueOf(filterMultipleTransactionDeals));

        appProps.setProperty(WINDOW_SIZE, windowWidth + "," + windowHeight);

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

    public String getPlayerAccount() {
        return getProp(PLAYER_ACCOUNT, "");
    }

    public void setPlayerAccount(String account) {
        setProp(PLAYER_ACCOUNT, account);
    }

    public String getPlayerCharacter() {
        return getProp(PLAYER_CHARACTER, "");
    }

    public void setPlayerCharacter(String player) {
        setProp(PLAYER_CHARACTER, player);
    }


    public boolean getFilterNoStockInfo() {
        return filterNoStockInfo;
    }

    public boolean getFilterOutOfStock() {
        return filterOutOfStock;
    }

    public boolean getFilterExcessive() {
        return filterExcessive;
    }

    public void setFilterNoStockInfo(boolean filterNoStockInfo) {
        this.filterNoStockInfo = filterNoStockInfo;
    }

    public void setFilterOutOfStock(boolean filterOutOfStock) {
        this.filterOutOfStock = filterOutOfStock;
    }

    public void setFilterExcessive(boolean filterExcessive) {
        this.filterExcessive = filterExcessive;
    }

    public void setLeague(String league) {
        if (league == null) {
            LogManager.getInstance().log(getClass(), "Prevented setting null as league.");
            return;
        }
        if (league.equals(currentLeague)) {
            return;
        }
        LogManager.getInstance().log(getClass(), "Setting " + league + " as new league.");
        currentLeague = league;

        callbackUI(LEAGUE_KEY, league);
    }

    public void setProp(String key, String value) {
        appProps.setProperty(key, value);
        callbackUI(key, value);
    }

    public String getProp(String key) {
        return appProps.getProperty(key);
    }

    public String getProp(String key, String defaultValue) {
        String result = getProp(key);
        if (defaultValue != null && (result == null || result.isEmpty())) {
            result = defaultValue;
            if (!key.startsWith("auto_update")) {
                setProp(key, result);
            }
        }
        return result;
    }

    public String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return join(list);
    }

    public List<String> stringToList(String s) {
        if (s == null || s.isEmpty()) {
            return new LinkedList<>();
        }
        return Arrays.asList(s.split(","));
    }

    public String currencyListToString(List<CurrencyID> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CurrencyID currencyID : list) {
            sb.append(currencyID.getTradeID());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public List<CurrencyID> stringToCurrencyList(String s) {
        List<CurrencyID> list = new LinkedList<>();
        if (s != null && !s.isEmpty()) {
            for (String currency : s.split(",")) {
                try {
                    CurrencyID id = CurrencyID.getByTradeID(currency.toLowerCase());
                    if (id == null) {
                        LogManager.getInstance().log(getClass(), "Error parsing currencyID: " + currency);
                        continue;
                    }
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
        if (path.isEmpty()) {
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
        return Integer.parseInt(getProp(UPDATE_DELAY_MINUTES, "5"));
    }

    public void setUpdateDelay(int updateDelay) {
        setProp(UPDATE_DELAY_MINUTES, String.valueOf(updateDelay));
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

    public ObservableList<CurrencyID> getPrimaryCurrencyList() {
        return primaryCurrencyList;
    }


    private void callbackUI(String key, String value) {
        if (uiCallback != null) {
            Platform.runLater(() -> uiCallback.onPropChanged(key, value));
        }
    }

    public void setUICallback(UICallback call) {
        uiCallback = call;
    }

    public boolean getBooleanProp(String key, boolean def) {
        return Boolean.parseBoolean(getProp(key, String.valueOf(def)));
    }

    public ObservableList<String> getIgnoredPlayers() {
        if (ignoredPlayers == null) {
            ignoredPlayers = FXCollections.observableArrayList(stringToList(PropertyManager.getInstance().getProp("ignored_players", null)));
        }
        return ignoredPlayers;
    }

    public void addIgnoredPlayer(String player) {
        List<String> newList = getIgnoredPlayers();
        newList.add(player);
        setProp("ignored_players", listToString(newList));
    }

    public void removeIgnoredPlayer(String player) {
        ObservableList<String> newList = getIgnoredPlayers();
        newList.remove(player);
        ignoredPlayers = newList;
        setProp("ignored_players", listToString(newList));
    }

    public String getCurrentWebParser(){
        return PropertyManager.getInstance().getProp("trade_data_source", PoeTradeApiParser.IDENTIFIER);
    }

    public void setWindowSize(double newWindowWidth, double newWindowHeight) {
        if (!Double.isNaN(newWindowWidth)) {
            this.windowWidth = newWindowWidth;
        }
        if (!Double.isNaN(newWindowHeight)) {
            this.windowHeight = newWindowHeight;
        }
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }
    public interface UICallback {

        void onPropChanged(String key, String value);
    }

    public static String join(Collection<?> col) {
        StringBuilder result = new StringBuilder();

        for (Iterator<?> var3 = col.iterator(); var3.hasNext(); result.append((String) var3.next())) {
            if (result.length() != 0) {
                result.append(",");
            }
        }

        return result.toString();
    }
}
