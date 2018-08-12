package de.crass.poetradeparser.parser;

import de.crass.poetradeparser.LogManager;
import de.crass.poetradeparser.web.HttpManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by mcrass on 12.08.2018.
 */
public class PoeApiParser {
    final static String leagueParseURL = "http://api.pathofexile.com/leagues";
    final static String leagueParams = "?type=main&compact=1";

    private ObservableList<String> currentLeagues = FXCollections.observableArrayList();

    PoeApiParser(){
        updateLeagues();
    }

    public void updateLeagues(){
        Thread runThread = new Thread(() -> {
            try {
                currentLeagues.clear();

                LogManager.getInstance().log(getClass(), "Fetching current leagues...");
                JSONArray jsonArray = new JSONArray(HttpManager.getInstance().get(leagueParseURL, leagueParams));
                if (jsonArray.length() == 0) {
                    LogManager.getInstance().log(getClass(), "Error: Could not retrieve leagues.");
                    currentLeagues.add("Standard");
                    return;
                }

                for (Object object : jsonArray) {
                    if (object instanceof JSONObject) {
                        JSONObject json = (JSONObject) object;
                        currentLeagues.add(json.getString("id"));
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        runThread.setDaemon(true);
        runThread.start();
    }

    public ObservableList<String> getCurrentLeagues() {
        return currentLeagues;
    }
}
