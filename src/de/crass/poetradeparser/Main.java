package de.crass.poetradeparser;/**
 * Created by mcrass on 19.07.2018.
 */

import de.crass.poetradeparser.model.CurrencyDeal;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.parser.TradeManager;
import de.crass.poetradeparser.ui.CurrencyOfferCell;
import de.crass.poetradeparser.ui.PlayerTradeCell;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Main extends Application {

    public static final String versionText = "v0.2-SNAPSHOT";

    @FXML
    private Button addPlayerButton;

    @FXML
    private TextArea console;

    @FXML
    private Button removePlayerButton;

    @FXML
    private ListView<CurrencyDeal> playerList;

    @FXML
    private ListView<CurrencyDeal> currencyList;

    @FXML
    private Button updateButton;

    @FXML
    private ComboBox<String> playerComboBox;

    @FXML
    private ComboBox<CurrencyID> primaryComboBox;

    @FXML
    private Label version;

    private TradeManager tradeManager;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        primaryStage.setTitle("PoeTradeParser");
        primaryStage.setScene(new Scene(root, 600, 650));
        primaryStage.show();
    }

    @FXML
    void initialize() {
        assert console != null : "fx:id=\"console\" was not injected: check your FXML file 'layout.fxml'.";
        assert currencyList != null : "fx:id=\"currencyList\" was not injected: check your FXML file 'layout.fxml'.";
        assert updateButton != null : "fx:id=\"updateButton\" was not injected: check your FXML file 'layout.fxml'.";
        assert version != null : "fx:id=\"version\" was not injected: check your FXML file 'layout.fxml'.";

        tradeManager = new TradeManager();
        LogManager.getInstance().setConsole(console);
        setupUI();

        LogManager.getInstance().log(getClass(), "Started");
    }

    private void setupUI() {
        version.setText(versionText);

        currencyList.setEditable(false);
        currencyList.setCellFactory(new Callback<ListView<CurrencyDeal>, ListCell<CurrencyDeal>>() {
            @Override
            public ListCell<CurrencyDeal> call(ListView<CurrencyDeal> studentListView) {
                return new CurrencyOfferCell<>();
            }
        });

        playerList.setEditable(false);
        playerList.setCellFactory(new Callback<ListView<CurrencyDeal>, ListCell<CurrencyDeal>>() {
            @Override
            public ListCell<CurrencyDeal> call(ListView<CurrencyDeal> studentListView) {
                return new PlayerTradeCell<>();
            }
        });

        ObservableList<CurrencyDeal> currentDeals = tradeManager.getCurrentDeals();
        ObservableList<CurrencyDeal> playerDeals = tradeManager.getPlayerDeals();
        currencyList.setItems(currentDeals);
        playerList.setItems(playerDeals);

        playerComboBox.setItems(PropertyManager.getInstance().getPlayerList());

        updateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                tradeManager.updateOffers();
            }
        });


        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.values());
//        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.EXALTED);
        primaryComboBox.setItems(currencyList);
        primaryComboBox.setValue(PropertyManager.getInstance().getPrimaryCurrency());
        primaryComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                CurrencyID newValue = primaryComboBox.getValue();
                PropertyManager.getInstance().setPrimaryCurrency(newValue);

            }
        });
    }


}
