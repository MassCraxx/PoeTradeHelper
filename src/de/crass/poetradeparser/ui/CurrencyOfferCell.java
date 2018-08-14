package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.PropertyManager;
import de.crass.poetradeparser.model.CurrencyDeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.IOException;

import static de.crass.poetradeparser.PropertyManager.prettyFloat;

/**
 * Created by mcrass on 19.07.2018.
 */
public class CurrencyOfferCell<T> extends javafx.scene.control.ListCell<CurrencyDeal> {

    private FXMLLoader mLLoader;

    @FXML
    private Text sellOffer;

    @FXML
    private Text chaosValue;

    @FXML
    private AnchorPane root;

    @FXML
    private HBox hBox;

    @FXML
    private Text diffValue;

    @FXML
    private Text buyOffer;

    @FXML
    private Text offersText;

    @FXML
    private Text diff;

    @FXML
    private ImageView currencyIcon;

    @Override
    protected void updateItem(CurrencyDeal deal, boolean empty) {
        super.updateItem(deal, empty);

        if (empty || deal == null) {

            setText(null);
            setGraphic(null);

        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("currency_cell.fxml"));
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

//            root.setBorder(new Border(new BorderStroke(Color.BLACK,
//                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));

            String url = deal.getSecondaryCurrencyID().getID() + ".png";
            PropertyManager.setImage(url,currencyIcon);
            currencyIcon.setCache(true);
            currencyIcon.setCacheHint(CacheHint.SPEED);

            float buy = deal.getBuyAmount();
            float sell = deal.getSellAmount();

            chaosValue.setText(prettyFloat(deal.getcValue()) + "c");
            offersText.setText(prettyFloat(deal.getOffers()));
            buyOffer.setText(prettyFloat(buy));
            sellOffer.setText(prettyFloat(sell));
            diff.setText(prettyFloat((deal.getDiff())));
            diffValue.setText(prettyFloat((deal.getDiffValue())) + "c");

            setGraphic(root);

            setContextMenu(new DealContextMenu(deal));
        }

    }

    @Override
    public void updateSelected(boolean selected) {
//        super.updateSelected(selected);
    }
}
