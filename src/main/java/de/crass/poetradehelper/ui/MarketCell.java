package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.parser.PoeTradeApiParser;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mcrass on 19.07.2018.
 */
public class MarketCell<T> extends javafx.scene.control.ListCell<CurrencyDeal> {

    private FXMLLoader mLLoader;

    @FXML
    private Text sellOffer;

    @FXML
    private Text timestamp;

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

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

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

            String url = deal.getSecondaryCurrencyID().getID() + ".png";
            UIManager.setImage(url, currencyIcon);
            currencyIcon.setCache(true);
            currencyIcon.setCacheHint(CacheHint.SPEED);

            float buy = deal.getBuyAmount();
            float sell = deal.getSellAmount();

            timestamp.setText(timeFormat.format(new Date(deal.getTimestamp())));
            String offersTextString = UIManager.prettyFloat(deal.getOffers());
            if (PropertyManager.getInstance().getCurrentWebParser().equals(PoeTradeApiParser.IDENTIFIER)
                    && deal.getOffers() == PoeTradeApiParser.OFFER_FETCH_AMOUNT * 2) {
                offersTextString = ">" + offersTextString;
            }
            offersText.setText(offersTextString);
            buyOffer.setText(UIManager.prettyFloat(buy, true, true));
            sellOffer.setText(UIManager.prettyFloat(sell, true, true));
            diff.setText(UIManager.prettyFloat((deal.getDiff()), true, false));
            diffValue.setText(UIManager.prettyFloat((deal.getDiffValue()), false, false) + "c");

            setGraphic(root);

            setContextMenu(new DealContextMenu(deal));

            setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                    if (mouseEvent.getClickCount() == 2) {
                        UIManager.getInstance().openOffers(deal.getSecondaryCurrencyID());
                    }
                }
            });
        }
    }
}
