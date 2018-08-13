package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.PropertyManager;
import de.crass.poetradeparser.model.CurrencyDeal;
import de.crass.poetradeparser.parser.PoeTradeWebParser;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * Created by mcrass on 13.08.2018.
 */
public class DealContextMenu extends ContextMenu {

    DealContextMenu(CurrencyDeal deal) {
        String league = PropertyManager.getInstance().getCurrentLeague();

        MenuItem buyItem = new MenuItem("Open Buy Offers");
        buyItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PoeTradeWebParser.openInBrowser(league, deal.getPrimaryCurrencyID(), deal.getSecondaryCurrencyID());

            }
        });

        MenuItem sellItem = new MenuItem("Open Sell Offers");
        sellItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PoeTradeWebParser.openInBrowser(league, deal.getSecondaryCurrencyID(), deal.getPrimaryCurrencyID());
            }
        });

        MenuItem buyValueItem = new MenuItem("Market Buy Value: " + deal.getcValue() * deal.getBuyAmount());
        MenuItem sellValueItem = new MenuItem("Market Sell Value: " + deal.getcValue() * deal.getSellAmount());

        getItems().addAll(buyItem, sellItem, buyValueItem, sellValueItem);
    }
}
