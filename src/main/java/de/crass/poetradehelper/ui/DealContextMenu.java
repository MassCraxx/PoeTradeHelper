package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.Main;
import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.parser.PoeTradeWebParser;
import de.crass.poetradehelper.parser.TradeManager;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Created by mcrass on 13.08.2018.
 */
class DealContextMenu extends ContextMenu {

    DealContextMenu(CurrencyDeal deal) {
        String league = deal.getLeague();

        MenuItem buyItem = new MenuItem("Open Buy Offers");
        buyItem.setOnAction(event -> PoeTradeWebParser.openInBrowser(league, deal.getPrimaryCurrencyID(), deal.getSecondaryCurrencyID()));

        MenuItem sellItem = new MenuItem("Open Sell Offers");
        sellItem.setOnAction(event -> PoeTradeWebParser.openInBrowser(league, deal.getSecondaryCurrencyID(), deal.getPrimaryCurrencyID()));

        MenuItem buyValueItem = new MenuItem("Market Buy Value: " + Main.prettyFloat(deal.getcValue() * deal.getBuyAmount()) + "c");
        MenuItem sellValueItem = new MenuItem("Market Sell Value: " + Main.prettyFloat(deal.getcValue() * deal.getSellAmount()) + "c");

        MenuItem updateItem = new MenuItem("Update Currency");
        updateItem.setOnAction(event -> TradeManager.getInstance().updateOffersForCurrency(deal.getSecondaryCurrencyID(), true));

        MenuItem updatePlayerItem = new MenuItem("Update Player Offers");
        updatePlayerItem.setOnAction(event -> TradeManager.getInstance().updatePlayerOffers());

        getItems().addAll(
                buyValueItem,
                sellValueItem,
                new SeparatorMenuItem(),
                buyItem,
                sellItem,
                new SeparatorMenuItem(),
                updateItem,
                updatePlayerItem);
    }
}
