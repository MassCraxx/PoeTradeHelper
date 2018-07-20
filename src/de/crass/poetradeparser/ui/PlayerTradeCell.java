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
public class PlayerTradeCell<T> extends javafx.scene.control.ListCell<CurrencyDeal> {

    private FXMLLoader mLLoader;

    @FXML
    private AnchorPane root;

    @FXML
    private Text playerSell;

    @FXML
    private Text marketSell;

    @FXML
    private Text diff;

    @FXML
    private ImageView currencyIcon;

    @FXML
    private Text marketBuy;

    @FXML
    private Text diffValue;

    @FXML
    private Text playerBuy;

    @Override
    protected void updateItem(CurrencyDeal deal, boolean empty) {
        super.updateItem(deal, empty);

        if (empty || deal == null) {

            setText(null);
            setGraphic(null);

        } else {
            if (mLLoader == null) {
                mLLoader = new FXMLLoader(getClass().getResource("player_cell.fxml"));
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

            float pBuy = deal.getPlayerBuyAmount();
            float pSell = deal.getPlayerSellAmount();

            float diffF = 0;

            float diffV = 0;
            if(pBuy != 0 && pSell != 0){
                diffF = pBuy - pSell;
                diffV = diffF * deal.getcValue();
            }

            if(pBuy > 0 && pBuy > buy){
//                playerBuy.setStyle("-fx-text-fill: red;");
                playerBuy.setFill(Color.RED);
            } else{
                playerBuy.setFill(Color.BLACK);
            }

            if(pSell > 0 && pSell < sell){
                playerSell.setFill(Color.RED);
            }else{
                playerSell.setFill(Color.BLACK);
            }

            marketBuy.setText(prettyFloat(buy));
            marketSell.setText(prettyFloat(sell));
            diff.setText(prettyFloat((diffF)));

            playerBuy.setText(prettyFloat(pBuy));
            playerSell.setText(prettyFloat(pSell));

            diffValue.setText(prettyFloat((diffV)) + "c");

            setGraphic(root);
        }

    }

    String prettyFloat(float in){
//        return String.format(Locale.ENGLISH, "%.2f", in);
        if(in == 0){
            return "---";
        }
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);
        return String.valueOf(df.format(in));
    }

}
