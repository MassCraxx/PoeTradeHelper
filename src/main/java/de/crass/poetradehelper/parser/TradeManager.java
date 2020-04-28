package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static de.crass.poetradehelper.Main.poeConfigReader;

/**
 * Created by mcrass on 19.07.2018.
 */
public class TradeManager implements PoeTradeWebParser.OfferParseListener {
    private static TradeManager instance;

    private PoeApiParser poeApiParser;
    private WebParser webParser;
    private ObservableList<CurrencyDeal> currentDeals;
    private ObservableList<CurrencyDeal> playerDeals;
    private PoeNinjaParser poeNinjaParser;

    private static ScheduledExecutorService autoUpdateExecutor;

    private Comparator<? super CurrencyDeal> diffValueSorter = (Comparator<CurrencyDeal>) (o1, o2) -> {
        float o1Value = o1.getDiffValue();
        float o2Value = o2.getDiffValue();

        if (o1Value == o2Value) {
            return 0;
        }
        return o1Value > o2Value ? -1 : 1;
    };
    private Comparator<? super CurrencyDeal> playerDiffValueSorter = (Comparator<CurrencyDeal>) (o1, o2) -> {
        float o1Value = o1.getPlayerDiffValue();
        float o2Value = o2.getPlayerDiffValue();

        if (o1Value == o2Value) {
            return 0;
        }
        return o1Value > o2Value ? -1 : 1;
    };
    private DealParseListener listener;
    private boolean updating = false;
    private boolean autoUpdate = false;

    private TradeManager() {
        poeApiParser = new PoeApiParser();
        poeNinjaParser = new PoeNinjaParser();

        currentDeals = FXCollections.observableArrayList();
        playerDeals = FXCollections.observableArrayList();
    }

    public static TradeManager getInstance() {
        if (instance == null) {
            instance = new TradeManager();
        }
        return instance;
    }

    public void updateOffers(boolean clear) {
        if(!updating) {
            pauseUpdateTask();
            updateOffers(clear, true, PropertyManager.getInstance().getFilterList());
        } else {
//            LogManager.getInstance().log(getClass(), "Prevented update attempt while updating!");
            System.out.println("Prevented update attempt while updating!");
        }
    }

    private void updateOffers(boolean clear, boolean async, Collection<CurrencyID> currencyIDs) {
        if(currencyIDs == null || currencyIDs.isEmpty()){
            LogManager.getInstance().log(getClass(), "No Currency selected. Check your parsing settings!");
            return;
        }

        if (!updating) {
            updating = true;

            if (listener != null) {
                Platform.runLater(() -> listener.onUpdateStarted());
            }

            List<CurrencyID> currencyIDList;
            if (PropertyManager.getInstance().getFilterMultipleTransactionDeals()) {
                currencyIDList = new LinkedList<>();
                for (CurrencyID currencyID : currencyIDs) {
                    //only works for 20stack items
                    if (!poeNinjaParser.requiresMultipleTransactions(currencyID)) {
                        currencyIDList.add(currencyID);
                    } else {
                        LogManager.getInstance().log(getClass(), "Skipping update of " + currencyID + " since it would require multiple trade transactions.");
                    }
                }
            } else {
                currencyIDList = PropertyManager.getInstance().getFilterList();
            }

            webParser.updateCurrencies(currencyIDList, clear, async);
        } else {
//            LogManager.getInstance().log(getClass(), "Prevented update attempt while updating!");
            System.out.println("Prevented update attempt while updating!");
        }
    }

    public void updatePlayerOffers() {
        if(playerDeals.isEmpty()){
            LogManager.getInstance().log(getClass(), "No player offers found.");
            return;
        }
        updateOffers(false, true, getPlayerCurrencies());
    }

    public List<CurrencyID> getPlayerCurrencies(){
        List<CurrencyID> list = new LinkedList<>();
        for (CurrencyDeal deal : playerDeals) {
            list.add(deal.getSecondaryCurrencyID());
        }
        return list;
    }

    public void updateOffersForCurrency(CurrencyID secondaryCurrencyID, boolean async) {
        if(!updating) {
            updating = true;
            pauseUpdateTask();

            if (listener != null) {
                Platform.runLater(() -> listener.onUpdateStarted());
            }

            webParser.updateCurrency(secondaryCurrencyID, async);
        } else {
//            LogManager.getInstance().log(getClass(), "Prevented update attempt while updating!");
            System.out.println("Prevented update attempt while updating!");
        }
    }

    public void parseDeals() {
        Platform.runLater(() -> {
            CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
            currentDeals.clear();
            playerDeals.clear();

            if (listener != null) {
                listener.onParsingStarted();
            }

            HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> marketOffers = webParser.getCurrentOffers();
            HashSet<Pair<CurrencyID, CurrencyID>> processedKeys = new HashSet<>();

            List<CurrencyDeal> newDeals = new LinkedList<>();
            if (marketOffers == null || marketOffers.isEmpty()) {
                LogManager.getInstance().log(TradeManager.class, "No offers to parse.");
            } else {
//                LogManager.getInstance().log(TradeManager.class, "Parsing offers...");

                // Iterate through all combinations, check inverted offer, but every pair only once
                for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : marketOffers.entrySet()) {
                    Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
                    Pair<CurrencyID, CurrencyID> invertedKey = new Pair<>(key.getValue(), key.getKey());

                    // If other way already processed
                    if (processedKeys.contains(invertedKey)) {
                        continue;
                    }

                    processedKeys.add(key);

                    int totalOffers = (marketOffers.get(key) == null ? 0 : marketOffers.get(key).size())
                            + (marketOffers.get(invertedKey) == null ? 0 : marketOffers.get(invertedKey).size());

                    if (totalOffers == 0) {
                        continue;
                    }

                    CurrencyOffer bestMarketSellOffer;
                    CurrencyOffer bestMarketBuyOffer;

                    if (!key.getKey().equals(primaryCurrency)) {
                        // Check if offer actually contains primary
                        if (!key.getValue().equals(primaryCurrency)) {
                            continue;
                        }

                        //Buy offer -> Sell offer
                        key = invertedKey;
                        invertedKey = offerMap.getKey();
                    }

                    CurrencyID secondaryCurrency = key.getValue();
                    float cValue = poeNinjaParser.getCurrentCValueFor(secondaryCurrency);

                    if(poeNinjaParser.requiresMultipleTransactions(secondaryCurrency)){
                        LogManager.getInstance().log(getClass(), "Skip parse " + secondaryCurrency + " since it would require multiple trade transactions.");
                        continue;
                    }

                    long timestamp = -1;

                    bestMarketSellOffer = getBestOffer(marketOffers.get(key));
                    bestMarketBuyOffer = getBestOffer(marketOffers.get(invertedKey));

                    float marketSellPrice = 0;
                    if (bestMarketSellOffer != null) {
                        marketSellPrice = bestMarketSellOffer.getSellAmount();
                        marketSellPrice /= bestMarketSellOffer.getBuyAmount();
                        timestamp = bestMarketSellOffer.getTimestamp();
                    }

                    float marketBuyPrice = 0;
                    if (bestMarketBuyOffer != null) {
                        marketBuyPrice = bestMarketBuyOffer.getBuyAmount();
                        marketBuyPrice /= bestMarketBuyOffer.getSellAmount();
                        timestamp = bestMarketBuyOffer.getTimestamp();
                    }

                    CurrencyDeal deal = new CurrencyDeal(
                            primaryCurrency,
                            secondaryCurrency,
                            cValue,
                            totalOffers,
                            marketBuyPrice,
                            marketSellPrice,
                            timestamp);

                    if(bestMarketBuyOffer != null && bestMarketBuyOffer.getQueryID() != null && !bestMarketBuyOffer.getQueryID().isEmpty()){
                        deal.setBuyQueryID(bestMarketBuyOffer.getQueryID());
                    }

                    if(bestMarketSellOffer != null && bestMarketSellOffer.getQueryID() != null && !bestMarketSellOffer.getQueryID().isEmpty()){
                        deal.setSellQueryID(bestMarketSellOffer.getQueryID());
                    }

                    newDeals.add(deal);
                }

                newDeals.sort(diffValueSorter);
                currentDeals.setAll(newDeals);

                // Parse Players
                HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers = webParser.getPlayerOffers();
                HashSet<Pair<CurrencyID, CurrencyID>> processedPlayerKeys = new HashSet<>();
                List<CurrencyDeal> newPlayerDeals = new LinkedList<>();
                boolean notified = false;
                for (Map.Entry<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> offerMap : playerOffers.entrySet()) {
                    Pair<CurrencyID, CurrencyID> key = offerMap.getKey();
                    Pair<CurrencyID, CurrencyID> invertedKey = new Pair<>(key.getValue(), key.getKey());

                    // If other way already processed
                    if (processedPlayerKeys.contains(invertedKey)) {
                        continue;
                    }

                    processedPlayerKeys.add(key);

                    CurrencyOffer bestMarketSellOffer;
                    CurrencyOffer bestMarketBuyOffer;
                    CurrencyOffer playerSellOffer = null;
                    CurrencyOffer playerBuyOffer = null;

                    if (!key.getKey().equals(primaryCurrency)) {
                        // Check if offer actually contains primary
                        if (!key.getValue().equals(primaryCurrency)) {
                            continue;
                        }
                        //Buy offer -> Sell offer
                        key = invertedKey;
                        invertedKey = offerMap.getKey();
                    }

                    CurrencyID secondaryCurrency = key.getValue();

                    if (playerOffers.get(key) != null) {
                        if (!playerOffers.get(key).isEmpty()) {
                            playerSellOffer = playerOffers.get(key).get(0);
                        }
//                        else {
//                          LogManager.getInstance().log(TradeManager.class, "PlayerOffer for "+key+" was empty...");
//                        }
                    }

                    List<CurrencyOffer> playerBuyOfferList = playerOffers.get(invertedKey);
                    if (playerBuyOfferList != null) {
                        if (!playerBuyOfferList.isEmpty()) {
                            playerBuyOffer = playerBuyOfferList.get(0);
                        }
//                        else {
//                          LogManager.getInstance().log(TradeManager.class, "PlayerOffer for "+key+" was empty...");
//                        }
                    }

                    bestMarketSellOffer = getBestOffer(marketOffers.get(key));

                    bestMarketBuyOffer = getBestOffer(marketOffers.get(invertedKey));

                    float playerSellPrice = 0;
                    float playerBuyPrice = 0;
                    int playerSellStock = 0;
                    int playerBuyStock = 0;
                    float marketSellPrice = 0;
                    float marketBuyPrice = 0;
                    long timestamp = -1;

                    if (bestMarketSellOffer != null) {
                        marketSellPrice = bestMarketSellOffer.getSellAmount();
                        marketSellPrice /= bestMarketSellOffer.getBuyAmount();
                    }

                    if (bestMarketBuyOffer != null) {
                        marketBuyPrice = bestMarketBuyOffer.getBuyAmount();
                        marketBuyPrice /= bestMarketBuyOffer.getSellAmount();
                    }

                    if (playerSellOffer != null) {
                        playerSellPrice = playerSellOffer.getSellAmount();
                        playerSellPrice /= playerSellOffer.getBuyAmount();

                        playerSellStock = playerSellOffer.getStock();
                        timestamp = playerSellOffer.getTimestamp();
                    }

                    if (playerBuyOffer != null) {
                        playerBuyPrice = playerBuyOffer.getBuyAmount();
                        playerBuyPrice /= playerBuyOffer.getSellAmount();

                        playerBuyStock = playerBuyOffer.getStock();
                        timestamp = playerBuyOffer.getTimestamp();
                    }

                    //FIXME
                    if (!notified && poeConfigReader.isActive() && Boolean.parseBoolean(PropertyManager.getInstance().getProp("voice_notify_bad_tendency", "false"))) {
                        if (marketBuyPrice > 0 && playerBuyPrice > 0 && playerBuyPrice > marketBuyPrice || marketSellPrice > 0 && playerSellPrice > 0 && playerSellPrice < marketSellPrice) {
                            try {
                                poeConfigReader.notifyBadTendency();
                                notified = true;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    float cValue = poeNinjaParser.getCurrentCValueFor(secondaryCurrency);
                    int totalOffers = -1;
                    CurrencyDeal deal = new CurrencyDeal(primaryCurrency, secondaryCurrency, cValue, totalOffers, marketBuyPrice,
                            marketSellPrice, playerBuyPrice, playerSellPrice, playerBuyStock, playerSellStock, timestamp);

                    if(playerBuyOffer != null && playerBuyOffer.getQueryID() != null && !playerBuyOffer.getQueryID().isEmpty()){
                        deal.setBuyQueryID(playerBuyOffer.getQueryID());
                    }

                    if(playerSellOffer != null && playerSellOffer.getQueryID() != null && !playerSellOffer.getQueryID().isEmpty()){
                        deal.setSellQueryID(playerSellOffer.getQueryID());
                    }

                    if (playerBuyPrice > 0 || playerSellPrice > 0) {
                        newPlayerDeals.add(deal);
                    }
//                    else {
//                        // Is logged before now. Needs further investigation...
////                            LogManager.getInstance().log(TradeManager.class, "Player offer did not contain information...!");
//                    }
                }

                newPlayerDeals.sort(diffValueSorter);
                playerDeals.setAll(newPlayerDeals);
            }

            if (listener != null) {
                listener.onParsingFinished();
            }
        });
    }

    private CurrencyOffer getBestOffer(List<CurrencyOffer> list) {
        return getBestOffer(list,
                PropertyManager.getInstance().getFilterNoApi(),
                PropertyManager.getInstance().getFilterOutOfStock(),
                PropertyManager.getInstance().getFilterExcessive());
    }

    private CurrencyOffer getBestOffer(List<CurrencyOffer> list,
                                       boolean filterStockOffers,
                                       boolean filterValidStockOffers,
                                       boolean filterExcessiveOffers) {
        CurrencyOffer bestOffer = null;
        if (list == null) {
            return null;
        }
        for (CurrencyOffer offer : list) {
            // Skip player offers

            if(offer.isPlayerOffer()){
                continue;
            }

            // player is on ignorelist
            if (offer.isIgnored()) {
                continue;
            }

            // Return most top offer that meets filter requirements

            if (filterStockOffers && offer.getStock() < 0) {
                continue;
            }

            if (filterValidStockOffers && (offer.getStock() >= 0 && offer.getStock() < offer.getSellAmount())) {
                continue;
            }

            if (filterExcessiveOffers) {
                float buyValue = poeNinjaParser.getCurrentCValueFor(offer.getBuyID()) * offer.getBuyAmount();
                float sellValue = poeNinjaParser.getCurrentCValueFor(offer.getSellID()) * offer.getSellAmount();
                float higherValue = Math.max(buyValue, sellValue);

                // If sell and buy value differ too much from each other, filter
                if (Math.abs(buyValue - sellValue) > (PropertyManager.getInstance().getExcessiveTreshold() / 100f) * higherValue) {
                    boolean isBuyOffer = PropertyManager.getInstance().getPrimaryCurrency().equals(offer.getSellID());
                    LogManager.getInstance().log(getClass(), "Filtered " + (isBuyOffer ? "buy" : "sell") + " offer for " + offer.getBuyID() + " - " + offer.getSellID() + ": " + buyValue + "c vs " + sellValue + "c");
                    continue;
                }
            }

            bestOffer = offer;
            break;
        }
        return bestOffer;
    }

    public ObservableList<CurrencyDeal> getCurrentDeals() {
        return currentDeals;
    }

    public ObservableList<CurrencyDeal> getPlayerDeals() {
        return playerDeals;
    }

    @Override
    public void onUpdateStarted() {
        updating = true;
    }

    @Override
    public void onUpdateFinished() {
        poeNinjaParser.fetchRates(PropertyManager.getInstance().getCurrentLeague(), false);
        parseDeals();

        if(autoUpdate){
            startUpdateTask();
        }

        if(listener != null){
            listener.onUpdateFinished();
        }

        updating = false;
    }

    @Override
    public void onUpdateError() {
        if(listener != null){
            listener.onError();
        }

        updating = false;
    }

    private ScheduledFuture<?> autoUpdateFuture;
    private void startUpdateTask() {
        if (autoUpdateExecutor == null) {
            autoUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        int updateDelay = PropertyManager.getInstance().getUpdateDelay() * 60;
        autoUpdateFuture = autoUpdateExecutor.schedule(() -> {
            LogManager.getInstance().log("AutoUpdate", "Invoke Automatic Update");
            Set<CurrencyID> toUpdate = new HashSet<>(PropertyManager.getInstance().getFilterList());
            if (PropertyManager.getInstance().getBooleanProp("auto_update_player_offers", false)) {
                toUpdate.addAll(getPlayerCurrencies());
            }
            TradeManager.getInstance().updateOffers(false, false, toUpdate);

        }, updateDelay, TimeUnit.SECONDS);
        LogManager.getInstance().log(getClass(), "Auto Update scheduled.");
        autoUpdate = true;
    }

    private void pauseUpdateTask(){
        if (autoUpdateFuture != null) {
            autoUpdateFuture.cancel(true);
            LogManager.getInstance().log(getClass(), "Auto Update cancelled.");
        }
    }

    private void stopUpdateTask() {
        pauseUpdateTask();
        if (autoUpdateExecutor != null) {
            LogManager.getInstance().log(getClass(), "Auto Update shutdown.");
            autoUpdateExecutor.shutdown();
            try {
                autoUpdateExecutor.awaitTermination(5,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LogManager.getInstance().log(getClass(), "Auto Update shutdownNow.");
                autoUpdateExecutor.shutdownNow();
            }
            autoUpdateExecutor = null;
        }
        autoUpdate = false;
    }

    public boolean isUpdating() {
        return updating;
    }

    public void registerListener(DealParseListener listener, PoeNinjaParser.PoeNinjaListener ninjaListener) {
        this.listener = listener;
        poeNinjaParser.registerListener(ninjaListener);
    }

    public void cancelUpdate() {
        webParser.cancel();
    }

    public ObservableList<String> getLeagueList() {
        return poeApiParser.getCurrentLeagues();
    }

    public void updateCurrencyValues() {
        poeNinjaParser.fetchRates(PropertyManager.getInstance().getCurrentLeague(), true);
    }

    public Float getCurrencyValue(CurrencyID what, CurrencyID inWhat) {
        return poeNinjaParser.getCurrentValue(what, inWhat);
    }

    public ObservableList<Map.Entry<CurrencyID, Float>> getCurrencyValues() {
        return FXCollections.observableArrayList(poeNinjaParser.getCurrentRates().entrySet());
    }

    public ObservableList<CurrencyOffer> getBuyOffers(CurrencyID secondary) {
        ObservableList<CurrencyOffer> result = webParser.getOffersFor(secondary, false);
        if (result == null || result.isEmpty()) {
            updateOffersForCurrency(secondary, false);
            result = webParser.getOffersFor(secondary, false);
        }
        return result;
//        return (ObservableList<CurrencyOffer>) webParser.getCurrentOffers().get(new Pair<>(secondary, PropertyManager.getInstance().getPrimaryCurrency()));
    }

    public ObservableList<CurrencyOffer> getSellOffers(CurrencyID secondary) {
        ObservableList<CurrencyOffer> result = webParser.getOffersFor(secondary, true);
        if (result == null || result.isEmpty()) {
            //FIXME: Should be async
            updateOffersForCurrency(secondary, false);
            result = webParser.getOffersFor(secondary, true);
        }
        return result;
//        return (ObservableList<CurrencyOffer>) webParser.getCurrentOffers().get(new Pair<>(PropertyManager.getInstance().getPrimaryCurrency(), secondary));
    }

    public void setAutoUpdate(boolean enabled) {
        if (autoUpdate == enabled) {
            return;
        } else if (isUpdating()) {
            autoUpdate = enabled;
            return;
        }
        LogManager.getInstance().log(getClass(), "Automatic Updates " + (enabled ? "en" : "dis") + "abled.");

        if (enabled) {
            startUpdateTask();
        } else {
            stopUpdateTask();
        }
    }

    public boolean isAutoUpdating() {
        return autoUpdate;
    }

    public void release() {
        stopUpdateTask();
    }

    public void reset() {
        poeNinjaParser.reset();
        webParser.reset();
        playerDeals.clear();
        currentDeals.clear();
    }

    public void removeDeal(CurrencyDeal deal) {
        currentDeals.remove(deal);
        playerDeals.remove(deal);
    }

    public void setWebParser(String value) {
        switch (value){
            case PoeTradeWebParser.IDENTIFIER:
                webParser = new PoeTradeWebParser(this);
                break;
            case PoeTradeApiParser.IDENTIFIER:
                webParser = new PoeTradeApiParser(this);
                break;
        }
        PropertyManager.getInstance().setProp("trade_data_source", value);
    }

    public String getCurrencyValuePercentage(float amount, CurrencyID primary, CurrencyID secondary) {
        float primaryValue = TradeManager.getInstance().getCurrencyValue(primary, CurrencyID.CHAOS);
        float secondaryValue = TradeManager.getInstance().getCurrencyValue(secondary, CurrencyID.CHAOS);
        int percentage = Math.round(amount * secondaryValue * 100 / primaryValue) - 100;
        return (percentage >= 0 ? "+" : "") + percentage + "%";
    }

    public void removeOffers(CurrencyDeal deal) {
        webParser.removeOffers(PropertyManager.getInstance().getPrimaryCurrency(), deal.getSecondaryCurrencyID());
    }

    public interface DealParseListener {
        void onUpdateStarted();

        void onUpdateFinished();

        void onParsingStarted();

        void onParsingFinished();

        void onError();
    }
}
