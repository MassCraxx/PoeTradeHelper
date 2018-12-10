package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.web.HttpManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by mcrass on 12.08.2018.
 */
public class PoeApiParser {
    private final static String leagueParseURL = "http://api.pathofexile.com/leagues";
    private final static String leagueParams = "?type=main&compact=1";

    private ObservableList<String> currentLeagues = FXCollections.observableArrayList();

    public PoeApiParser() {
        updateLeagues();
    }

    public void updateLeagues() {
        try {
            currentLeagues.clear();

            LogManager.getInstance().log(PoeApiParser.class, "Fetching current leagues...");
            JSONArray jsonArray = new JSONArray(HttpManager.getInstance().get(leagueParseURL, leagueParams));
            if (jsonArray.length() == 0) {
                LogManager.getInstance().log(getClass(), "Error: Could not retrieve leagues.");
                currentLeagues.add("Standard");
                return;
            }

            for (Object object : jsonArray) {
                if (object instanceof JSONObject) {
                    JSONObject json = (JSONObject) object;
                    String league = json.getString("id");
                    if (!league.contains("SSF")) {
                        currentLeagues.add(league);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!currentLeagues.contains(PropertyManager.getInstance().getCurrentLeague())) {
            LogManager.getInstance().log(getClass(), "Saved league " + PropertyManager.getInstance().getCurrentLeague() + " is not valid anymore. Resetting to Standard.");
            PropertyManager.getInstance().resetLeague();
        }
    }

    public ObservableList<String> getCurrentLeagues() {
        if (currentLeagues.isEmpty()) {
            updateLeagues();
        }
        return currentLeagues;
    }
}
