package de.crass.poetradehelper;
/*
  Created by mcrass on 19.07.2018.
 */

import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.ParseListener;
import de.crass.poetradehelper.parser.PoeNinjaParser;
import de.crass.poetradehelper.parser.TradeManager;
import de.crass.poetradehelper.tts.PoeChatTTS;
import de.crass.poetradehelper.ui.CurrencyContextMenu;
import de.crass.poetradehelper.ui.MarketCell;
import de.crass.poetradehelper.ui.PlayerTradeCell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class Main extends Application implements ParseListener, PoeNinjaParser.PoeNinjaListener {

    private static final String title = "PoeTradeHelper";
    private static final String versionText = "v0.5-SNAPSHOT";

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
    private ComboBox<String> speakerCB;

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
    private CheckBox autoUpdate;

    @FXML
    private TableView<CurrencyOffer> buyOfferTable;

    @FXML
    private TableView<CurrencyOffer> sellOfferTable;

    @FXML
    private ComboBox<CurrencyID> offerSecondary;

    @FXML
    private Button refreshBtn;

    private static Stage currentStage;
    private static PoeChatTTS poeChatTTS;
    private static ScheduledExecutorService autoUpdateExecutor;

    private TradeManager tradeManager;

    private boolean currencyFilterChanged = false;

    private static DecimalFormat dFormat = new DecimalFormat("0.##");
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
        assert console != null : "fx:id=\"console\" was not injected: check your FXML file 'layout.fxml'.";
        assert currencyList != null : "fx:id=\"currencyList\" was not injected: check your FXML file 'layout.fxml'.";
        assert updateButton != null : "fx:id=\"updateButton\" was not injected: check your FXML file 'layout.fxml'.";
        assert version != null : "fx:id=\"version\" was not injected: check your FXML file 'layout.fxml'.";

        // Setup console
        LogManager.getInstance().setConsole(console);

        // Setup trade manager
        tradeManager = TradeManager.getInstance();
        tradeManager.registerListener(this, this);

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

        stopUpdateTask();

        LogManager.getInstance().log(getClass(), "Shutdown complete.");
    }

    private int versionClicked = 0;

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
                JOptionPane.showMessageDialog(null, "PoE Ninja will hate me for this...");
                autoUpdate.setVisible(true);
            }
        });

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
            if (autoUpdateExecutor != null) {
                stopUpdateTask();
            }
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
        Callback<TableColumn.CellDataFeatures<CurrencyOffer, Number>, ObservableValue<Number>> stockCellFactory =
                param -> {
                    int stock = param.getValue().getStock();
                    if (stock < 0) {
                        return null;
                    }
                    return new SimpleFloatProperty(stock);
                };

        Callback<TableColumn.CellDataFeatures<CurrencyOffer, String>, ObservableValue<String>> playerCellFactory =
                param -> new SimpleStringProperty(param.getValue().getPlayerName());

        TableColumn<CurrencyOffer, Number> valueColumn = new TableColumn<>();
        valueColumn.setText("Amount");
        valueColumn.setCellValueFactory(param -> new SimpleFloatProperty(param.getValue().getBuyAmount() / param.getValue().getSellAmount()));

        TableColumn<CurrencyOffer, Number> stockColumn = new TableColumn<>();
        stockColumn.setText("Stock");
        stockColumn.setCellValueFactory(stockCellFactory);

        TableColumn<CurrencyOffer, String> playerColumn = new TableColumn<>();
        playerColumn.setText("Playername");
        playerColumn.setCellValueFactory(playerCellFactory);

        buyOfferTable.getColumns().clear();
        buyOfferTable.getColumns().addAll(valueColumn, stockColumn, playerColumn);

        // Sell table
        TableColumn<CurrencyOffer, Number> sellValueColumn = new TableColumn<>();
        sellValueColumn.setText("Amount");
        sellValueColumn.setCellValueFactory(param -> new SimpleFloatProperty(param.getValue().getSellAmount() / param.getValue().getBuyAmount()));

        TableColumn<CurrencyOffer, Number> sellStockColumn = new TableColumn<>();
        sellStockColumn.setText("Stock");
        sellStockColumn.setCellValueFactory(stockCellFactory);

        TableColumn<CurrencyOffer, String> sellPlayerColumn = new TableColumn<>();
        sellPlayerColumn.setText("Playername");
        sellPlayerColumn.setCellValueFactory(playerCellFactory);

        sellOfferTable.getColumns().clear();
        sellOfferTable.getColumns().addAll(sellValueColumn, sellStockColumn, sellPlayerColumn);

        offerSecondary.setItems(PropertyManager.getInstance().getFilterList());
        offerSecondary.setOnAction(event -> {
            CurrencyID newValue = offerSecondary.getValue();
            if (newValue != null) {
                buyOfferTable.setItems(tradeManager.getBuyOffers(newValue));
                sellOfferTable.setItems(tradeManager.getSellOffers(newValue));
            }
        });

        refreshBtn.setOnAction(event -> tradeManager.updateOffersForCurrency(offerSecondary.getValue()));

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
                    setText(prettyFloat(number.floatValue(), valueFormat));
                }
            }
        });

        column.setText("Currency");
        column.setPrefWidth(100);
        column.setMaxWidth(100);
        column2.setText("Value in C");
        column2.setPrefWidth(100);
        column2.setMaxWidth(100);
        column2.setStyle("-fx-alignment: CENTER-RIGHT;");

        valueTable.getColumns().clear();
        valueTable.getColumns().addAll(column, column2);

        updateValuesButton.setOnAction(event -> tradeManager.updateCurrencyValues());

        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.values());
        valueInputCB.setItems(currencyList);
        valueInputCB.setValue(CurrencyID.EXALTED);
        valueOutputCB.setItems(currencyList);
        valueOutputCB.setValue(CurrencyID.CHAOS);
        valueOutputCB.setOnAction(event -> calculateValue());
        valueInputCB.setOnAction(event -> calculateValue());

        valueInputText.setOnKeyTyped(event -> {
//                calculateValue(event.getCharacter());
            valueOutputText.setText("");
        });

        valueOutputText.setOnKeyTyped(event -> valueInputText.setText(""));

        convertButton.setOnAction(event -> calculateValue());

        // SETTINGS
        primaryComboBox.setTooltip(new Tooltip("Select currency to flip with"));
        primaryComboBox.setItems(currencyList);
        primaryComboBox.setValue(PropertyManager.getInstance().getPrimaryCurrency());
        primaryComboBox.setOnAction(event -> {
            CurrencyID newValue = primaryComboBox.getValue();
            PropertyManager.getInstance().setPrimaryCurrency(newValue);
            tradeManager.parseDeals();
            valueTable.refresh();
        });

        filterInvalid.setTooltip(new Tooltip("Ignore all offers that do have a stock value but not enough on stock to sell"));
        filterInvalid.setOnAction(event -> {
            PropertyManager.getInstance().setFilterOutOfStock(filterInvalid.isSelected());
            tradeManager.parseDeals();
        });
        filterInvalid.setSelected(PropertyManager.getInstance().getFilterOutOfStock());

        filterWithoutAPI.setTooltip(new Tooltip("Ignore all offers that don't have stock information"));
        filterWithoutAPI.setOnAction(event -> {
            PropertyManager.getInstance().setFilterNoApi(filterWithoutAPI.isSelected());
            tradeManager.parseDeals();
        });
        filterWithoutAPI.setSelected(PropertyManager.getInstance().getFilterNoApi());

        filterExcessive.setTooltip(new Tooltip("Ignore all offers that have an insane buy to sell value ratio"));
        filterExcessive.setOnAction(event -> {
            PropertyManager.getInstance().setFilterExcessive(filterExcessive.isSelected());
            tradeManager.parseDeals();
        });
        filterExcessive.setSelected(PropertyManager.getInstance().getFilterExcessive());

        currencyFilterList.setItems(PropertyManager.getInstance().getFilterList());
        currencyFilterList.setTooltip(new Tooltip("Only offers for currency in this list will be fetched on update"));

        currencyFilterCB.setItems(FXCollections.observableArrayList(CurrencyID.values()));

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
        playerListView.setTooltip(new Tooltip("Offers from players in this list will be shown in PlayerOverview"));

        addPlayerButton.setTooltip(new Tooltip("Add player from TextField to list"));
        addPlayerButton.setOnAction(event -> {
            String newPlayer = playerField.getText();
            if (!newPlayer.isEmpty() && !playerList.contains(newPlayer))
                playerList.add(newPlayer);
        });

        removePlayerBtn.setTooltip(new Tooltip("Remove selected player from list"));
        removePlayerBtn.setOnAction(event -> playerList.remove(playerListView.getFocusModel().getFocusedItem()));

        leagueCB.setTooltip(new Tooltip("Set Path of Exile league"));
        leagueCB.setItems(tradeManager.getLeagueList());
        leagueCB.setValue(PropertyManager.getInstance().getCurrentLeague());

        leagueCB.setOnAction(event -> {
            PropertyManager.getInstance().setLeague(leagueCB.getValue());
            tradeManager.updateCurrencyValues();
            updateTitle();
        });


        // Setup Voice Controls
        List<String> supportedVoices = poeChatTTS.getSupportedVoices();
        if (supportedVoices == null) {
            LogManager.getInstance().log(getClass(), "TTS is disabled: Balcon not found.");
            setDisableVoiceControls();
        } else if (supportedVoices.isEmpty()) {
            LogManager.getInstance().log(getClass(), "TTS is disabled: No supported voices found.");
            setDisableVoiceControls();
        } else {
            voiceActive.setOnAction(event -> {
                if (voiceActive.isSelected()) {
                    poeChatTTS.startTTS();
                    poePath.setDisable(true);
                } else {
                    poeChatTTS.stopTTS();
                    poePath.setDisable(false);
                }
            });

            speakerCB.setItems(FXCollections.observableArrayList(supportedVoices));
            speakerCB.setValue(poeChatTTS.getVoice());
            speakerCB.setOnAction(event -> {
                String selected = speakerCB.getValue();
                if (!selected.isEmpty()) {
                    poeChatTTS.setVoice(selected);
                    PropertyManager.getInstance().setProp(PropertyManager.VOICE_SPEAKER, selected);
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

            int volume = PropertyManager.getInstance().getVoiceVolume();
            volumeSlider.setValue(volume);
            volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> volumeLabel.setText(String.valueOf(newValue.intValue())));
            volumeSlider.valueChangingProperty().addListener((observable, changeEnds, changeStarts) -> {
                if (changeEnds) {
                    int newVolume = (int) volumeSlider.getValue();
                    poeChatTTS.setVolume(newVolume);
                    PropertyManager.getInstance().setVoiceVolume(String.valueOf(newVolume));
                    poeChatTTS.testSpeech();
                }
            });

            volumeLabel.setTooltip(new Tooltip("Set volume of the voice speaker"));
            volumeLabel.setText(String.valueOf(volume));

            poePath.setText(PropertyManager.getInstance().getPathOfExilePath());
            poePath.setOnKeyTyped(event -> {
                String newPath = poePath.getText();
                poeChatTTS.setPath(newPath);
                PropertyManager.getInstance().setPathOfExilePath(newPath);
            });
        }

        // Auto Update checkbox
        autoUpdate.setOnAction(event -> {
            if (autoUpdate.isSelected()) {
                startUpdateTask();
            } else {
                stopUpdateTask();
            }
        });

        autoUpdate.setTooltip(new Tooltip("Invoke Update every " + PropertyManager.getInstance().getUpdateDelay() + " minutes."));

        if (PropertyManager.getInstance().getProp("DEBUG", null) != null) {
            autoUpdate.setVisible(false);
        }
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
        voiceActive.setTooltip(new Tooltip("Place balcon.exe next to the app to use this feature."));
        voiceActive.setDisable(true);
        voiceReadTradeOffers.setDisable(true);
        voiceReadChat.setDisable(true);
        voiceReadCurOffers.setDisable(true);
        volumeLabel.setDisable(true);
        volumeSlider.setDisable(true);
        voiceReadAFK.setDisable(true);
        voiceRandom.setDisable(true);
        poePath.setDisable(true);
    }

    private void updateTitle() {
        if (currentStage == null) {
            LogManager.getInstance().log(getClass(), "Error setting title.");
            return;
        }
        currentStage.setTitle(title + " - " + PropertyManager.getInstance().getCurrentLeague() + " League");
    }

    @Override
    public void onParsingStarted() {
        currencyList.setPlaceholder(new Label("Updating..."));
        playerDealList.setPlaceholder(new Label("Updating..."));

        updateButton.setText("Cancel");
//        updatePlayerButton.setText("Cancel");
        if (autoUpdate.isSelected()) {
            stopUpdateTask();
        }
    }

    @Override
    public void onParsingFinished() {
        currencyList.setPlaceholder(new Label("No deals to show."));
        playerDealList.setPlaceholder(new Label("No deals to show. Is your player set in settings?"));

        updateButton.setText("Update All");
        updateButton.setDisable(false);
//        updatePlayerButton.setText("Update Player");
//        updatePlayerButton.setDisable(false);

        currencyFilterChanged = false;

        if (autoUpdate.isSelected()) {
            startUpdateTask();
        }
    }

    @Override
    public void onRatesFetched() {
        valueTable.setItems(tradeManager.getCurrencyValues());
        TableColumn column = valueTable.getColumns().get(1);
        column.setSortType(TableColumn.SortType.DESCENDING);
        valueTable.getSortOrder().add(column);
        valueTable.refresh();

        calculateValue();
    }

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

        return String.valueOf(format.format(in));
    }

    public static void setImage(String name, ImageView view) {
        try {
            final Image image = SwingFXUtils.toFXImage(ImageIO.read(Main.class.getResource(name)), null);
            Platform.runLater(() -> view.setImage(image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String join(Collection col) {
        StringBuilder result = new StringBuilder();

        for (Iterator var3 = col.iterator(); var3.hasNext(); result.append((String) var3.next())) {
            if (result.length() != 0) {
                result.append(",");
            }
        }

        return result.toString();
    }

    private void startUpdateTask() {
        if (autoUpdateExecutor == null) {
            autoUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        int updateDelay = PropertyManager.getInstance().getUpdateDelay() * 60;
        autoUpdateExecutor.schedule(() -> {
            LogManager.getInstance().log("AutoUpdate", "Invoke Automatic Update");
            TradeManager.getInstance().updateOffers(currencyFilterChanged, false);
        }, updateDelay, TimeUnit.SECONDS);
    }

    private void stopUpdateTask() {
        if (autoUpdateExecutor != null) {
            autoUpdateExecutor.shutdownNow();
            autoUpdateExecutor = null;
        }
    }
}
