package de.crass.poetradeparser;

import com.sun.deploy.util.StringUtils;
import de.crass.poetradeparser.model.CurrencyID;
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

import static de.crass.poetradeparser.model.CurrencyID.*;

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
    private final String LEAGUE_KEY = "league";
    private final String PRIMARY_CURRENCY = "primary_currency";
    private final String FILTER_NOAPI = "filter_noapi";
    private final String FILTER_OUTOFSTOCK = "filter_outofstock";
    private final String CURRENCY_LIST = "currency_list";
    private final String PLAYER_LIST = "player_list";
    private final String POE_PATH = "poe_path";
    private final String VOICE_VOLUME = "voice_volume";

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
    boolean filterNoApi;
    boolean filterOutOfStock;

    private PropertyManager() {
        loadProperties();
    }

    private void loadProperties() {
        appProps = new Properties();
        try {
            appProps.load(new FileInputStream(propFilename));

            if(appProps.isEmpty()){
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
        appProps.setProperty(POE_PATH, defaultPoePath);

        // Following are not queried from props, will be stored on quit
        primaryCurrency = defaultPrimary;
        currencyFilterList = FXCollections.observableArrayList(defaultCurrencyFilterList);
        playerList = FXCollections.observableArrayList();

        storeProperties();
    }

    public void storeProperties() {
        appProps.setProperty(CURRENCY_LIST, currencyListToString(currencyFilterList));
        appProps.setProperty(PLAYER_LIST,listToString(playerList));
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
        this.filterNoApi= filterNoApi;
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

    public String listToString(List<String> list) {
        if(list == null || list.isEmpty()){
            return "";
        }
        return StringUtils.join(list, ",");
    }

    public List<String> stringToList(String s) {
        if(s == null || s.isEmpty()){
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
        sb.deleteCharAt(sb.length()-1);
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
        return Paths.get(appProps.getProperty(POE_PATH));
    }

    public int getVoiceVolume() {
        return Integer.parseInt(appProps.getProperty(VOICE_VOLUME, "100"));
    }
}
