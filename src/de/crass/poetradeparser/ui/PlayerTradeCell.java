package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.model.CurrencyDeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import static de.crass.poetradeparser.PropertyManager.setImage;

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

    @FXML
    private ImageView buyTendency;

    @FXML
    private ImageView sellTendency;

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

//            root.setBorder(new Border(new BorderStroke(Color.BLACK,
//                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));

            setImage(deal.getSecondaryCurrencyID().getID() + ".png", currencyIcon);

            Label test = new Label();
            test.setTooltip(new Tooltip("aaaw yeee"));

            test.setGraphic(currencyIcon);

            float buy = deal.getBuyAmount();
            float sell = deal.getSellAmount();

            float pBuy = deal.getPlayerBuyAmount();
            float pSell = deal.getPlayerSellAmount();

            float diffF = 0;

            float diffV = 0;
            if (pBuy != 0 && pSell != 0) {
                diffF = pBuy - pSell;
                diffV = diffF * deal.getcValue();
            }

            float playerBuyStock = deal.getPlayerBuyStock();
            // buystock is primary, but buy is secondary
            if (playerBuyStock > 0 && playerBuyStock < 1) {
                playerBuy.setFill(Color.GRAY);
            } else {
                playerBuy.setFill(marketBuy.getFill());
            }

            float playerSellStock = deal.getPlayerSellStock();
            if (playerSellStock > 0 && playerSellStock < pSell) {
                playerSell.setFill(Color.GRAY);
            } else {
                playerSell.setFill(marketBuy.getFill());
            }

            if (pBuy > 0 && pBuy >= buy) {
                setImage(pBuy == buy ? "neut.png" : "nok.png", buyTendency);
            } else if (pBuy > 0) {
                setImage("ok.png", buyTendency);
            } else {
                buyTendency.setImage(null);
            }

            if (pSell > 0 && pSell <= sell) {
                setImage(pSell == sell ? "neut.png" : "nok.png", sellTendency);
            } else if (pSell > 0) {
                setImage("ok.png", sellTendency);
            } else {
                sellTendency.setImage(null);
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


    String prettyFloat(float in) {
//        return String.format(Locale.ENGLISH, "%.2f", in);
        if (in == 0) {
            return "---";
        }
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);
        return String.valueOf(df.format(in));
    }

    @Override
    public void updateSelected(boolean selected) {
//        super.updateSelected(selected);
    }
}
