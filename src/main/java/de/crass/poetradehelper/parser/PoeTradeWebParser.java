package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.web.HttpManager;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.crass.poetradehelper.PropertyManager.offlineMode;

public class PoeTradeWebParser extends WebParser{
    public static final String IDENTIFIER = "poe.trade";

    private final static String poeTradeCurrencyURL = "http://currency.poe.trade/search";

    private final static int parseStartIndex = 435845;
    private final static int fetchDelay = 100;
    private final static boolean writeCache = false;

    private final static Pattern OFFER_PATTERN = Pattern.compile(
            "class=\"displayoffer \" " +
                    "data-username=\"(.+)\" " +
                    "data-sellcurrency=\"(.+)\" " +
                    "data-sellvalue=\"(.+)\" " +
                    "data-buycurrency=\"(.+)\" " +
                    "data-buyvalue=\"(.+)\" " +
                    "data-ign=\"(.+?)\"(.+)>");

    private final static Pattern OFFER_PATTERN_STOCK = Pattern.compile(
            "data-stock=\"(.+)\"");

    PoeTradeWebParser(OfferParseListener listener) {
        super(listener);
    }

    public void fetchOffers(CurrencyID primary, CurrencyID secondary, String league) throws IOException, InterruptedException {
        String buyResponseBody;
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(primary.toString() + "-" + secondary.toString() + ".html");
        if (!offlineMode) {
            LogManager.getInstance().log(getClass(), "Fetching " + secondary + " offers for " + primary);

            buyResponseBody = HttpManager.getInstance().get(poeTradeCurrencyURL, getBuyQuery(league, primary, secondary));

            if (writeCache && buyResponseBody != null) {
                objectMapper.writeValue(file, buyResponseBody);
            }
        } else {
            LogManager.getInstance().log(getClass(), "Offline Fetching " + secondary + " offers for " + primary);
            if (file.exists()) {
                buyResponseBody = objectMapper.readValue(file, String.class);
            } else {
                LogManager.getInstance().log(getClass(), "No file found for: " + primary + " - " + secondary);
                return;
            }
        }

        if (buyResponseBody == null) {
            LogManager.getInstance().log(getClass(), "Fetching offers failed! No Internet connection?");
            return;
        }

        Pair<CurrencyID, CurrencyID> key = new Pair<>(secondary, primary);
        if (!parseOffers(buyResponseBody)) {
            LogManager.getInstance().log(getClass(), "No offers found for " + key);
        }

        if (!offlineMode)
            Thread.sleep(fetchDelay);
    }

    private static String getBuyQuery(String league, CurrencyID want, CurrencyID have) {
        String leagueParam = league;
        try {
            leagueParam = URLEncoder.encode(league, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogManager.getInstance().log(PoeTradeWebParser.class, "Error encoding league: " + leagueParam);
        }
        return "?league=" + leagueParam + "&online=x&want=" + want.getID() + "&have=" + have.getID();
    }

    private boolean parseOffers(String responseBody) {
        Matcher offerMatcher = OFFER_PATTERN.matcher(responseBody);

        if (responseBody.length() < parseStartIndex) {
            LogManager.getInstance().log(getClass(), "Response invalid!");
        } else if (!offerMatcher.find(parseStartIndex)) {
            return false;
        } else {
            do {
                if (offerMatcher.groupCount() >= 6) {
                    String stockString = offerMatcher.group(7);
                    int stock = -1;
                    if (!stockString.isEmpty()) {
                        Matcher stockMatcher = OFFER_PATTERN_STOCK.matcher(stockString);
                        if (stockMatcher.find()) {
                            stock = Integer.parseInt(stockMatcher.group(1));
                        }
                    }
                    String charName = offerMatcher.group(6);
                    String accountName = offerMatcher.group(1);
                    float sell = Float.parseFloat(offerMatcher.group(3));
                    float buy = Float.parseFloat(offerMatcher.group(5));

                    CurrencyOffer offer = new CurrencyOffer(
                            charName,
                            accountName,
                            CurrencyID.get(Integer.parseInt(offerMatcher.group(2))),
                            sell,
                            CurrencyID.get(Integer.parseInt(offerMatcher.group(4))),
                            buy,
                            stock,
                            System.currentTimeMillis());

                    addOffer(offer);
                } else {
                    LogManager.getInstance().log(getClass(), "Only found " + offerMatcher.groupCount() + " groups in offer... Insufficient data!");
                }
            } while (offerMatcher.find());
        }
        return true;
    }

    public boolean isUpdating() {
        return updating;
    }

    public static String getSearchURL(CurrencyID want, CurrencyID have) {
        return poeTradeCurrencyURL + getBuyQuery(PropertyManager.getInstance().getCurrentLeague(), want, have);
    }
}
