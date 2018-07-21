package de.crass.poetradeparser.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.crass.poetradeparser.LogManager;
import de.crass.poetradeparser.PropertyManager;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.model.CurrencyOffer;
import de.crass.poetradeparser.web.HttpManager;
import javafx.application.Platform;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.crass.poetradeparser.PropertyManager.offlineMode;

public class PoeTradeWebParser {

    private final static String poeTradeCurrencyURL = "http://currency.poe.trade/search";
    private HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> currentOffers;
    private HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers;

    private final static int parseStartIndex = 435845;
    private final static int fetchDelay = 800;

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


    public PoeTradeWebParser() {
        reset();
    }

    public void reset() {
        currentOffers = new HashMap<>();
        playerOffers = new HashMap<>();
//        currentValidStockOffers = new HashMap<>();
    }

    void update() {
        reset();
        Thread runThread = new Thread(() -> {
            CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
            for (Object secondary : PropertyManager.getInstance().getFilterList().toArray()) {
                if (secondary != primaryCurrency) {
                    fetchCurrencyOffers(primaryCurrency, (CurrencyID) secondary, PropertyManager.getInstance().getCurrentLeague());
                }
            }
            if(parseListener != null){
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        parseListener.onParsingFinished();
                    }
                });
            }
        }, "PoeTradeWebParser");
        runThread.setDaemon(true);

        runThread.start();
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
        String buyResponseBody;
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(primary.toString() + "-" + secondary.toString() + ".html");
        if (!offlineMode) {
            LogManager.getInstance().log(getClass(), "Fetching " + secondary + " offers for " + primary);
            String buyQuery = "?league=" + league + "&online=x&want=" + primary.getID() + "&have=" + secondary.getID();
            buyResponseBody = HttpManager.getInstance().get(poeTradeCurrencyURL, buyQuery);

            objectMapper.writeValue(file, buyResponseBody);
        } else {
            LogManager.getInstance().log(getClass(), "Offline Fetching " + secondary + " offers for " + primary);
            if (file.exists()) {
                buyResponseBody = objectMapper.readValue(file, String.class);
            } else {
                LogManager.getInstance().log(getClass(), "No file found for: " + primary + " - " + secondary);
                return;
            }
        }
        parseOffers(buyResponseBody);

        Pair key = new Pair<>(secondary, primary);
        List<CurrencyOffer> currentOffer = currentOffers.get(key);
        if(currentOffer == null){
            LogManager.getInstance().log(getClass(), "No offers found for " + key);
        }

        if(!offlineMode)
        Thread.sleep(fetchDelay);
    }

    private void parseOffers(String responseBody) {
        Matcher offerMatcher = OFFER_PATTERN.matcher(responseBody);

        if (!offerMatcher.find(parseStartIndex)) {
            LogManager.getInstance().log(getClass(), "No match found in Response: " + responseBody);
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
    }

    private void addOffer(CurrencyOffer offer) {
        Pair<CurrencyID, CurrencyID> key = new Pair<>(offer.getBuyID(), offer.getSellID());
        if (PropertyManager.getInstance().getPlayerList().contains(offer.getPlayerName())) {
            LogManager.getInstance().log(getClass(), "Found player offer " + offer.getPlayerName());
            List<CurrencyOffer> offers = playerOffers.get(key);
            if (offers == null) {
                offers = new LinkedList<>();
            }
            offers.add(offer);
            playerOffers.put(key, offers);
        } else {
//            if (offer.getStock() > offer.getSellValue()) {
//                List<CurrencyOffer> stockOffers = currentValidStockOffers.get(key);
//                if (stockOffers == null) {
//                    stockOffers = new LinkedList<>();
//                }
//                stockOffers.add(offer);
//                currentValidStockOffers.put(key, stockOffers);
//            }
            List<CurrencyOffer> offers = currentOffers.get(key);
            if (offers == null) {
                offers = new LinkedList<>();
            }
            offers.add(offer);
            currentOffers.put(key, offers);
        }
    }

    public HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getCurrentOffers() {
        return currentOffers;
    }

    // FIXME
    public CurrencyOffer getBestOffer() {
        return null;
    }

    public CurrencyOffer getBestOfferForKey(List<CurrencyOffer> list, boolean filterStockOffers, boolean filterValidStockOffers){
        CurrencyOffer bestOffer = null;
        for (CurrencyOffer offer : list) {
            if (!filterStockOffers || (!filterValidStockOffers && offer.getStock() >= 0) ||
                    filterValidStockOffers && offer.getStock() > offer.getSellValue()) {
                bestOffer = offer;
                break;
            }
        }
        return bestOffer;
    }

    public HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getPlayerOffers() {
        return playerOffers;
    }

    public void setParseListener(ParseListener parseListener) {
        this.parseListener = parseListener;
    }
}
