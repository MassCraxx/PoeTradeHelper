package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.web.HttpManager;
import javafx.application.Platform;
import javafx.util.Pair;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.crass.poetradehelper.PropertyManager.offlineMode;

public class PoeTradeWebParser {

    private final static String poeTradeCurrencyURL = "http://currency.poe.trade/search";
    private HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> currentOffers;
    private HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers;

    private final static int parseStartIndex = 435845;
    private final static int fetchDelay = 500;
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

    private ParseListener parseListener;
    private boolean updating = false;
    private boolean cancel = false;


    public PoeTradeWebParser() {
        reset();
    }

    public void reset() {
        currentOffers = new HashMap<>();
        playerOffers = new HashMap<>();
//        currentValidStockOffers = new HashMap<>();
    }

    void updateAll() {
        reset();
        Thread runThread = new Thread(() -> {
            updating = true;
            CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
            for (CurrencyID secondary : PropertyManager.getInstance().getFilterList()) {
                if (secondary != primaryCurrency) {
                    fetchCurrencyOffers(primaryCurrency, secondary, PropertyManager.getInstance().getCurrentLeague());
                }
            }
            if (parseListener != null) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        parseListener.onParsingFinished();
                    }
                });
            }
            cancel = false;
            updating = false;
        }, "PoeTradeWebParser");

        runThread.setDaemon(true);
        runThread.start();
    }

    public void updateCurrency(CurrencyID currencyID) {
        if (!updating) {
            Thread runThread = new Thread(() -> {
                updating = true;
                removeOffers(PropertyManager.getInstance().getPrimaryCurrency(), currencyID);
                CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
                fetchCurrencyOffers(primaryCurrency, currencyID, PropertyManager.getInstance().getCurrentLeague());

                if (parseListener != null) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            parseListener.onParsingFinished();
                        }
                    });
                }
                updating = false;
            }, "PoeTradeWebParser");

            runThread.setDaemon(true);
            runThread.start();
        }
    }

    public void fetchCurrencyOffers(CurrencyID primary, CurrencyID secondary, String league) {
        try {
            // BUY
            fetchOffers(primary, secondary, league);

            // SELL
            fetchOffers(secondary, primary, league);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void fetchOffers(CurrencyID primary, CurrencyID secondary, String league) throws IOException, InterruptedException {
        if (cancel) {
            return;
        }
        String buyResponseBody;
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(primary.toString() + "-" + secondary.toString() + ".html");
        if (!offlineMode) {
            LogManager.getInstance().log(getClass(), "Fetching " + secondary + " offers for " + primary);

            buyResponseBody = HttpManager.getInstance().get(poeTradeCurrencyURL, getBuyQuery(league, primary, secondary));

            if (writeCache) {
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

        Pair key = new Pair<>(secondary, primary);
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
                            stock = Integer.valueOf(stockMatcher.group(1));
                        }
                    }
                    float sell = Float.valueOf(offerMatcher.group(3));
                    float buy = Float.valueOf(offerMatcher.group(5));

                    CurrencyOffer offer = new CurrencyOffer(
                            offerMatcher.group(6),
                            offerMatcher.group(1),
                            CurrencyID.get(Integer.valueOf(offerMatcher.group(2))),
                            sell,
                            CurrencyID.get(Integer.valueOf(offerMatcher.group(4))),
                            buy,
                            stock);

                    addOffer(offer);
                } else {
                    LogManager.getInstance().log(getClass(), "Only found " + offerMatcher.groupCount() + " groups... Insufficient data!");
                }
            } while (offerMatcher.find());
        }
        return true;
    }

    private void addOffer(CurrencyOffer offer) {
        Pair<CurrencyID, CurrencyID> key = new Pair<>(offer.getBuyID(), offer.getSellID());
        if (PropertyManager.getInstance().getPlayerList().contains(offer.getPlayerName())) {
            //LogManager.getInstance().log(getClass(), "Found player offer " + offer.getPlayerName());
            List<CurrencyOffer> offers = playerOffers.get(key);
            if (offers == null) {
                offers = new LinkedList<>();
            }
            offers.add(offer);
            playerOffers.put(key, offers);
        } else {
            List<CurrencyOffer> offers = currentOffers.get(key);
            if (offers == null) {
                offers = new LinkedList<>();
            }
            offers.add(offer);
            currentOffers.put(key, offers);
        }
    }

    public void removeOffers(CurrencyID primary, CurrencyID secondary){
        Pair<CurrencyID, CurrencyID> key = new Pair<>(primary, secondary);
        currentOffers.remove(key);
        playerOffers.remove(key);

        key = new Pair<>(secondary, primary);
        currentOffers.remove(key);
        playerOffers.remove(key);
    }

    public HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getCurrentOffers() {
        return currentOffers;
    }

    public CurrencyOffer getBestOffer(List<CurrencyOffer> list) {
        return getBestOffer(list, PropertyManager.getInstance().getFilterNoApi(), PropertyManager.getInstance()
                .getFilterOutOfStock());
    }

    public CurrencyOffer getBestOffer(List<CurrencyOffer> list, boolean filterStockOffers, boolean filterValidStockOffers) {
        CurrencyOffer bestOffer = null;
        if (list == null) {
            return null;
        }
        for (CurrencyOffer offer : list) {
            // Return most top offer that meets filter requirements

            if ((filterStockOffers && offer.getStock() < 0) ||
                    (filterValidStockOffers && (offer.getStock() >= 0 && offer.getStock() < offer.getSellValue()))) {
                continue;
            }

            bestOffer = offer;
            break;
        }
        return bestOffer;
    }

    public HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getPlayerOffers() {
        return playerOffers;
    }

    public void setParseListener(ParseListener parseListener) {
        this.parseListener = parseListener;
    }

    public boolean isUpdating() {
        return updating;
    }

    public void cancel() {
        cancel = true;
    }

    public static String getPoeTradeURL(String league, CurrencyID want, CurrencyID have) {
        return poeTradeCurrencyURL + getBuyQuery(league, want, have);
    }

    public static void openInBrowser(String league, CurrencyID want, CurrencyID have) {
        try {
            Desktop.getDesktop().browse(URI.create(getPoeTradeURL(league, want, have)));
        } catch (Exception e) {
            LogManager.getInstance().log(PoeTradeWebParser.class, "Error opening browser. " + e);
        }
    }
}
