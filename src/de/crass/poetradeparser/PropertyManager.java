package de.crass.poetradeparser;

import de.crass.poetradeparser.model.CurrencyID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;

import static de.crass.poetradeparser.model.CurrencyID.*;

/**
 * Created by mcrass on 19.07.2018.
 */
public class PropertyManager {
    private static PropertyManager instance;

    private String currentLeague = "Incursion";
    private CurrencyID primaryCurrency = EXALTED;
    public final static boolean offlineMode = false;
    public static boolean filterStockOffers = true;
    public static boolean filterValidStockOffers = true;


    private final ObservableList<CurrencyID>  defaultCurrencyFilter = FXCollections.observableArrayList(
            ALCHEMY,
            SCOURING,
            ALTERATION,
            REGAL,
            CHROMATIC,
            CHANCE,
            GCP);

    private ObservableList<String> playerCharacterNames =  FXCollections.observableArrayList("SenorDingDong", "FlashZoomDead");

    private ObservableList<CurrencyID> filterList = defaultCurrencyFilter;

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
        return filterList;
    }

    public void setPrimaryCurrency(CurrencyID primaryCurrency) {
        this.primaryCurrency = primaryCurrency;
    }

    public ObservableList<String> getPlayerList() {
        return playerCharacterNames;
    }

    public void addPlayer(String characterName){
        playerCharacterNames.add(characterName);
    }
    public void removePlayer(String characterName){
        playerCharacterNames.remove(characterName);
    }

    public boolean isFilterStockOffers() {
        return filterStockOffers;
    }

    public boolean isFilterValidStockOffers() {
        return filterValidStockOffers;
    }

    public static void setImage(String name, ImageView view){
        String url = "./res/" + name;
        File iconFile = new File(url);
        InputStream fs = null;
        if (iconFile.exists()) {
            try {
                fs = new FileInputStream(iconFile);
                view.setImage(new Image(fs));
            } catch (FileNotFoundException | IllegalArgumentException e) {
                LogManager.getInstance().log(PropertyManager.class, "Exception on loading image! " + e);
            } finally {
                if(fs != null){
                    try {
                        fs.close();
                    } catch (IOException e) {
                        LogManager.getInstance().log(PropertyManager.class, "Exception on loading image! " + e);
                    }
                }
            }
        } else {
            LogManager.getInstance().log(PropertyManager.class, "Image " + url + " not found!");
        }
    }

    public void setLeague(String league) {
        currentLeague = league;
    }
}
