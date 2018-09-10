package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.model.ItemOffer;
import de.crass.poetradehelper.model.PoeTradeQuery;
import de.crass.poetradehelper.web.HttpManager;
import okhttp3.MediaType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * https://app.swaggerhub.com/apis/Chuanhsing/poe/1.0.0#
 * Implement find items
 */

public class PoeTradeSearcher {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static String LEAGUE_KEY = "%LEAGUE%";

    public static final String poeTradeApiURL = "http://www.pathofexile.com/api/trade/search/" + LEAGUE_KEY;
    public static final String fetchURL = "https://www.pathofexile.com/api/trade/fetch/";


    public List<String> searchForItemType(String league, String type) {
        LogManager.getInstance().log(getClass(), "Searching for " + type + " in " + league + " League");
        String url = poeTradeApiURL.replace(LEAGUE_KEY, league);
        String query = new PoeTradeQuery(null, type).getQuery();
        try {
            JSONObject response = HttpManager.getInstance().postJSON(url, query);
            if (!response.has("result")) {
                LogManager.getInstance().log(getClass(), "Invalid Response! " + response);
                return null;
            }
            String searchID = response.getString("id");
            JSONArray offerIDs = response.getJSONArray("result");
            return fetchOffers(searchID, offerIDs, -1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> searchForItem(String league, String name, String type, int results) {
        LogManager.getInstance().log(getClass(), "Searching for " + name + " in " + league + " League");
        String url = poeTradeApiURL.replace(LEAGUE_KEY, league);
        String query = new PoeTradeQuery(name, type).getQuery();
        try {
            JSONObject response = HttpManager.getInstance().postJSON(url, query);
            if (!response.has("result")) {
                LogManager.getInstance().log(getClass(), "Invalid Response! " + response);
                return null;
            }
            String searchID = response.getString("id");
            JSONArray offerIDs = response.getJSONArray("result");
            return fetchOffers(searchID, offerIDs, results);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> fetchOffers(String searchID, JSONArray offerIDs, int results) {
        System.out.println("Fetching " + (results == -1 ? offerIDs.length() : results) + " offers.");
        List<String> result = new LinkedList<>();
        for (Object offerID : offerIDs) {
            if (result.size() == results) {
                break;
            }
            try {
                JSONObject response = HttpManager.getInstance().getJson(fetchURL, offerID + "?query=" + searchID);
                if (!response.has("result")) {
                    LogManager.getInstance().log(getClass(), "Fetching ItemOffer failed! " + response.toString());
                    return result;
                }
                result.add(response.toString());
                ItemOffer offer = new ItemOffer(response);
                System.out.println("Fetched ItemOffer from " + offer.getCharacterName() + ": " + offer.getPrice() + " " + offer.getCurrency());
                Thread.sleep(100);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
