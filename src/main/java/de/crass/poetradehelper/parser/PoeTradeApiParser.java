package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.web.HttpManager;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

public class PoeTradeApiParser extends WebParser {
    public static final String IDENTIFIER = "pathofexile.com";

    public static final String poeTradeURL = "https://www.pathofexile.com/api/trade/exchange/";
    public static final String poeFetchURL = "https://www.pathofexile.com/api/trade/fetch/";
    public static final String poeSearchURL = "https://www.pathofexile.com/trade/exchange/";

    PoeTradeApiParser(OfferParseListener listener) {
        super(listener);
    }

    public void fetchOffers(CurrencyID primaryCurrency, CurrencyID secondaryCurrency, String currentLeague) {
        LogManager.getInstance().log(getClass(), "Fetching " + secondaryCurrency + " offers for " + primaryCurrency);

        JSONObject status = new JSONObject();
        status.put("option", "online");

        JSONArray have = new JSONArray();
        have.put(primaryCurrency.getTradeID());
        JSONArray want = new JSONArray();
        want.put(secondaryCurrency.getTradeID());

        JSONObject exchange = new JSONObject();
        exchange.put("status", status);
        exchange.put("have", have);
        exchange.put("want", want);

        JSONObject param = new JSONObject();
        param.put("exchange", exchange);

        try {
            JSONObject response = HttpManager.getInstance().postJSON(poeTradeURL + PropertyManager.getInstance().getCurrentLeague(), String.valueOf(param));

            JSONArray offers = response.getJSONArray("result");
            if(offers == null || offers.length() == 0){
                LogManager.getInstance().log(getClass(), "No offers found for ");
                return;
            }

            String id = response.getString("id");
            if (id == null || response.isNull("id") || id.isEmpty()) {
                LogManager.getInstance().log(getClass(), "Fetching failed! id was null.");
                return;
            }

            StringBuilder query = new StringBuilder();
            boolean first = true;
            int count = 0;

            for (Object offerQuery : offers) {
                count++;
                if (first) {
                    first = false;
                } else {
                    query.append(',');
                }
                query.append(offerQuery.toString());

                // Consider looping in 20er steps
                if (count == 20) {
                    break;
                }
            }

            JSONObject data = HttpManager.getInstance().getJson(poeFetchURL + query.toString(), "?query=" + id + "&exchange");
            JSONArray result = data.getJSONArray("result");

            for (Object object : result) {
                if(object != JSONObject.NULL) {
                    JSONObject json = (JSONObject) object;
                    JSONObject listing = json.getJSONObject("listing");
                    String charName = listing.getJSONObject("account").getString("lastCharacterName");
                    String account = listing.getJSONObject("account").getString("name");

                    JSONObject price = listing.getJSONObject("price");
                    CurrencyID buyID = CurrencyID.getByTradeID(price.getJSONObject("exchange").getString("currency"));
                    CurrencyID sellID = CurrencyID.getByTradeID(price.getJSONObject("item").getString("currency"));
                    float buyValue = price.getJSONObject("exchange").getFloat("amount");
                    float sellValue = price.getJSONObject("item").getFloat("amount");
                    int stock = price.getJSONObject("item").getInt("stock");

                    CurrencyOffer offer = new CurrencyOffer(charName, account, sellID, sellValue, buyID, buyValue, stock);
                    offer.setQueryId(id);

                    addOffer(offer);
                } else {
                    LogManager.getInstance().log(getClass(), "JSON result was null.");
                    System.out.println(result);
                }
            }
        } catch (Exception e) {
            Platform.runLater(() -> parseListener.onUpdateError());
            LogManager.getInstance().log(getClass(), e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getSearchURL(String queryID) {
        return poeSearchURL + PropertyManager.getInstance().getCurrentLeague() + '/' + queryID;
    }
}
