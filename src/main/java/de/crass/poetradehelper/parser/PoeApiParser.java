package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.web.HttpManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by mcrass on 12.08.2018.
 */
class PoeApiParser {
    private final static String leagueParseURL = "http://api.pathofexile.com/leagues";
    private final static String leagueParams = "?type=main&compact=1";

    private ObservableList<String> currentLeagues = FXCollections.observableArrayList();

    PoeApiParser() {
        updateLeagues();
    }

    private void updateLeagues() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    currentLeagues.clear();
                    LogManager.getInstance().log(PoeApiParser.class, "Fetching current leagues...");
                    String response = HttpManager.getInstance().get(leagueParseURL, leagueParams);

                    if (response == null) {
                        LogManager.getInstance().log(getClass(), "Fetching league failed! No connection to " + leagueParseURL);
                        if (PropertyManager.getInstance().getCurrentLeague() == null) {
                            PropertyManager.getInstance().setLeague("Standard");
                        }
                        return;
                    } else if (!response.startsWith("[")) {
                        LogManager.getInstance().log(getClass(), "Fetching league failed! PoE under maintenance?");
                        currentLeagues.add("Standard");
                        if (PropertyManager.getInstance().getCurrentLeague() == null) {
                            PropertyManager.getInstance().setLeague("Standard");
                        }
                        return;
                    }

                    JSONArray jsonArray = new JSONArray(response);
                    if (jsonArray.length() == 0) {
                        LogManager.getInstance().log(getClass(), "Error: Could not retrieve leagues.");
                        currentLeagues.add("Standard");
                        if (PropertyManager.getInstance().getCurrentLeague() == null) {
                            PropertyManager.getInstance().setLeague("Standard");
                        }
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

                    if (!currentLeagues.contains(PropertyManager.getInstance().getCurrentLeague())) {
                        String currentTempLeague = "Standard";

                        if (currentLeagues.size() > 2) {
                            currentTempLeague = currentLeagues.get(2);
                        }

                        if (PropertyManager.getInstance().getCurrentLeague() != null) {
                            LogManager.getInstance().log(PoeApiParser.class, "Saved league " + PropertyManager.getInstance().getCurrentLeague() + " is not valid anymore. Setting to " + currentTempLeague);
                            JOptionPane.showMessageDialog(null, "League " + PropertyManager.getInstance().getCurrentLeague() + " ended - setting to " + currentTempLeague + ".");
                        }
                        PropertyManager.getInstance().setLeague(currentTempLeague);
                    }
                } catch (IOException e) {
                    if (e instanceof UnknownHostException) {
                        LogManager.getInstance().log(getClass(), "Fetching league failed! No connection to " + leagueParseURL);
                    } else {
                        e.printStackTrace();
                    }
                    if (PropertyManager.getInstance().getCurrentLeague() == null) {
                        PropertyManager.getInstance().setLeague("Standard");
                    }
                }
            }
        }).start();
    }

    ObservableList<String> getCurrentLeagues() {
//        if (currentLeagues.isEmpty()) {
//            updateLeagues();
//        }
        return currentLeagues;
    }
}
