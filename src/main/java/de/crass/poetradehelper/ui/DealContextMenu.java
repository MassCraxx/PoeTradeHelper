package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.Main;
import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.parser.TradeManager;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Created by mcrass on 13.08.2018.
 */
class DealContextMenu extends ContextMenu {

    DealContextMenu(CurrencyDeal deal) {
        MenuItem nameItem = new MenuItem(deal.getSecondaryCurrencyID().getDisplayName());

        //TODO
        MenuItem offersItem = new MenuItem("Open in Offers Tab");

        MenuItem buyItem = new MenuItem("Open Buy in Browser");
        if (deal.getBuyQueryID() == null) {
            buyItem.setOnAction(event -> Main.openInBrowser(deal.getPrimaryCurrencyID(), deal.getSecondaryCurrencyID()));
        } else {
            buyItem.setOnAction(event -> Main.openInBrowser(deal.getBuyQueryID()));
        }

        MenuItem sellItem = new MenuItem("Open Sell in Browser");
        if (deal.getSellQueryID() == null) {
            buyItem.setOnAction(event -> Main.openInBrowser(deal.getSecondaryCurrencyID(), deal.getPrimaryCurrencyID()));
        } else {
            buyItem.setOnAction(event -> Main.openInBrowser(deal.getSellQueryID()));
        }

        String buyPercentage = String.valueOf(TradeManager.getInstance().getCurrencyValuePercentage(deal.getBuyAmount(), deal.getPrimaryCurrencyID(), deal.getSecondaryCurrencyID()));
        String sellPercentage = String.valueOf(TradeManager.getInstance().getCurrencyValuePercentage(deal.getSellAmount(), deal.getPrimaryCurrencyID(), deal.getSecondaryCurrencyID()));

        MenuItem buyValueItem = new MenuItem("Market Buy Value: " + Main.prettyFloat(deal.getcValue() * deal.getBuyAmount()) + "c" + " (" + buyPercentage + ")");
        MenuItem sellValueItem = new MenuItem("Market Sell Value: " + Main.prettyFloat(deal.getcValue() * deal.getSellAmount()) + "c" + " (" + sellPercentage + ")");

        MenuItem updateItem = new MenuItem("Update Currency");
        updateItem.setOnAction(event -> TradeManager.getInstance().updateOffersForCurrency(deal.getSecondaryCurrencyID(), true));

        MenuItem updatePlayerItem = new MenuItem("Update Player Offers");
        updatePlayerItem.setOnAction(event -> TradeManager.getInstance().updatePlayerOffers());

        MenuItem removeItem = new MenuItem("Remove from Overview");
        removeItem.setOnAction(event -> TradeManager.getInstance().removeDeal(deal));

        getItems().addAll(
                nameItem,
                new SeparatorMenuItem(),
                buyValueItem,
                sellValueItem,
                new SeparatorMenuItem(),
                buyItem,
                sellItem,
                new SeparatorMenuItem(),
                updateItem,
                updatePlayerItem,
                new SeparatorMenuItem(),
                removeItem);
    }
}
