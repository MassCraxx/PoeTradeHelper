package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.PoeTradeWebParser;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class OfferContextMenu extends ContextMenu {

    private TableView<CurrencyOffer> tableView;

    public OfferContextMenu(TableView<CurrencyOffer> tableView, ComboBox<CurrencyID> secondary) {
        this.tableView = tableView;

        MenuItem browserItem = new MenuItem("Open in Browser");

        browserItem.setOnAction(event -> {
            if (isSellOffer(secondary.getValue())) {
                PoeTradeWebParser.openInBrowser(PropertyManager.getInstance().getCurrentLeague(), secondary.getValue(), PropertyManager.getInstance().getPrimaryCurrency());
            } else {
                PoeTradeWebParser.openInBrowser(PropertyManager.getInstance().getCurrentLeague(), PropertyManager.getInstance().getPrimaryCurrency(), secondary.getValue());
            }
        });

        MenuItem copyItem = new MenuItem("Whisper to Clipboard");
        copyItem.setOnAction(event -> {
            String playerName = getSelected().getPlayerName();

            //Switcheroo, because chat comes from the opposite perspective
            int buyAmount = (int) getSelected().getSellAmount();
            int sellAmount = (int) getSelected().getBuyAmount();
            CurrencyID buyCurrency = getSelected().getSellID();
            CurrencyID sellCurrency = getSelected().getBuyID();
            String myString = "@" + playerName + " Hi, I'd like to buy your " +
                    buyAmount + " " +
                    buyCurrency.name().toLowerCase() + " for my " +
                    sellAmount + " " +
                    sellCurrency.name().toLowerCase() + " in " +
                    PropertyManager.getInstance().getCurrentLeague() + ".";
            StringSelection stringSelection = new StringSelection(myString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });

        getItems().addAll(browserItem, copyItem);
    }

    private CurrencyOffer getSelected() {
        int i = tableView.getSelectionModel().getFocusedIndex();
        return tableView.getItems().get(i);
    }

    private boolean isSellOffer(CurrencyID secondary) {
        CurrencyID thisSellCurrency = getSelected().getSellID();
        return secondary.equals(thisSellCurrency);
    }
}