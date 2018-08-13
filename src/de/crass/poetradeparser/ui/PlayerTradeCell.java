package de.crass.poetradeparser.ui;

import de.crass.poetradeparser.model.CurrencyDeal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.CacheHint;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.IOException;

import static de.crass.poetradeparser.PropertyManager.prettyFloat;
import static de.crass.poetradeparser.PropertyManager.setImage;

/**
 * Created by mcrass on 19.07.2018.
 */
public class PlayerTradeCell extends javafx.scene.control.ListCell<CurrencyDeal> {

    private static final float WARNING_DIFF_THRESHOLD = 10;

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
            setOnContextMenuRequested(null);

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
                buyTendency.setEffect(getColorEffect(Color.GRAY));
            } else {
                playerBuy.setFill(marketBuy.getFill());
            }

            float playerSellStock = deal.getPlayerSellStock();
            if (playerSellStock > 0 && playerSellStock < pSell) {
                playerSell.setFill(Color.GRAY);
                sellTendency.setEffect(getColorEffect(Color.GRAY));
            } else {
                playerSell.setFill(marketBuy.getFill());
            }

            // Set icons
            if (buy > 0 && pBuy > 0) {
                if (pBuy > 0 && pBuy >= buy) {
                    setImage(pBuy == buy ? "neut.png" : "nok.png", buyTendency);
                } else if (pBuy > 0) {
                    setImage("ok.png", buyTendency);
                }
                // Check if player offer is too far from the market offers
                if (Math.abs(pBuy - buy) * deal.getcValue() > WARNING_DIFF_THRESHOLD) {
                    buyTendency.setEffect(getColorEffect(Color.RED));
                    buyTendency.setCache(true);
                    buyTendency.setCacheHint(CacheHint.SPEED);
                } else{
                    buyTendency.setEffect(null);
                }
            } else {
                buyTendency.setImage(null);
                buyTendency.setEffect(null);
            }

            if (sell > 0 && pSell > 0) {
                if (pSell > 0 && pSell <= sell) {
                    setImage(pSell == sell ? "neut.png" : "nok.png", sellTendency);
                } else if (pSell > 0) {
                    setImage("ok.png", sellTendency);
                }
                // Check if player offer is too far from the market offers

                if (Math.abs(pSell - sell) * deal.getcValue() > WARNING_DIFF_THRESHOLD) {
                    sellTendency.setEffect(getColorEffect(Color.RED));
                    sellTendency.setCache(true);
                    sellTendency.setCacheHint(CacheHint.SPEED);
                } else{
                    sellTendency.setEffect(null);
                }

            } else {
                sellTendency.setImage(null);
                sellTendency.setEffect(null);
            }

            marketBuy.setText(prettyFloat(buy));
            marketSell.setText(prettyFloat(sell));
            diff.setText(prettyFloat((diffF)));

            playerBuy.setText(prettyFloat(pBuy));
            playerSell.setText(prettyFloat(pSell));

            diffValue.setText(prettyFloat((diffV)) + "c");

            setGraphic(root);

            setContextMenu(new DealContextMenu(deal));
        }
    }

    @Override
    public void updateSelected(boolean selected) {
//        super.updateSelected(selected);
    }

    private static Effect getColorEffect(Color color) {
            Lighting lighting = new Lighting();
            lighting.setDiffuseConstant(1.0);
            lighting.setSpecularConstant(0.0);
            lighting.setSurfaceScale(0.0);
            lighting.setLight(new Light.Distant(45, 45, color));

           return lighting;
    }
}
