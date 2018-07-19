package de.crass.poetradeparser.parser;

import de.crass.poetradeparser.web.HttpManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class StashChangeListener {

    private List<JSONObject> parsedData = new LinkedList<>();

    public StashChangeListener() {
        //fetch initial ID from poe.ninja?
        String initialID = "225851916-234904969-220932477-253800309-238923776";

        new Thread(() -> fetchStashes(initialID, true)).start();
    }

    private void fetchStashes(String next_id, boolean stream) {
        String url = "http://api.pathofexile.com/public-stash-tabs";
        if (next_id != null) {
            url += "?id=" + next_id;
        }
        try {
            JSONObject parsed = HttpManager.getInstance().readJsonFromUrl(url);
            parseStashes((JSONArray) parsed.get("stashes"));
            parsedData.add(parsed);
            next_id = String.valueOf(parsed.get("next_change_id"));
            System.out.println("Next ID is: " + next_id);
            if (stream && next_id != null && !next_id.isEmpty()) {
                Thread.sleep(500);
                fetchStashes(next_id, stream);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Fetched " + parsedData.size() + " updates.");
    }

    private void parseStashes(JSONArray stashes) {
        HashSet<String> leagues = new HashSet<>();
        for (Object stash : stashes) {
            if (stash instanceof JSONObject) {
                Object league = ((JSONObject) stash).get("currentLeague");
                if (league instanceof String) {
                    leagues.add((String) league);
                }
            }
        }
        System.out.println("Received " + stashes.length() + " stashes in " + leagues.size() + " leagues.");
    }

    public List<JSONObject> getParsedData() {
        return parsedData;
    }
}
