package de.crass.poetradeparser;

import de.crass.poetradeparser.model.CurrencyID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Arrays;
import java.util.List;

import static de.crass.poetradeparser.model.CurrencyID.*;

/**
 * Created by mcrass on 19.07.2018.
 */
public class PropertyManager {
    private static PropertyManager instance;

    private String currentLeague = "Incursion";
    private CurrencyID primaryCurrency = EXALTED;

    private final List<CurrencyID> defaultCurrencyFilter = Arrays.asList(
            ALCHEMY,
            SCOURING,
            ALTERATION,
            REGAL,
            CHROMATIC,
            CHANCE);

    private ObservableList<String> playerCharacterNames =  FXCollections.observableArrayList("SenorDingDong", "FlashZoomDead");

    //TODO: Filter
    private List<CurrencyID> filterList = defaultCurrencyFilter;

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

    public List<CurrencyID> getFilterList() {
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
}
