package de.crass.poetradehelper;
/*
  Created by mcrass on 19.07.2018.
 */

import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.PoeNinjaParser;
import de.crass.poetradehelper.parser.PoeTradeApiParser;
import de.crass.poetradehelper.parser.PoeTradeWebParser;
import de.crass.poetradehelper.parser.TradeManager;
import de.crass.poetradehelper.tts.PoeChatTTS;
import de.crass.poetradehelper.ui.CurrencyContextMenu;
import de.crass.poetradehelper.ui.MarketCell;
import de.crass.poetradehelper.ui.OfferContextMenu;
import de.crass.poetradehelper.ui.PlayerTradeCell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.*;

@SuppressWarnings("unchecked")
public class Main extends Application implements TradeManager.DealParseListener, PoeNinjaParser.PoeNinjaListener, PropertyManager.UICallback {

    private static final String title = "PoeTradeHelper";
    private static final String versionText = "v0.7-SNAPSHOT";

    @FXML
    private ListView<CurrencyDeal> playerDealList;

    @FXML
    private CheckBox voiceReadCurOffers;

    @FXML
    private CheckBox voiceReadTradeOffers;

    @FXML
    private TextArea console;

    @FXML
    private Button removeCurrencyFilterBtn;

    @FXML
    private ListView<CurrencyDeal> currencyList;

    @FXML
    private CheckBox filterWithoutAPI;

    @FXML
    private CheckBox filterExcessive;

    @FXML
    private CheckBox voiceActive;

    @FXML
    private ListView<CurrencyID> currencyFilterList;

    @FXML
    private ComboBox<CurrencyID> primaryComboBox;

    @FXML
    private ComboBox<CurrencyID> currencyFilterCB;

    @FXML
    private Label version;

    @FXML
    private ComboBox<String> voiceSpeakerCB;

    @FXML
    private ComboBox<String> webParsingCB;

    @FXML
    private Button addPlayerButton;

    @FXML
    private Button removePlayerBtn;

    @FXML
    private ComboBox<String> leagueCB;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Label volumeLabel;

    @FXML
    private TextField playerField;

    @FXML
    private Button updateButton;

    @FXML
    private CheckBox voiceReadChat;

    @FXML
    private CheckBox filterInvalid;

    @FXML
    private ListView<String> playerListView;

    @FXML
    private Button addCurrencyFilterBtn;

    @FXML
    private CheckBox voiceReadAFK;

    @FXML
    private CheckBox voiceRandom;

    @FXML
    private Button restoreCurrencyFilterBtn;

    @FXML
    private TextField poePath;

    @FXML
    private TableView<Map.Entry<CurrencyID, Float>> valueTable;

    @FXML
    private Button updateValuesButton;

//    @FXML
//    private Button updatePlayerButton;

    @FXML
    private TextField valueInputText;

    @FXML
    private TextField valueOutputText;

    @FXML
    private ComboBox<CurrencyID> valueInputCB;

    @FXML
    private ComboBox<CurrencyID> valueOutputCB;

    @FXML
    private Button convertButton;

    @FXML
    private Button openConversionInBrowser;

    @FXML
    private CheckBox autoUpdate;

    @FXML
    private TableView<CurrencyOffer> buyOfferTable;

    @FXML
    private TableView<CurrencyOffer> sellOfferTable;

    @FXML
    private ComboBox<CurrencyID> offerSecondary;

    @FXML
    private Button refreshBtn;

    @FXML
    private Slider updateSlider;

    @FXML
    private Label updateTime;

    @FXML
    private TextField voiceShoutoutWords;

    @FXML
    private TextField voiceExcludeWords;

    @FXML
    private Button voiceTestButton;

    @FXML
    private Text volumeTopicLabel;

    @FXML
    private Label excessiveTresholdLabel;

    @FXML
    private Slider excessiveTresholdSlider;

    @FXML
    private CheckBox filterMultiTrade;

    private static Stage currentStage;
    public static PoeChatTTS poeChatTTS;

    private TradeManager tradeManager;

    public static boolean currencyFilterChanged = false;

    private static DecimalFormat dFormat = new DecimalFormat("#0.##");
    private static DecimalFormat valueFormat;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        Scene scene = new Scene(root, 650, 650);
        scene.getStylesheets().add(getClass().getResource("stylesheet.css").toExternalForm());
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(225);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("icon.png")));
        primaryStage.show();

        currentStage = primaryStage;
        updateTitle();
    }

    @FXML
    void initialize() {
        //TODO: Somehow fetch available items before prop man starts up

        // Setup console
        LogManager.getInstance().setConsole(console);

        // Setup trade manager
        tradeManager = TradeManager.getInstance();
        tradeManager.registerListener(this, this);

        // Prop man
        PropertyManager.getInstance().setUICallback(this);
        setWebParser(PropertyManager.getInstance().getProp("trade_data_source", PoeTradeApiParser.IDENTIFIER));

        // Setup TTS
        poeChatTTS = new PoeChatTTS(() -> {
            voiceActive.setSelected(false);
            poePath.setDisable(false);
        });

        // Setup UI
        setupUI();

        LogManager.getInstance().log(getClass(), "Started");
    }

    @Override
    public void stop() {
        LogManager.getInstance().log(getClass(), "Shutting down app.");
        PropertyManager.getInstance().storeProperties();

        if (poeChatTTS != null && poeChatTTS.isActive()) {
            poeChatTTS.stopTTS();
            poeChatTTS = null;
        }

        if (TradeManager.getInstance() != null) {
            TradeManager.getInstance().release();
        }

        LogManager.getInstance().log(getClass(), "Shutdown complete.");
    }

    private int versionClicked = 0;
    private int volumeClicked = 0;

    private void setupUI() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        dFormat.setDecimalFormatSymbols(symbols);
        dFormat.setRoundingMode(RoundingMode.HALF_UP);

        valueFormat = (DecimalFormat) dFormat.clone();
        valueFormat.applyPattern("0.00");

        version.setText(versionText);
        version.setOnMouseClicked(event -> {
            versionClicked++;
            if (versionClicked % 10 == 0) {
                JOptionPane.showMessageDialog(null, "Automatic Update setting enabled! Please don't abuse this.");
                autoUpdate.setVisible(true);
                updateTime.setVisible(true);
                updateSlider.setVisible(true);
            }
        });
        ContextMenu vContext = new ContextMenu();
        vContext.getItems().add(new MenuItem("by MassCraxx"));
        version.setContextMenu(vContext);

        currencyList.setEditable(false);
        currencyList.setCellFactory(studentListView -> new MarketCell<>());
        currencyList.setItems(tradeManager.getCurrentDeals());

        playerDealList.setEditable(false);
        playerDealList.setCellFactory(studentListView -> new PlayerTradeCell());
        playerDealList.setItems(tradeManager.getPlayerDeals());

        currencyList.setPlaceholder(new Label("Update to fill lists."));
        playerDealList.setPlaceholder(new Label("Update to fill lists."));

        updateButton.setTooltip(new Tooltip("Fetch offers from poe.trade for currency configured in settings"));
        updateButton.setOnAction(event -> {
            if (tradeManager.isUpdating()) {
                tradeManager.cancelUpdate();
                updateButton.setDisable(true);
//                    updatePlayerButton.setDisable(true);
            } else {
                tradeManager.updateOffers(currencyFilterChanged);
//                    updatePlayerButton.setDisable(true);
            }
        });

//        updatePlayerButton.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                if(updateTimer != null){
//                    stopUpdateTask();
//                    startUpdateTask();
//                }
//                if (tradeManager.isUpdating()) {
//                    tradeManager.cancelUpdate();
//                    updateButton.setDisable(true);
//                    updatePlayerButton.setDisable(true);
//                } else {
//                    tradeManager.updatePlayerOffers();
//                    updateButton.setDisable(true);
//                }
//            }
//        });

        // Offer tab
        ObservableList<CurrencyID> currencies = FXCollections.observableArrayList(CurrencyID.getValues());
        currencies.sort(Comparator.comparing(Object::toString));
        offerSecondary.setItems(currencies);
        offerSecondary.setOnAction(event -> new Thread(() -> {
            CurrencyID newValue = offerSecondary.getValue();
            if (newValue != null) {
                buyOfferTable.setItems(null);
                sellOfferTable.setItems(null);
                buyOfferTable.setItems(tradeManager.getBuyOffers(newValue));
                sellOfferTable.setItems(tradeManager.getSellOffers(newValue));
            }
        }).start());

        // Buy table
        Callback<TableColumn.CellDataFeatures<CurrencyOffer, Number>, ObservableValue<Number>> stockCellFactory =
                param -> {
                    int stock = param.getValue().getStock();
                    if (stock < 0) {
                        return null;
                    }
                    return new SimpleIntegerProperty(stock);
                };

        Callback<TableColumn.CellDataFeatures<CurrencyOffer, String>, ObservableValue<String>> playerCellFactory =
                param -> new SimpleStringProperty(param.getValue().getPlayerName());

        TableColumn<CurrencyOffer, String> valueColumn = new TableColumn<>();
        valueColumn.setText("Price");
        valueColumn.setCellValueFactory(param -> new SimpleStringProperty(prettyFloat(param.getValue().getBuyAmount() / param.getValue().getSellAmount())));
        valueColumn.setPrefWidth(50);

        TableColumn<CurrencyOffer, Number> stockColumn = new TableColumn<>();
        stockColumn.setText("Stock");
        stockColumn.setCellValueFactory(stockCellFactory);
        stockColumn.setPrefWidth(50);

        TableColumn<CurrencyOffer, String> playerColumn = new TableColumn<>();
        playerColumn.setText("Character");
        playerColumn.setCellValueFactory(playerCellFactory);
        playerColumn.setPrefWidth(200);


//        TableColumn<CurrencyOffer, String> apiColumn = new TableColumn<>();
//        apiColumn.setText("API");
//        apiColumn.setCellValueFactory(param -> new SimpleStringProperty((param.getValue().getStock() < 0) ? "" : "X"));

        buyOfferTable.getColumns().clear();
        buyOfferTable.getColumns().addAll(valueColumn, stockColumn, playerColumn);

        buyOfferTable.setContextMenu(new OfferContextMenu(buyOfferTable, offerSecondary));

        // Sell table
        TableColumn<CurrencyOffer, String> sellValueColumn = new TableColumn<>();
        sellValueColumn.setText("Price");
        sellValueColumn.setCellValueFactory(param -> new SimpleStringProperty(prettyFloat(param.getValue().getSellAmount() / param.getValue().getBuyAmount())));
        sellValueColumn.setPrefWidth(50);

        TableColumn<CurrencyOffer, Number> sellStockColumn = new TableColumn<>();
        sellStockColumn.setText("Stock");
        sellStockColumn.setCellValueFactory(stockCellFactory);
        sellStockColumn.setPrefWidth(50);

        TableColumn<CurrencyOffer, String> sellPlayerColumn = new TableColumn<>();
        sellPlayerColumn.setText("Character");
        sellPlayerColumn.setCellValueFactory(playerCellFactory);
        sellPlayerColumn.setPrefWidth(200);

//        TableColumn<CurrencyOffer, String> sellApiColumn = new TableColumn<>();
//        sellApiColumn.setText("API");
//        sellApiColumn.setCellValueFactory(param -> new SimpleStringProperty((param.getValue().getStock() < 0) ? "" : "X"));

        sellOfferTable.getColumns().clear();
        sellOfferTable.getColumns().addAll(sellValueColumn, sellStockColumn, sellPlayerColumn);
        sellOfferTable.setContextMenu(new OfferContextMenu(sellOfferTable, offerSecondary));


        refreshBtn.setOnAction(event -> {
            if (offerSecondary.getValue() != null) {
                tradeManager.updateOffersForCurrency(offerSecondary.getValue(), true);
            }
        });

        // Value Tab
        valueTable.setRowFactory(tv -> new TableRow<Map.Entry<CurrencyID, Float>>() {
            @Override
            public void updateItem(Map.Entry<CurrencyID, Float> item, boolean empty) {
                // updates on refresh
                super.updateItem(item, empty);
                if (item != null && PropertyManager.getInstance().getPrimaryCurrency().equals(item.getKey())) {
                    setStyle("-fx-background-color: -fx-accent;");
                } else {
                    setStyle("");
                }
            }
        });

        valueTable.setContextMenu(new CurrencyContextMenu(valueTable));

        TableColumn<Map.Entry<CurrencyID, Float>, String> column = new TableColumn<>();
        column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey().toString()));

        TableColumn<Map.Entry<CurrencyID, Float>, Number> column2 = new TableColumn<>();
        column2.setCellValueFactory(param -> new SimpleFloatProperty(param.getValue().getValue()));
        column2.setCellFactory(tc -> new TableCell<Map.Entry<CurrencyID, Float>, Number>() {
            @Override
            protected void updateItem(Number number, boolean empty) {
                super.updateItem(number, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(prettyFloat(number.floatValue(), valueFormat) + "c");
                }
            }
        });

        column.setText("Currency");
        column2.setText("Value");
        column2.setMinWidth(80);
        column2.setMaxWidth(100);
        column2.setStyle("-fx-alignment: CENTER-RIGHT;");

        valueTable.getColumns().clear();
        valueTable.getColumns().addAll(column, column2);

        updateValuesButton.setOnAction(event -> tradeManager.updateCurrencyValues());

        valueInputCB.setItems(currencies);
        valueInputCB.setValue(CurrencyID.getByTradeID("exa"));
        valueOutputCB.setItems(currencies);
        valueOutputCB.setValue(CurrencyID.getByTradeID("chaos"));
        valueOutputCB.setOnAction(event -> calculateValue());
        valueInputCB.setOnAction(event -> calculateValue());

        valueInputText.setOnKeyTyped(event -> valueOutputText.setText(""));

        valueOutputText.setOnKeyTyped(event -> valueInputText.setText(""));

        convertButton.setOnAction(event -> calculateValue());

        //FIXME
        openConversionInBrowser.setDisable(true);
//        openConversionInBrowser.setOnAction(event -> Main.openInBrowser(PropertyManager.getInstance().getCurrentLeague(),
////                valueOutputCB.getValue(), valueInputCB.getValue()));

        // SETTINGS
        primaryComboBox.setTooltip(new Tooltip("Select currency to flip with"));
        primaryComboBox.setItems(PropertyManager.getInstance().getPrimaryCurrencyList());
        primaryComboBox.setValue(PropertyManager.getInstance().getPrimaryCurrency());
        primaryComboBox.setOnAction(event -> {
            CurrencyID newValue = primaryComboBox.getValue();
            PropertyManager.getInstance().setPrimaryCurrency(newValue);
            tradeManager.parseDeals();
            valueTable.refresh();
        });

        filterInvalid.setSelected(PropertyManager.getInstance().getFilterOutOfStock());
        filterInvalid.setTooltip(new Tooltip("Ignore all offers that do have a stock value but not enough on stock to sell"));
        filterInvalid.setOnAction(event -> {
            PropertyManager.getInstance().setFilterOutOfStock(filterInvalid.isSelected());
            tradeManager.parseDeals();
        });

        filterWithoutAPI.setSelected(PropertyManager.getInstance().getFilterNoApi());
        filterWithoutAPI.setTooltip(new Tooltip("Ignore all offers that don't have stock information"));
        filterWithoutAPI.setOnAction(event -> {
            PropertyManager.getInstance().setFilterNoApi(filterWithoutAPI.isSelected());
            tradeManager.parseDeals();
        });

        filterExcessive.setSelected(PropertyManager.getInstance().getFilterExcessive());
        filterExcessive.setTooltip(new Tooltip("Ignore all offers that have an insane buy to sell value ratio"));
        filterExcessive.setOnAction(event -> {
            PropertyManager.getInstance().setFilterExcessive(filterExcessive.isSelected());
            tradeManager.parseDeals();
        });

        excessiveTresholdSlider.setTooltip(new Tooltip("If \"Filter Excessive\" is active, all deals where the buy and sell value difference exceeds a given percentage of the higher value are ignored.\nE.g. 50% means all offers will be ignored where the buy and sell difference is more than 50% of the primary currency value"));
        int excessiveTreshold = PropertyManager.getInstance().getExcessiveTreshold();
        excessiveTresholdSlider.setValue(excessiveTreshold);
        excessiveTresholdLabel.setText(excessiveTreshold + " %");
        excessiveTresholdSlider.valueProperty().addListener((observable, oldValue, newValue) -> excessiveTresholdLabel.setText(newValue.intValue() + " %"));
        excessiveTresholdSlider.valueChangingProperty().addListener((observable, changeEnds, changeStarts) -> {
            if (changeEnds) {
                int newUpdateDelay = (int) excessiveTresholdSlider.getValue();
                PropertyManager.getInstance().setExcessiveTreshold(newUpdateDelay);
                tradeManager.parseDeals();
            }
        });

        filterMultiTrade.setTooltip(new Tooltip("Skip updating currencies that would require multiple full inventories to trade against one unit of primary"));
        filterMultiTrade.setSelected(PropertyManager.getInstance().getFilterMultipleTransactionDeals());
        filterMultiTrade.setOnAction(event -> {
            PropertyManager.getInstance().setFilterMultipleTransactionDeals(filterMultiTrade.isSelected());
            tradeManager.parseDeals();
        });

        currencyFilterList.setItems(PropertyManager.getInstance().getFilterList());
        currencyFilterList.setTooltip(new Tooltip("Only offers for currency in this list will be fetched on update"));

        currencyFilterCB.setItems(currencies);
        currencyFilterCB.setPromptText("Add currency");

        addCurrencyFilterBtn.setTooltip(new Tooltip("Add selected currency to list"));
        addCurrencyFilterBtn.setOnAction(event -> {
            CurrencyID newCurrency = currencyFilterCB.getValue();
            if (newCurrency != null) {
                List<CurrencyID> filterList = PropertyManager.getInstance().getFilterList();
                if (!filterList.contains(newCurrency)) {
                    currencyFilterChanged = true;
                    filterList.add(newCurrency);
                }
            }
        });

        removeCurrencyFilterBtn.setTooltip(new Tooltip("Remove selected currency from list"));
        removeCurrencyFilterBtn.setOnAction(event -> {
            CurrencyID focus = currencyFilterList.getFocusModel().getFocusedItem();
            if (focus != null) {
                currencyFilterChanged = true;
                PropertyManager.getInstance().getFilterList().remove(focus);
            }
        });

        restoreCurrencyFilterBtn.setTooltip(new Tooltip("Restore default currency list"));
        restoreCurrencyFilterBtn.setOnAction(event -> PropertyManager.getInstance().resetFilterList());

        ObservableList<String> playerList = PropertyManager.getInstance().getPlayerList();
        playerListView.setItems(playerList);
        playerListView.setTooltip(new Tooltip("Offers from account in this list will be shown in PlayerOverview"));

        addPlayerButton.setTooltip(new Tooltip("Add an account name from the text field to the list"));
        addPlayerButton.setOnAction(event -> {
            String newPlayer = playerField.getText();
            if (!newPlayer.isEmpty() && !playerList.contains(newPlayer))
                playerList.add(newPlayer);
            playerField.setText("");
        });

        removePlayerBtn.setTooltip(new Tooltip("Remove selected player from list"));
        removePlayerBtn.setOnAction(event -> playerList.remove(playerListView.getFocusModel().getFocusedItem()));

        leagueCB.setTooltip(new Tooltip("Set Path of Exile league"));
        leagueCB.setItems(tradeManager.getLeagueList());
        leagueCB.setValue(PropertyManager.getInstance().getCurrentLeague());

        leagueCB.setOnAction(event -> PropertyManager.getInstance().setLeague(leagueCB.getValue()));

        webParsingCB.setItems(FXCollections.observableArrayList(PoeTradeWebParser.IDENTIFIER, PoeTradeApiParser.IDENTIFIER));
        String defaultParser = PropertyManager.getInstance().getProp("trade_data_source", PoeTradeApiParser.IDENTIFIER);
        webParsingCB.setValue(defaultParser);
        webParsingCB.setOnAction(event -> setWebParser(webParsingCB.getValue()));

        // Setup Voice Controls
        poeChatTTS.setWordIncludeTextField(voiceShoutoutWords);
        poeChatTTS.setWordExcludeTextField(voiceExcludeWords);
        List<String> supportedVoices = poeChatTTS.getSupportedVoices();
        if (supportedVoices == null) {
            setDisableVoiceControls();

            voiceActive.setOnAction(event -> {
                File jarDir = new File("");
                LogManager.getInstance().log(getClass(), "Balcon not found! To enable TTS notifications, download balcon.exe from http://balabolka.site/balcon.zip place it in " + jarDir.getAbsolutePath() + " and restart the application.");
                voiceActive.setSelected(false);
            });
        } else if (supportedVoices.isEmpty()) {
            LogManager.getInstance().log(getClass(), "TTS is disabled: No supported voices found.");
            setDisableVoiceControls();
        } else {
            voiceActive.setOnAction(event -> {
                if (voiceActive.isSelected()) {
                    poeChatTTS.startTTS();
                    poePath.setDisable(true);
                    String newPath = poePath.getText();
                    poeChatTTS.setPath(newPath);
                    PropertyManager.getInstance().setPathOfExilePath(newPath);
                } else {
                    poeChatTTS.stopTTS();
                    poePath.setDisable(false);
                }
            });

            voiceSpeakerCB.setItems(FXCollections.observableArrayList(supportedVoices));
            voiceSpeakerCB.setValue(poeChatTTS.getVoice());
            voiceSpeakerCB.setOnAction(event -> {
                String selected = voiceSpeakerCB.getValue();
                if (!selected.isEmpty()) {
                    poeChatTTS.setVoice(selected);
                }
            });

            voiceReadAFK.setSelected(poeChatTTS.isReadAFK());
            voiceReadAFK.setOnAction(event -> {
                poeChatTTS.setReadAFK(voiceReadAFK.isSelected());
                PropertyManager.getInstance().setProp(PropertyManager.VOICE_AFK, String.valueOf(voiceReadAFK
                        .isSelected()));
            });

            voiceReadChat.setSelected(poeChatTTS.isReadChatMessages());
            voiceReadChat.setOnAction(event -> {
                poeChatTTS.setReadChatMessages(voiceReadChat.isSelected());
                PropertyManager.getInstance().setProp(PropertyManager.VOICE_CHAT, String.valueOf(voiceReadChat
                        .isSelected()));
            });

            voiceReadCurOffers.setSelected(poeChatTTS.isReadCurrencyRequests());
            voiceReadCurOffers.setOnAction(event -> {
                poeChatTTS.setReadCurrencyRequests(voiceReadCurOffers.isSelected());
                PropertyManager.getInstance().setProp(PropertyManager.VOICE_CURRENCY, String.valueOf(voiceReadCurOffers
                        .isSelected()));
            });

            voiceReadTradeOffers.setSelected(poeChatTTS.isReadTradeRequests());
            voiceReadTradeOffers.setOnAction(event -> {
                poeChatTTS.setReadTradeRequests(voiceReadTradeOffers.isSelected());
                PropertyManager.getInstance().setProp(PropertyManager.VOICE_TRADE, String.valueOf(voiceReadTradeOffers.isSelected()));
            });

            voiceRandom.setSelected(poeChatTTS.isRandomizeMessages());
            voiceRandom.setOnAction(event -> {
                poeChatTTS.setRandomizeMessages(voiceRandom.isSelected());
                PropertyManager.getInstance().setProp(PropertyManager.VOICE_RANDOMIZE, String.valueOf
                        (voiceRandom.isSelected()));
            });

            volumeLabel.setTooltip(new Tooltip("Set volume of the voice speaker"));

            int volume = PropertyManager.getInstance().getVoiceVolume();
            volumeLabel.setText(volume + " %");
            volumeSlider.setValue(volume);
            volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> volumeLabel.setText(newValue.intValue() + " %"));
            volumeSlider.valueChangingProperty().addListener((observable, changeEnds, changeStarts) -> {
                if (changeEnds) {
                    int newVolume = (int) volumeSlider.getValue();
                    poeChatTTS.setVolume(newVolume);
                    PropertyManager.getInstance().setVoiceVolume(String.valueOf(newVolume));
                    poeChatTTS.testSpeech();
                }
            });

            // Who knew daniel was used in Pendulum's Bloodsugar?
            volumeTopicLabel.setOnMouseClicked(event -> {
                if (poeChatTTS.getVoice() != null && poeChatTTS.getVoice().contains("Daniel")) {
                    String msg;
                    volumeClicked++;
                    if (volumeClicked >= 5) {
                        msg = "Ok, Fuck it, I lied! It's Drum and Bass, what you gonna do!";
                    } else {
                        return;
                    }
                    try {
                        poeChatTTS.textToSpeech(msg, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            voiceTestButton.setOnAction(event -> poeChatTTS.randomTradeMessage());

            poePath.setText(PropertyManager.getInstance().getPathOfExilePath());
        }

        // Auto Update checkbox
        autoUpdate.setOnAction(event -> {
            tradeManager.setAutoUpdate(autoUpdate.isSelected());
            autoUpdate.setSelected(tradeManager.isAutoUpdating());
        });

        autoUpdate.setTooltip(new Tooltip("Automatically fetches all offers after the given time interval."));

        int updateDelay = PropertyManager.getInstance().getUpdateDelay();
        updateSlider.setValue(updateDelay);
        updateSlider.valueProperty().addListener((observable, oldValue, newValue) -> updateTime.setText(newValue.intValue() + " min"));
        updateSlider.valueChangingProperty().addListener((observable, changeEnds, changeStarts) -> {
            if (changeEnds) {
                int newUpdateDelay = (int) updateSlider.getValue();
                PropertyManager.getInstance().setUpdateDelay(newUpdateDelay);
            }
        });

        updateTime.setTooltip(new Tooltip("Set volume of the voice speaker"));
        updateTime.setText(updateDelay + " min");

        if (PropertyManager.getInstance().getProp("DEBUG", null) == null) {
            autoUpdate.setVisible(false);
            updateTime.setVisible(false);
            updateSlider.setVisible(false);
        }
    }

    private void setWebParser(String newValue) {
        if (newValue.equals(PoeTradeApiParser.IDENTIFIER)) {
            //pathofexile.com does this by default.
            filterInvalid.setSelected(true);
            filterWithoutAPI.setSelected(true);
            PropertyManager.getInstance().setFilterOutOfStock(false);
            PropertyManager.getInstance().setFilterNoApi(false);
            filterInvalid.setDisable(true);
            filterWithoutAPI.setDisable(true);
        } else {
            filterInvalid.setSelected(false);
            filterWithoutAPI.setSelected(false);
            filterInvalid.setDisable(false);
            filterWithoutAPI.setDisable(false);
        }
        tradeManager.setWebParser(newValue);
        resetUiItems();
    }

    private void calculateValue() {
        String inString = valueInputText.getText();

        boolean outputReversed = false;
        if (inString == null || inString.isEmpty()) {
            inString = valueOutputText.getText();
            outputReversed = true;
        }

        String result = "";
        if (inString != null && !inString.isEmpty()) {
            try {
                float inAmount = dFormat.parse(inString).floatValue();
                if (outputReversed) {
                    result = prettyFloat(inAmount * tradeManager.getCurrencyValue(valueOutputCB.getValue(), valueInputCB.getValue()), valueFormat);
                } else {
                    result = prettyFloat(inAmount * tradeManager.getCurrencyValue(valueInputCB.getValue(), valueOutputCB.getValue()), valueFormat);
                }
            } catch (ParseException ignored) {

            }
        }

        if (outputReversed) {
            valueInputText.setText(result);
        } else {
            valueOutputText.setText(result);
        }
    }

    private void setDisableVoiceControls() {
        String balconMissingText = "Place balcon.exe next to the app to enable this feature.";
//        voiceActive.setDisable(true);
        voiceActive.setTooltip(new Tooltip(balconMissingText));
        voiceReadTradeOffers.setDisable(true);
        voiceReadTradeOffers.setTooltip(new Tooltip(balconMissingText));
        voiceReadChat.setDisable(true);
        voiceReadChat.setTooltip(new Tooltip(balconMissingText));
        voiceReadCurOffers.setDisable(true);
        voiceReadCurOffers.setTooltip(new Tooltip(balconMissingText));
        volumeLabel.setDisable(true);
        volumeLabel.setTooltip(new Tooltip(balconMissingText));
        volumeSlider.setDisable(true);
        volumeSlider.setTooltip(new Tooltip(balconMissingText));
        voiceReadAFK.setDisable(true);
        voiceReadAFK.setTooltip(new Tooltip(balconMissingText));
        voiceRandom.setDisable(true);
        voiceRandom.setTooltip(new Tooltip(balconMissingText));
        voiceTestButton.setDisable(true);
        voiceTestButton.setTooltip(new Tooltip(balconMissingText));
        voiceShoutoutWords.setDisable(true);
        voiceExcludeWords.setDisable(true);
        voiceSpeakerCB.setDisable(true);

        poePath.setDisable(true);
    }

    private void updateTitle() {
        if (currentStage == null) {
            LogManager.getInstance().log(getClass(), "Error setting title.");
            return;
        }
        currentStage.setTitle(title + " - " + PropertyManager.getInstance().getCurrentLeague() + " League - " + PropertyManager.getInstance().getProp("trade_data_source"));
    }

    private long updateStart;

    @Override
    public void onUpdateStarted() {
        updateStart = System.currentTimeMillis();

        currencyList.setPlaceholder(new Label("Updating..."));
        playerDealList.setPlaceholder(new Label("Updating..."));

        updateButton.setText("Cancel");
//        updatePlayerButton.setText("Cancel");
    }

    @Override
    public void onUpdateFinished() {
        LogManager.getInstance().log(TradeManager.class, "Update took " + prettyFloat((System.currentTimeMillis() - updateStart) / 1000f) + " seconds");
    }

    private long parseStartTime;

    @Override
    public void onParsingStarted() {
        parseStartTime = System.currentTimeMillis();
    }

    @Override
    public void onError() {
        resetUI();
    }

    private void resetUI() {
        currencyList.setPlaceholder(new Label("No deals to show."));
        Label label = new Label("No player offers found.");
        if (PropertyManager.getInstance().getPlayerList().isEmpty()) {
            label = new Label("Add your poe in-game account name to the list in Settings -> General to evaluate your current offers.");
        }
        playerDealList.setPlaceholder(label);

        updateButton.setText("Update All");
        updateButton.setDisable(false);
//        updatePlayerButton.setText("Update Player");
//        updatePlayerButton.setDisable(false);

        currencyFilterChanged = false;
    }

    @Override
    public void onParsingFinished() {
        float time = System.currentTimeMillis() - parseStartTime;
        if (time > 1f) {
            LogManager.getInstance().log(TradeManager.class, "Parsing took " + prettyFloat(time) + " milliseconds");
        }

        resetUI();
    }

    @Override
    public void onRatesFetched() {
        valueTable.setItems(tradeManager.getCurrencyValues());
        TableColumn<Map.Entry<CurrencyID, Float>, ?> column = valueTable.getColumns().get(1);
        column.setSortType(TableColumn.SortType.DESCENDING);
        valueTable.getSortOrder().add(column);
        valueTable.refresh();

        calculateValue();
    }

    @Override
    public void onPropChanged(String key, String value) {
        if (PropertyManager.LEAGUE_KEY.equals(key)) {
            leagueCB.setValue(value);
            resetUiItems();
        }
    }

    private void resetUiItems() {
        if (currentStage != null) {
            valueTable.getItems().clear();
            buyOfferTable.getItems().clear();
            sellOfferTable.getItems().clear();
            playerDealList.getItems().clear();
            currencyList.getItems().clear();

            tradeManager.reset();

            updateTitle();
        }
    }

    // UTIL

    public static String prettyFloat(float in) {
        return prettyFloat(in, null);
    }

    public static String prettyFloat(float in, DecimalFormat format) {
        if (in == 0) {
            return "---";
        }
        if (format == null) {
            format = dFormat;
        }

        return format.format(in);
    }

    public static void setImage(String name, ImageView view) {
        URL url = Main.class.getResource(name);
        if (url == null) {
            url = Main.class.getResource("0.png");
        }

        try {
            final Image image = SwingFXUtils.toFXImage(ImageIO.read(url), null);
            Platform.runLater(() -> view.setImage(image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String join(Collection<?> col) {
        StringBuilder result = new StringBuilder();

        for (Iterator<?> var3 = col.iterator(); var3.hasNext(); result.append((String) var3.next())) {
            if (result.length() != 0) {
                result.append(",");
            }
        }

        return result.toString();
    }

    public static void openInBrowser(String query) {
        if(query == null || query.isEmpty() || query.equals("null")){
            LogManager.getInstance().log(Main.class, "Could not open browser. Query was null!");
            return;
        }
        URI uri = URI.create(PoeTradeApiParser.getSearchURL(query));

        try {
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
            LogManager.getInstance().log(PoeTradeWebParser.class, "Error opening browser. " + e);
        }
    }

    public static void openInBrowser(CurrencyOffer offer) {
        URI uri;
        if (offer.getQueryID() != null && !offer.getQueryID().isEmpty()) {
            openInBrowser(offer.getQueryID());
            return;
        } else {
            uri = URI.create(PoeTradeWebParser.getSearchURL(offer.getSellID(), offer.getBuyID()));
        }

        try {
            Desktop.getDesktop().browse(uri);
        } catch (Exception e) {
            LogManager.getInstance().log(PoeTradeWebParser.class, "Error opening browser. " + e);
        }
    }

    public static void openInBrowser(CurrencyID primary, CurrencyID secondary) {
        try {
            Desktop.getDesktop().browse(URI.create(PoeTradeWebParser.getSearchURL(primary, secondary)));
        } catch (Exception e) {
            LogManager.getInstance().log(PoeTradeWebParser.class, "Error opening browser. " + e);
        }
    }

}
