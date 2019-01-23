package de.crass.poetradehelper.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.web.HttpManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    //TODO: Switch to (observable) sets
    //TODO: https://stackoverflow.com/questions/1732348/regex-match-open-tags-except-xhtml-self-contained-tags

    private final static String poeTradeCurrencyURL = "http://currency.poe.trade/search";
    private HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> currentOffers = new HashMap<>();
    private HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers = new HashMap<>();

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

    private OfferParseListener parseListener;
    private boolean updating = false;
    private boolean cancel = false;

    PoeTradeWebParser(OfferParseListener listener) {
        this();
        setParseListener(listener);
    }

    private PoeTradeWebParser() {
        reset();
    }

    void reset() {
        for (List<CurrencyOffer> list : currentOffers.values()) {
            list.clear();
        }

        for (List<CurrencyOffer> list : playerOffers.values()) {
            list.clear();
        }
    }

    void updateCurrencies(List<CurrencyID> currencyList, boolean clear) {
        updateCurrencies(currencyList, clear, true);
    }

    void updateCurrencies(List<CurrencyID> currencyList, boolean clear, boolean async) {
        if (updating) {
            return;
        }
        if (parseListener != null) {
            Platform.runLater(() -> parseListener.onUpdateStarted());
        }

        if (clear) {
            reset();
        }

        if (async) {
            Thread runThread = new Thread(() -> doUpdate(currencyList, clear), "PoeTradeWebParser");

            runThread.setDaemon(true);
            runThread.start();
        } else {
            doUpdate(currencyList, clear);
        }
    }

    private void doUpdate(List<CurrencyID> currencyList, boolean clear) {
        updating = true;
        CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
        for (CurrencyID secondaryCurrency : currencyList) {
            if (cancel) {
                break;
            }
            if (!clear)
                removeOffers(primaryCurrency, secondaryCurrency);
            fetchCurrencyOffers(primaryCurrency, secondaryCurrency, PropertyManager.getInstance().getCurrentLeague());
        }

        if (parseListener != null) {
            Platform.runLater(() -> parseListener.onUpdateFinished());
        }
        cancel = false;
        updating = false;
    }

    void updateCurrency(CurrencyID secondaryCurrencyID, boolean async) {
        List<CurrencyID> list = new LinkedList<>();
        list.add(secondaryCurrencyID);
        updateCurrencies(list, false, async);
    }

    private void fetchCurrencyOffers(CurrencyID primary, CurrencyID secondary, String league) {
        try {
            // BUY
            fetchOffers(primary, secondary, league);

            // SELL
            fetchOffers(secondary, primary, league);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedAssignment")
    private void fetchOffers(CurrencyID primary, CurrencyID secondary, String league) throws IOException, InterruptedException {
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
                    LogManager.getInstance().log(getClass(), "Only found " + offerMatcher.groupCount() + " groups in offer... Insufficient data!");
                }
            } while (offerMatcher.find());
        }
        return true;
    }

    private void addOffer(CurrencyOffer offer) {
        Pair<CurrencyID, CurrencyID> key = new Pair<>(offer.getBuyID(), offer.getSellID());
        String offerPlayer = offer.getPlayerName();
        boolean isPlayerOffer = false;
        List<String> playerList = PropertyManager.getInstance().getPlayerList();
        if (playerList != null && !playerList.isEmpty()) {
            for (String playerName : playerList) {
                if (offerPlayer.equalsIgnoreCase(playerName)) {
                    isPlayerOffer = true;
                    break;
                }
            }
        }

        if (isPlayerOffer) {
            //LogManager.getInstance().log(getClass(), "Found player offer " + offer.getPlayerName());
            List<CurrencyOffer> offers = playerOffers.get(key);
            if (offers == null) {
                offers = new LinkedList<>();
            }
//            LogManager.getInstance().log(getClass(), "AddOffer - Key: " + key + " Offer: " + offer.toString());
            offers.add(offer);
            playerOffers.put(key, offers);
        } else {
            ObservableList<CurrencyOffer> offers = getOffersFor(key, false);
            offers.add(offer);
            currentOffers.put(key, offers);
        }
    }

    private void removeOffers(CurrencyID primary, CurrencyID secondary) {
        Pair<CurrencyID, CurrencyID> key = new Pair<>(primary, secondary);
        clearListIfPossible(currentOffers.get(key));
        clearListIfPossible(playerOffers.get(key));

        key = new Pair<>(secondary, primary);
        clearListIfPossible(currentOffers.get(key));
        clearListIfPossible(playerOffers.get(key));
    }

    private void clearListIfPossible(List list) {
        if (list != null && !list.isEmpty()) {
            list.clear();
        }
    }

    HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getCurrentOffers() {
        return currentOffers;
    }

    HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getPlayerOffers() {
        return playerOffers;
    }

    private void setParseListener(OfferParseListener parseListener) {
        this.parseListener = parseListener;
    }

    public boolean isUpdating() {
        return updating;
    }

    void cancel() {
        cancel = true;
    }

    private static String getPoeTradeURL(String league, CurrencyID want, CurrencyID have) {
        return poeTradeCurrencyURL + getBuyQuery(league, want, have);
    }

    public static void openInBrowser(String league, CurrencyID want, CurrencyID have) {
        try {
            Desktop.getDesktop().browse(URI.create(getPoeTradeURL(league, want, have)));
        } catch (Exception e) {
            LogManager.getInstance().log(PoeTradeWebParser.class, "Error opening browser. " + e);
        }
    }

    ObservableList<CurrencyOffer> getOffersFor(CurrencyID secondary, boolean sell) {
        return getOffersFor(new Pair<>(PropertyManager.getInstance().getPrimaryCurrency(), secondary), !sell);
    }

    ObservableList<CurrencyOffer> getOffersFor(Pair<CurrencyID, CurrencyID> key, boolean invert) {
        if (invert) {
            key = new Pair<>(key.getValue(), key.getKey());
        }

        List<CurrencyOffer> offers = currentOffers.get(key);
        if (offers == null) {
            // lists will be used in offers table, therefore must be observable
            offers = FXCollections.observableArrayList();
        }
        return (ObservableList<CurrencyOffer>) offers;
    }
    public interface OfferParseListener {
        void onUpdateStarted();

        void onUpdateFinished();
    }
}
