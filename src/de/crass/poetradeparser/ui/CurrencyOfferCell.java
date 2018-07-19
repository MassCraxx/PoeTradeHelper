package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.Main;
import de.crass.poetradeparser.model.CurrencyDeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.*;

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

            root.setBorder(new Border(new BorderStroke(Color.BLACK,
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));

            String url = "./res/" + deal.getSecondaryCurrencyID().getID() + ".png";
            File iconFile = new File(url);
            InputStream fs = null;
            if (iconFile.exists()) {
                try {
                    fs = new FileInputStream(iconFile);
                    currencyIcon.setImage(new Image(fs));
                } catch (FileNotFoundException | IllegalArgumentException e) {
                    Main.log(getClass(), "Exception on loading image! " + e);
                } finally {
                    if(fs != null){
                        try {
                            fs.close();
                        } catch (IOException e) {
                            Main.log(getClass(), "Exception on loading image! " + e);
                        }
                    }
                }
            } else {
                Main.log(getClass(), "Image " + url + " for currency " + deal.getSecondaryCurrencyID().toString() +
                        " not found!");
            }

            float buy = deal.getBuyAmount();
            float sell = deal.getSellAmount();
            float diffF = buy - sell;
            float diffV = diffF * deal.getcValue();

            chaosValue.setText(String.valueOf(deal.getcValue()));
            offersText.setText(String.valueOf(deal.getOffers()));
            buyOffer.setText(String.valueOf(buy));
            sellOffer.setText(String.valueOf(sell));
            diff.setText(String.valueOf((diffF)));
            diffValue.setText(String.valueOf((diffV)));

            setGraphic(root);
        }

    }

}
