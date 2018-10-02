package de.crass.poetradehelper;/**
 * Created by mcrass on 19.07.2018.
 */

import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.ParseListener;
import de.crass.poetradehelper.parser.TradeManager;
import de.crass.poetradehelper.tts.PoeChatTTS;
import de.crass.poetradehelper.ui.MarketCell;
import de.crass.poetradehelper.ui.PlayerTradeCell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

public class Main extends Application implements ParseListener {

    public static final String title = "PoeTradeHelper";
    public static final String versionText = "v0.4-SNAPSHOT";

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

    private static Stage currentStage;

    private TradeManager tradeManager;

    private static PoeChatTTS poeChatTTS;

    private Timer updateTimer;
    private int updateDelay = 1000 * 60 * 5; // 5 minutes

    private boolean currencyFilterChanged = false;

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

        LogManager.getInstance().setConsole(console);

        tradeManager = TradeManager.getInstance();
        tradeManager.registerListener(this);

        poeChatTTS = new PoeChatTTS(new PoeChatTTS.Listener() {
            @Override
            public void onShutDown() {
                voiceActive.setSelected(false);
                poePath.setDisable(false);
            }
        });

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

        stopUpdateTimer();

        LogManager.getInstance().log(getClass(), "Shutdown complete.");
    }

    private void setupUI() {
        version.setText(versionText);

        currencyList.setEditable(false);
        currencyList.setCellFactory(new Callback<ListView<CurrencyDeal>, ListCell<CurrencyDeal>>() {
            @Override
            public ListCell<CurrencyDeal> call(ListView<CurrencyDeal> studentListView) {
                return new MarketCell<>();
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
                if(updateTimer != null){
                    stopUpdateTimer();
                    startUpdateTimer();
                }
                if (tradeManager.isUpdating()) {
                    tradeManager.cancelUpdate();
                    updateButton.setDisable(true);
//                    updatePlayerButton.setDisable(true);
                } else {
                    tradeManager.updateOffers(currencyFilterChanged);
//                    updatePlayerButton.setDisable(true);
                }
            }
        });

//        updatePlayerButton.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent event) {
//                if(updateTimer != null){
//                    stopUpdateTimer();
//                    startUpdateTimer();
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
                new Callback<TableColumn.CellDataFeatures<CurrencyOffer, Number>, ObservableValue<Number>>() {
                    @Override
                    public ObservableValue<Number> call(TableColumn.CellDataFeatures<CurrencyOffer, Number> param) {
                        int stock = param.getValue().getStock();
                        if(stock < 0){
                            return null;
                        }
                        return new SimpleFloatProperty(stock);
                    }
                };

        Callback<TableColumn.CellDataFeatures<CurrencyOffer, String>, ObservableValue<String>> playerCellFactory =
                new Callback<TableColumn.CellDataFeatures<CurrencyOffer, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(TableColumn.CellDataFeatures<CurrencyOffer, String> param) {
                        return new SimpleStringProperty(param.getValue().getPlayerName());
                    }
                };

        TableColumn<CurrencyOffer, Number> valueColumn = new TableColumn<>();
        valueColumn.setText("Amount");
        valueColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<CurrencyOffer, Number>, ObservableValue<Number>>() {
            @Override
            public ObservableValue<Number> call(TableColumn.CellDataFeatures<CurrencyOffer, Number> param) {
                return new SimpleFloatProperty(param.getValue().getBuyAmount());
            }
        });

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
        sellValueColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<CurrencyOffer, Number>, ObservableValue<Number>>() {
            @Override
            public ObservableValue<Number> call(TableColumn.CellDataFeatures<CurrencyOffer, Number> param) {
                return new SimpleFloatProperty(param.getValue().getSellAmount());
            }
        });

        TableColumn<CurrencyOffer, Number> sellStockColumn = new TableColumn<>();
        sellStockColumn.setText("Stock");
        sellStockColumn.setCellValueFactory(stockCellFactory);

        TableColumn<CurrencyOffer, String> sellPlayerColumn = new TableColumn<>();
        sellPlayerColumn.setText("Playername");
        sellPlayerColumn.setCellValueFactory(playerCellFactory);

        sellOfferTable.getColumns().clear();
        sellOfferTable.getColumns().addAll(sellValueColumn, sellStockColumn, sellPlayerColumn);

        offerSecondary.setItems(PropertyManager.getInstance().getFilterList());
        offerSecondary.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                CurrencyID newValue = offerSecondary.getValue();
                if(newValue != null) {
                    buyOfferTable.setItems(tradeManager.getBuyOffers(newValue));
                    sellOfferTable.setItems(tradeManager.getSellOffers(newValue));
                }
            }
        });

        // Value Tab
        valueTable.setRowFactory(tv -> new TableRow<Map.Entry<CurrencyID, Float>>() {
            @Override
            public void updateItem(Map.Entry<CurrencyID, Float> item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && PropertyManager.getInstance().getPrimaryCurrency().equals(item.getKey())) {
                    setStyle("-fx-background-color: -fx-accent;");
                } else {
                    setStyle("");
                }
            }
        });

        TableColumn<Map.Entry<CurrencyID, Float>, String> column = new TableColumn<>();
        column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<CurrencyID, Float>, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<Map.Entry<CurrencyID, Float>, String> param) {
                return new SimpleStringProperty(param.getValue().getKey().toString());
            }
        });

        TableColumn<Map.Entry<CurrencyID, Float>, Number> column2 = new TableColumn<>();
        column2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Map.Entry<CurrencyID, Float>, Number>, ObservableValue<Number>>() {
            @Override
            public ObservableValue<Number> call(TableColumn.CellDataFeatures<Map.Entry<CurrencyID, Float>, Number> param) {
                return new SimpleFloatProperty(param.getValue().getValue());
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

        //FIXME: Dont create new lists all the time... also refresh on league change
        valueTable.setItems(FXCollections.observableArrayList(tradeManager.getCurrencyValues().entrySet()));

        updateValuesButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                tradeManager.updateCurrencyValues();
                valueTable.setItems(FXCollections.observableArrayList(tradeManager.getCurrencyValues().entrySet()));
                valueTable.refresh();
            }
        });

        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.values());
        valueInputCB.setItems(currencyList);
        valueInputCB.setValue(CurrencyID.EXALTED);
        valueOutputCB.setItems(currencyList);
        valueOutputCB.setValue(CurrencyID.CHAOS);
        valueOutputCB.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                calculateValue();
            }
        });
        valueInputCB.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                calculateValue();
            }
        });

        valueInputText.setOnKeyTyped(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                // FIXME: Handle text input
//                calculateValue(event.getCharacter());
                valueOutputText.setText("");
            }
        });

        convertButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                calculateValue();
            }
        });

        calculateValue();

        // SETTINGS
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
                if (newCurrency != null) {
                    List<CurrencyID> filterList = PropertyManager.getInstance().getFilterList();
                    if (!filterList.contains(newCurrency)) {
                        currencyFilterChanged = true;
                        filterList.add(newCurrency);
                    }
                }
            }
        });

        removeCurrencyFilterBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                CurrencyID focus = currencyFilterList.getFocusModel().getFocusedItem();
                if (focus != null) {
                    currencyFilterChanged = true;
                    PropertyManager.getInstance().getFilterList().remove(focus);
                }
            }
        });

        restoreCurrencyFilterBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().resetFilterList();
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
                LogManager.getInstance().log(getClass(), "Setting " + leagueCB.getValue() + " as new league.");
                PropertyManager.getInstance().setLeague(leagueCB.getValue());
                tradeManager.updateCurrencyValues();
                updateTitle();
            }
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
            voiceActive.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (voiceActive.isSelected()) {
                        poeChatTTS.startTTS();
                        poePath.setDisable(true);
                    } else {
                        poeChatTTS.stopTTS();
                        poePath.setDisable(false);
                    }
                }
            });

            speakerCB.setItems(FXCollections.observableArrayList(supportedVoices));
            speakerCB.setValue(poeChatTTS.getVoice());
            speakerCB.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    String selected = speakerCB.getValue();
                    if (!selected.isEmpty()) {
                        poeChatTTS.setVoice(selected);
                        PropertyManager.getInstance().setProp(PropertyManager.VOICE_SPEAKER, selected);
                    }
                }
            });

            voiceReadAFK.setSelected(poeChatTTS.isReadAFK());
            voiceReadAFK.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    poeChatTTS.setReadAFK(voiceReadAFK.isSelected());
                    PropertyManager.getInstance().setProp(PropertyManager.VOICE_AFK, String.valueOf(voiceReadAFK
                            .isSelected()));
                }
            });

            voiceReadChat.setSelected(poeChatTTS.isReadChatMessages());
            voiceReadChat.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    poeChatTTS.setReadChatMessages(voiceReadChat.isSelected());
                    PropertyManager.getInstance().setProp(PropertyManager.VOICE_CHAT, String.valueOf(voiceReadChat
                            .isSelected()));
                }
            });

            voiceReadCurOffers.setSelected(poeChatTTS.isReadCurrencyRequests());
            voiceReadCurOffers.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    poeChatTTS.setReadCurrencyRequests(voiceReadCurOffers.isSelected());
                    PropertyManager.getInstance().setProp(PropertyManager.VOICE_CURRENCY, String.valueOf(voiceReadCurOffers
                            .isSelected()));
                }
            });

            voiceReadTradeOffers.setSelected(poeChatTTS.isReadTradeRequests());
            voiceReadTradeOffers.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    poeChatTTS.setReadTradeRequests(voiceReadTradeOffers.isSelected());
                    PropertyManager.getInstance().setProp(PropertyManager.VOICE_TRADE, String.valueOf(voiceReadTradeOffers.isSelected()));
                }
            });

            voiceRandom.setSelected(poeChatTTS.isRandomizeMessages());
            voiceRandom.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    poeChatTTS.setRandomizeMessages(voiceRandom.isSelected());
                    PropertyManager.getInstance().setProp(PropertyManager.VOICE_RANDOMIZE, String.valueOf
                            (voiceRandom.isSelected()));
                }
            });

            int volume = PropertyManager.getInstance().getVoiceVolume();
            volumeSlider.setValue(volume);
            volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    volumeLabel.setText(String.valueOf(newValue.intValue()));
                }
            });
            volumeSlider.valueChangingProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean changeEnds, Boolean changeStarts) {
                    if (changeEnds) {
                        int newVolume = (int) volumeSlider.getValue();
                        poeChatTTS.setVolume(newVolume);
                        PropertyManager.getInstance().setVoiceVolume(String.valueOf(newVolume));
                        poeChatTTS.testSpeech();
                    }
                }
            });

            volumeLabel.setText(String.valueOf(volume));

            poePath.setText(PropertyManager.getInstance().getPathOfExilePath());
            poePath.setOnKeyTyped(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    String newPath = poePath.getText();
                    poeChatTTS.setPath(newPath);
                    PropertyManager.getInstance().setPathOfExilePath(newPath);
                }
            });

            // Auto Update checkbox
            autoUpdate.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if(autoUpdate.isSelected()) {
                        startUpdateTimer();
                    } else{
                        stopUpdateTimer();
                    }
                }
            });

            if(PropertyManager.getInstance().getProp("DEBUG", null) == null){
                autoUpdate.setDisable(true);
            }
        }
    }

    private void calculateValue(){
        calculateValue(null);
    }

    private void calculateValue(String newText) {
        String inString = valueInputText.getText();
        if(newText != null){
            inString += newText;
        }

        String result = "0";
        if (inString != null && !inString.isEmpty()) {
            try {
                float inAmount = Float.parseFloat(inString);
                result = prettyFloat(inAmount * tradeManager.getCurrencyValue(valueInputCB.getValue(), valueOutputCB.getValue()));
            } catch (NumberFormatException ignored) {

            }
        }
        valueOutputText.setText(result);
    }

    private void setDisableVoiceControls() {
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
    }

    public static String prettyFloat(float in) {
        if (in == 0) {
            return "---";
        }
        DecimalFormat df = new DecimalFormat("0.##");
        df.setRoundingMode(RoundingMode.HALF_UP);
        return String.valueOf(df.format(in));
    }

    public static void setImage(String name, ImageView view) {
        try {
            final Image image = SwingFXUtils.toFXImage(ImageIO.read(Main.class.getResource(name)), null);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    view.setImage(image);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String join(Collection col, String seperator) {
        StringBuilder result = new StringBuilder();

        for (Iterator var3 = col.iterator(); var3.hasNext(); result.append((String) var3.next())) {
            if (result.length() != 0) {
                result.append(seperator);
            }
        }

        return result.toString();
    }

    TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            LogManager.getInstance().log(getClass(), "Invoke Automatic Update");
            TradeManager.getInstance().updateOffers(currencyFilterChanged);
        }
};

    //FIXME: Timer leaks thread
    private void startUpdateTimer() {
        if (updateTimer == null) {
            updateTimer = new Timer("poeTradeHelperUpdateTimer");
            updateTimer.scheduleAtFixedRate(updateTask, updateDelay, updateDelay);
        }
    }

    private void stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer.purge();
            updateTimer = null;
            updateTask.cancel();
        }
    }
}
