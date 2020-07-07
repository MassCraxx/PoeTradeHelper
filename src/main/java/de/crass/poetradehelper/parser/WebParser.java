package de.crass.poetradehelper.parser;

import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class WebParser {
    HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> currentOffers = new HashMap<>();
    HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> playerOffers = new HashMap<>();

    boolean updating = false;
    boolean cancel = false;
    public OfferParseListener parseListener;
    private boolean cancelAutoUpdateOnCancel = PropertyManager.getInstance().getBooleanProp("auto_update_cancel_on_error", true);


    WebParser(OfferParseListener listener) {
        this();
        setParseListener(listener);
    }

    private WebParser() {
        reset();
    }

    public void updateCurrencies(List<CurrencyID> currencyList, boolean clear, boolean async) {
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



    void doUpdate(List<CurrencyID> currencyList, boolean clear) {
        try {
            updating = true;
            CurrencyID primaryCurrency = PropertyManager.getInstance().getPrimaryCurrency();
            for (CurrencyID secondaryCurrency : currencyList) {
                if (cancel) {
                    break;
                }
                if (!clear)
                    removeOffers(primaryCurrency, secondaryCurrency);

                // BUY
                fetchOffers(secondaryCurrency, primaryCurrency, PropertyManager.getInstance().getCurrentLeague());

                if (cancel) {
                    break;
                }

                // SELL
                fetchOffers(primaryCurrency, secondaryCurrency, PropertyManager.getInstance().getCurrentLeague());
            }

            if (parseListener != null) {
                Platform.runLater(() -> parseListener.onUpdateFinished());
            }
        } catch (IOException e) {
            LogManager.getInstance().log(getClass(), "Fetching offers failed. No internet connection?");
            e.printStackTrace();

            if (parseListener != null) {
                Platform.runLater(() -> parseListener.onUpdateError());
            }
        } catch (InterruptedException i) {
            i.printStackTrace();
            if (parseListener != null) {
                Platform.runLater(() -> parseListener.onUpdateFinished());
            }
        }
        cancel = false;
        updating = false;
    }

    protected abstract void fetchOffers(CurrencyID primaryCurrency, CurrencyID secondaryCurrency, String currentLeague) throws IOException, InterruptedException;

    public void cancel() {
        if (cancelAutoUpdateOnCancel && TradeManager.getInstance().isAutoUpdating()) {
            TradeManager.getInstance().setAutoUpdate(false);
        }
        cancel = true;
    }

    public void updateCurrency(CurrencyID secondaryCurrencyID, boolean async){
        List<CurrencyID> list = new LinkedList<>();
        list.add(secondaryCurrencyID);
        updateCurrencies(list, false, async);
    }

    public HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getCurrentOffers() {
        return currentOffers;
    }

    public HashMap<Pair<CurrencyID, CurrencyID>, List<CurrencyOffer>> getPlayerOffers() {
        return playerOffers;
    }

    public void updateCurrencies(List<CurrencyID> currencyList, boolean clear) {
        updateCurrencies(currencyList, clear, true);
    }

    public ObservableList<CurrencyOffer> getOffersFor(CurrencyID secondary, boolean sell) {
        return getOffersFor(new Pair<>(PropertyManager.getInstance().getPrimaryCurrency(), secondary), !sell);
    }

    public ObservableList<CurrencyOffer> getOffersFor(Pair<CurrencyID, CurrencyID> key, boolean invert) {
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

    public void addOffer(CurrencyOffer offer) {
        Pair<CurrencyID, CurrencyID> key = new Pair<>(offer.getBuyID(), offer.getSellID());
        if (PropertyManager.getInstance().getPlayerCharacter().equalsIgnoreCase(offer.getPlayerName()) ||
                PropertyManager.getInstance().getPlayerAccount().equalsIgnoreCase(offer.getAccountName())) {
            offer.setPlayerOffer(true);
        }

        if (offer.isPlayerOffer()) {
            //LogManager.getInstance().log(getClass(), "Found player offer " + offer.getPlayerName());
            List<CurrencyOffer> offers = playerOffers.get(key);
            if (offers == null) {
                offers = new LinkedList<>();
            }
//            LogManager.getInstance().log(getClass(), "AddOffer - Key: " + key + " Offer: " + offer.toString());
            offers.add(offer);
            playerOffers.put(key, offers);
        }

        ObservableList<CurrencyOffer> offers = getOffersFor(key, false);
        offers.add(offer);
        currentOffers.put(key, offers);
    }

    public void removeOffers(CurrencyID primary, CurrencyID secondary) {
        Pair<CurrencyID, CurrencyID> key = new Pair<>(primary, secondary);
        clearListIfPossible(currentOffers.get(key));
        clearListIfPossible(playerOffers.get(key));

        key = new Pair<>(secondary, primary);
        clearListIfPossible(currentOffers.get(key));
        clearListIfPossible(playerOffers.get(key));
    }

    private void clearListIfPossible(List<?> list) {
        if (list != null && !list.isEmpty()) {
            list.clear();
        }
    }

    public void reset() {
        // The individual offer tables may be bound to the offer list views
        for (List<CurrencyOffer> list : currentOffers.values()) {
            list.clear();
        }

        playerOffers.clear();
    }

    public void setParseListener(OfferParseListener parseListener) {
        this.parseListener = parseListener;
    }

    public interface OfferParseListener {
        void onUpdateStarted();
        void onUpdateFinished();
        void onUpdateError();
    }
}
