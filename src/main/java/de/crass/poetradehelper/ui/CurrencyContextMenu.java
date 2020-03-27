package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.model.CurrencyID;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableView;

import java.util.Map;

public class CurrencyContextMenu extends ContextMenu {

    private TableView<Map.Entry<CurrencyID, Float>> tableView;

    public CurrencyContextMenu(TableView<Map.Entry<CurrencyID, Float>> tableView){
        this.tableView = tableView;

        // FIXME
//        MenuItem buyItem = new MenuItem("Open Buy Offers");
//        buyItem.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                Main.openInBrowser(PropertyManager.getInstance().getCurrentLeague(), PropertyManager.getInstance().getPrimaryCurrency(), getSelected());
//            }
//        });
//
//        MenuItem sellItem = new MenuItem("Open Sell Offers");
//        sellItem.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                Main.openInBrowser(PropertyManager.getInstance().getCurrentLeague(), getSelected(), PropertyManager.getInstance().getPrimaryCurrency());
//            }
//        });

//        getItems().addAll(buyItem, sellItem);
    }

    private CurrencyID getSelected() {
        int i = tableView.getSelectionModel().getFocusedIndex();
        Map.Entry<CurrencyID, Float> entry = tableView.getItems().get(i);
        if(entry != null) {
            return entry.getKey();
        }
        return null;
    }
}
