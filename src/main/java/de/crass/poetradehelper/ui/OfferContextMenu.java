package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.Main;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.TradeManager;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;

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
                Main.openInBrowser(getSelected());
            } else {
                Main.openInBrowser(getSelected());
            }
        });

        MenuItem copyItem = new MenuItem("Whisper to Clipboard");
        copyItem.setOnAction(event -> {
            //Switcheroo, because chat comes from the opposite perspective
            int buyAmount = (int) getSelected().getSellAmount();
            int sellAmount = (int) getSelected().getBuyAmount();

            String whisper = getSelected().getWhisper();
            if (whisper == null || whisper.isEmpty()) {
                String playerName = getSelected().getPlayerName();

                CurrencyID buyCurrency = getSelected().getSellID();
                CurrencyID sellCurrency = getSelected().getBuyID();
                whisper = "@" + playerName + " Hi, I'd like to buy your " +
                        buyAmount + " " +
                        buyCurrency.getDisplayName() + " for my " +
                        sellAmount + " " +
                        sellCurrency.getDisplayName() + " in " +
                        PropertyManager.getInstance().getCurrentLeague() + ".";
            } else {
                whisper = whisper.replace("{0}", String.valueOf(buyAmount));
                whisper = whisper.replace("{1}", String.valueOf(sellAmount));
            }
            StringSelection stringSelection = new StringSelection(whisper);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        MenuItem ignoreItem;

        ignoreItem = new MenuItem("Toggle ignore player");
        ignoreItem.setOnAction(event -> {
            if (getSelected().isIgnored()) {
                PropertyManager.getInstance().removeIgnoredPlayer(getSelected().getAccountName());
            } else {
                PropertyManager.getInstance().addIgnoredPlayer(getSelected().getAccountName());
            }
            TradeManager.getInstance().parseDeals();
            tableView.refresh();
        });

        getItems().addAll(copyItem, browserItem, new SeparatorMenuItem(), ignoreItem);
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
