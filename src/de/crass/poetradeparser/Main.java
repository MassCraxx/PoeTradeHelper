package de.crass.poetradeparser;/**
 * Created by mcrass on 19.07.2018.
 */

import de.crass.poetradeparser.model.CurrencyDeal;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.parser.ParseListener;
import de.crass.poetradeparser.parser.TradeManager;
import de.crass.poetradeparser.ui.CurrencyOfferCell;
import de.crass.poetradeparser.ui.PlayerTradeCell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class Main extends Application implements ParseListener {

    public static final String title = "PoeTradeParser";
    public static final String versionText = "v0.3.0-SNAPSHOT";

    @FXML
    private ListView<CurrencyDeal> playerDealList;

    @FXML
    private TextArea console;

    @FXML
    private Button removeCurrencyFilterBtn;

    @FXML
    private ListView<CurrencyDeal> currencyList;

    @FXML
    private CheckBox filterWithoutAPI;

    @FXML
    private ListView<CurrencyID> currencyFilterList;

    @FXML
    private ComboBox<CurrencyID> primaryComboBox;

    @FXML
    private Label version;

    @FXML
    private ComboBox<CurrencyID> currencyFilterCB;

    @FXML
    private Button addPlayerButton;

    @FXML
    private Button removePlayerBtn;

    @FXML
    private TextField playerField;

    @FXML
    private ComboBox<String> leagueCB;

    @FXML
    private Button updateButton;

    @FXML
    private CheckBox filterInvalid;

    @FXML
    private ListView<String> playerListView;

    @FXML
    private Button addCurrencyFilterBtn;

    private static Stage currentStage;

    private TradeManager tradeManager;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        Scene scene = new Scene(root, 600, 650);
        scene.getStylesheets().add(getClass().getResource("stylesheet.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();

        currentStage = primaryStage;
        updateTitle();
    }

    @FXML
    void initialize() {
        assert console != null : "fx:id=\"console\" was not injected: check your FXML file 'layout.fxml'.";
        assert currencyList != null : "fx:id=\"currencyList\" was not injected: check your FXML file 'layout.fxml'.";
        assert updateButton != null : "fx:id=\"updateButton\" was not injected: check your FXML file 'layout.fxml'.";
        assert version != null : "fx:id=\"version\" was not injected: check your FXML file 'layout.fxml'.";

        tradeManager = new TradeManager();
        tradeManager.registerListener(this);

        LogManager.getInstance().setConsole(console);
        setupUI();

        LogManager.getInstance().log(getClass(), "Started");
    }

    @Override
    public void stop(){
        LogManager.getInstance().log(getClass(), "Shutting down app.");
        PropertyManager.getInstance().storeProperties();
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
        currencyList.setItems(tradeManager.getCurrentDeals());

        playerDealList.setEditable(false);
        playerDealList.setCellFactory(new Callback<ListView<CurrencyDeal>, ListCell<CurrencyDeal>>() {
            @Override
            public ListCell<CurrencyDeal> call(ListView<CurrencyDeal> studentListView) {
                return new PlayerTradeCell();
            }
        });
        playerDealList.setItems(tradeManager.getPlayerDeals());

        currencyList.setPlaceholder(new Label("Update to fill lists."));
        playerDealList.setPlaceholder(new Label("Update to fill lists."));

        updateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!tradeManager.isUpdating()) {
                    tradeManager.updateOffers();
                    currencyList.setPlaceholder(new Label("Updating..."));
                    playerDealList.setPlaceholder(new Label("Updating..."));
                    updateButton.setText("Cancel");
                } else {
                    tradeManager.cancelUpdate();
                    updateButton.setDisable(true);
                }
            }
        });

        // SETTINGS
        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.values());
        primaryComboBox.setItems(currencyList);
        primaryComboBox.setValue(PropertyManager.getInstance().getPrimaryCurrency());
        primaryComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                CurrencyID newValue = primaryComboBox.getValue();
                PropertyManager.getInstance().setPrimaryCurrency(newValue);
            }
        });

        filterInvalid.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().setFilterOutOfStock(filterInvalid.isSelected());
                tradeManager.parseDeals(true);
            }
        });
        filterInvalid.setSelected(PropertyManager.getInstance().getFilterOutOfStock());

        filterWithoutAPI.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().setFilterNoApi(filterWithoutAPI.isSelected());
                tradeManager.parseDeals(true);
            }
        });
        filterWithoutAPI.setSelected(PropertyManager.getInstance().getFilterNoApi());

        currencyFilterList.setItems(PropertyManager.getInstance().getFilterList());

        currencyFilterCB.setItems(FXCollections.observableArrayList(CurrencyID.values()));

        addCurrencyFilterBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                CurrencyID newCurrency = currencyFilterCB.getValue();
                List<CurrencyID> filterList = PropertyManager.getInstance().getFilterList();
                if (!filterList.contains(newCurrency)) {
                    filterList.add(newCurrency);
                }
            }
        });

        removeCurrencyFilterBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().getFilterList().remove(currencyFilterList.getFocusModel().getFocusedItem());
            }
        });

        ObservableList<String> playerList = PropertyManager.getInstance().getPlayerList();
        playerListView.setItems(playerList);
        addPlayerButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String newPlayer = playerField.getText();
                if (!newPlayer.isEmpty() && !playerList.contains(newPlayer))
                    playerList.add(newPlayer);
            }
        });

        removePlayerBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                playerList.remove(playerListView.getFocusModel().getFocusedItem());
            }
        });

        leagueCB.setItems(tradeManager.getLeagueList());
        leagueCB.setValue(PropertyManager.getInstance().getCurrentLeague());

        leagueCB.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().setLeague(leagueCB.getValue());
                updateTitle();
                tradeManager.updateCurrencyValues();
            }
        });
    }

    private void updateTitle(){
        if(currentStage == null){
            LogManager.getInstance().log(getClass(), "Error setting title.");
            return;
        }
        currentStage.setTitle(title + " - " + PropertyManager.getInstance().getCurrentLeague() + " League");
    }

    @Override
    public void onParsingFinished() {
        currencyList.setPlaceholder(new Label("No deals to show."));
        playerDealList.setPlaceholder(new Label("No deals to show. Is your player set in settings?"));

        updateButton.setText("Update");
        updateButton.setDisable(false);
    }

    public static String prettyFloat(float in) {
        if (in == 0) {
            return "---";
        }
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return String.valueOf(df.format(in));
    }

    public static void setImage(String name, ImageView view) {
        String url = "./res/" + name;
        File iconFile = new File(url);
        if (iconFile.exists()) {
            Image image = new Image(iconFile.toURI().toString());
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    view.setImage(image);
                }
            });
        } else {
            LogManager.getInstance().log(PropertyManager.class, "Image " + url + " not found!");
        }
    }
}
