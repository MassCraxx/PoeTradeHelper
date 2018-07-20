package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.LogManager;
import de.crass.poetradeparser.model.CurrencyDeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;

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
                    LogManager.getInstance().log(getClass(), "Exception on loading image! " + e);
                } finally {
                    if(fs != null){
                        try {
                            fs.close();
                        } catch (IOException e) {
                            LogManager.getInstance().log(getClass(), "Exception on loading image! " + e);
                        }
                    }
                }
            } else {
                LogManager.getInstance().log(getClass(), "Image " + url + " for currency " + deal.getSecondaryCurrencyID().toString() +
                        " not found!");
            }

            float buy = deal.getBuyAmount();
            float sell = deal.getSellAmount();

            chaosValue.setText(prettyFloat(deal.getcValue()) + "c");
            offersText.setText(prettyFloat(deal.getOffers()));
            buyOffer.setText(prettyFloat(buy));
            sellOffer.setText(prettyFloat(sell));
            diff.setText(prettyFloat((deal.getDiff())));
            diffValue.setText(prettyFloat((deal.getDiffValue())) + "c");

            setGraphic(root);
        }

    }

    String prettyFloat(float in){
//        return String.format(Locale.ENGLISH, "%.2f", in);
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return String.valueOf(df.format(in));
    }

}
