package de.crass.poetradehelper;
/*
  Created by mcrass on 19.07.2018.
 */

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.PoeNinjaParser;
import de.crass.poetradehelper.parser.PoeTradeApiParser;
import de.crass.poetradehelper.parser.PoeTradeWebParser;
import de.crass.poetradehelper.parser.TradeManager;
import de.crass.poetradehelper.tts.PoeLogReader;
import de.crass.poetradehelper.ui.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
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
    private static final String versionText = "v0.9-SNAPSHOT";

    @FXML
    private SplitPane splitPane;
//    private static SplitPane splitPaneStatic;

    @FXML
    private TabPane tabPane;
    public static TabPane tabPaneStatic;

    @FXML
    private Accordion settingsPane;

    @FXML
    private ListView<CurrencyDeal> playerDealList;

    @FXML
    private TextArea console;

    @FXML
    private Button removeCurrencyFilterBtn;

    @FXML
    private Button reloadConfigBtn;

    @FXML
    private Button openConfigBtn;

    @FXML
    private Button openTtsConfigBtn;

    @FXML
    private ListView<CurrencyDeal> currencyList;

    @FXML
    private CheckBox filterWithoutStockInfo;

    @FXML
    private CheckBox filterExcessive;

    @FXML
    private AnchorPane voiceLayout;

    @FXML
    private CheckBox voiceActive;

    @FXML
    private CheckBox parseActive;

    @FXML
    private CheckBox overlayIn;

    @FXML
    private CheckBox overlayOut;

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
    private Button resetWindowBtn;

    @FXML
    private Slider volumeSlider;

    @FXML
    private Label volumeLabel;

    @FXML
    private TextField playerField;

    @FXML
    private Button updateButton;

    @FXML
    private CheckBox filterOutOfStock;

    @FXML
    private ListView<String> playerListView;

    @FXML
    private Button addCurrencyFilterBtn;

    @FXML
    private Button restoreCurrencyFilterBtn;

    @FXML
    private TextField poePath;

    @FXML
    private TableView<Map.Entry<CurrencyID, Float>> valueTable;

    @FXML
    private Button updateValuesButton;

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
    private AnchorPane autoUpdateLayout;

    @FXML
    private CheckBox autoUpdate;

    @FXML
    private CheckBox autoUpdatePlayerOffers;

    @FXML
    private TableView<CurrencyOffer> buyOfferTable;

    @FXML
    private TableView<CurrencyOffer> sellOfferTable;

    @FXML
    private ComboBox<CurrencyID> offerSecondary;
    public static ComboBox<CurrencyID> offerSecondaryStatic;

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
    private Button parseTestButton;

    @FXML
    private Button parseOutTestButton;

    @FXML
    private Text volumeTopicLabel;

    @FXML
    private Label excessiveTresholdLabel;

    @FXML
    private Slider excessiveTresholdSlider;

    @FXML
    private CheckBox filterMultiTrade;

    private static Stage currentStage;
    public static PoeLogReader poeLogReader;

    private TradeManager tradeManager;

    public static boolean currencyFilterChanged = false;
    public static Tab lastTab;

    private static DecimalFormat dFormat = new DecimalFormat("#0.##");
    private static DecimalFormat valueFormat;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("stylesheet.css").toExternalForm());

        ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) ->
                PropertyManager.getInstance().setWindowSize(primaryStage.getWidth(), primaryStage.getHeight());

        primaryStage.widthProperty().addListener(stageSizeListener);
        primaryStage.heightProperty().addListener(stageSizeListener);

        primaryStage.setMinWidth(650);
        primaryStage.setMinHeight(300);
        primaryStage.setWidth(PropertyManager.getInstance().getWindowWidth());
        primaryStage.setHeight(PropertyManager.getInstance().getWindowHeight());
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("icon.png")));
        primaryStage.show();

        currentStage = primaryStage;

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (lastTab != null && event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.DEAD_CIRCUMFLEX) {
                    tabPaneStatic.getSelectionModel().select(lastTab);
                }
            }
        });

        updateTitle();
    }

    @FXML
    void initialize() {
        // Setup console
        LogManager.getInstance().setConsole(console);

        // Setup trade manager
        tradeManager = TradeManager.getInstance();
        tradeManager.registerListener(this, this);

        // Prop man
        PropertyManager.getInstance().setUICallback(this);
        setWebParser(PropertyManager.getInstance().getCurrentWebParser());

        // Setup TTS
        poeLogReader = new PoeLogReader(new PoeLogReader.Listener() {
            @Override
            public void onStarted() {
                poePath.setDisable(true);
            }

            @Override
            public void onShutDown() {
                parseActive.setSelected(false);
                poePath.setDisable(false);
            }
        });
        if (PropertyManager.getInstance().getBooleanProp("do_log_parsing", false)) {
            poeLogReader.startLogParsing();
        }

        // Setup UI
        setupUI();

        LogManager.getInstance().log(getClass(), "Started");
    }

    @Override
    public void stop() {
        LogManager.getInstance().log(getClass(), "Shutting down app.");
        OverlayManager.getInstance().shutdown();

//        PropertyManager.getInstance().setProp("window_divider", String.valueOf(splitPaneStatic.getDividerPositions()[0]));
        PropertyManager.getInstance().storeProperties();

        if (poeLogReader != null) {
            poeLogReader.shutdown();
            poeLogReader = null;
        }

        if (TradeManager.getInstance() != null) {
            TradeManager.getInstance().release();
        }

        LogManager.getInstance().log(getClass(), "Shutdown complete.");
    }

    private int versionClicked = 0;
    private int volumeClicked = 0;

    private void setupUI() {
        // Attempt on persisting divide position failed, position changes unpredictably during runtime
//        splitPane.setDividerPosition(0, Double.parseDouble(PropertyManager.getInstance().getProp("window_divider", "0.825")));
        // For 6 items and 725 height use 0.86415
        splitPane.setDividerPosition(0, 0.8615);
//        splitPaneStatic = splitPane;

        tabPaneStatic = tabPane;
        tabPane.setOnScroll(event -> {
            if (event.isShiftDown() || event.isControlDown()) {
                if (tabPane != null && tabPane.getSelectionModel() != null) {
                    double deltaY = event.getDeltaY();
                    if (deltaY > 0) {
                        tabPane.getSelectionModel().selectNext();
                    } else {
                        tabPane.getSelectionModel().selectPrevious();
                    }
                }
            }
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Tab>() {
                    @Override
                    public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
                        lastTab = t;
                    }
                }
        );

        settingsPane.setExpandedPane(settingsPane.getPanes().get(0));

        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        dFormat.setDecimalFormatSymbols(symbols);
        dFormat.setRoundingMode(RoundingMode.HALF_UP);

        valueFormat = (DecimalFormat) dFormat.clone();
        valueFormat.applyPattern("0.00");

        version.setText(versionText);
        version.setOnMouseClicked(event -> {
            versionClicked++;
            if (versionClicked == 10) {
                if (!PropertyManager.getInstance().getBooleanProp("auto_update_enabled", false)) {
                    PropertyManager.getInstance().setProp("auto_update_enabled", "true");
                    JOptionPane.showMessageDialog(null, "Ah yes, I see you are worthy. Please don't abuse your power.\n\nAutomatic Update setting unlocked!");
                    autoUpdateLayout.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Very nice clicking, but Automatic Update settings are already unlocked.");
                }
            } else if (versionClicked % 10 == 0) {
                JOptionPane.showMessageDialog(null, "Ok. You can stop now, there is nothing more to achieve here...");
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
                updateButton.setText("Cancel...");
            } else {
                tradeManager.updateOffers(false);
            }
        });

        // Offer tab
        ObservableList<CurrencyID> currencies = FXCollections.observableArrayList(CurrencyID.getValues());
        currencies.sort(Comparator.comparing(Object::toString));
        offerSecondaryStatic = offerSecondary;
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
        valueColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<CurrencyOffer, String> buyPercentageColumn = new TableColumn<>();
        buyPercentageColumn.setText("%");
        buyPercentageColumn.setCellValueFactory(param -> {
            float amount = param.getValue().getBuyAmount() / param.getValue().getSellAmount();
            String percentage = tradeManager.getCurrencyValuePercentage(amount, PropertyManager.getInstance().getPrimaryCurrency(), param.getValue().getBuyID());
            return new SimpleStringProperty(percentage);
        });
        buyPercentageColumn.setPrefWidth(50);
        buyPercentageColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<CurrencyOffer, Number> stockColumn = new TableColumn<>();
        stockColumn.setText("Stock");
        stockColumn.setCellValueFactory(stockCellFactory);
        stockColumn.setPrefWidth(50);
        stockColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<CurrencyOffer, String> playerColumn = new TableColumn<>();
        playerColumn.setText("Character");
        playerColumn.setCellValueFactory(playerCellFactory);
        playerColumn.setPrefWidth(140);

        buyOfferTable.getColumns().clear();
        buyOfferTable.getColumns().addAll(valueColumn, buyPercentageColumn, stockColumn, playerColumn);

        buyOfferTable.setContextMenu(new OfferContextMenu(buyOfferTable, offerSecondary));

        final PseudoClass whisperedClass = PseudoClass.getPseudoClass("whispered");
        final PseudoClass afkClass = PseudoClass.getPseudoClass("afk");
        final PseudoClass ignoredClass = PseudoClass.getPseudoClass("ignored");
        final PseudoClass playerClass = PseudoClass.getPseudoClass("player");
        Callback<TableView<CurrencyOffer>, TableRow<CurrencyOffer>> offerRowFactory =
                new Callback<TableView<CurrencyOffer>, TableRow<CurrencyOffer>>() {
                    @Override
                    public TableRow<CurrencyOffer> call(TableView<CurrencyOffer> row) {
                        return new TableRow<CurrencyOffer>() {
                            @Override
                            public void updateItem(CurrencyOffer item, boolean empty) {
                                super.updateItem(item, empty);

                                if (item != null && item.getTimesWhispered() > 0) {
                                    pseudoClassStateChanged(whisperedClass, true);
                                } else {
                                    pseudoClassStateChanged(whisperedClass, false);
                                }

                                if (item != null && item.getAfk()) {
                                    pseudoClassStateChanged(afkClass, true);
                                } else {
                                    pseudoClassStateChanged(afkClass, false);
                                }

                                if (item != null && item.isIgnored()) {
                                    pseudoClassStateChanged(ignoredClass, true);
                                } else if (item != null && item.isPlayerOffer()) {
                                    pseudoClassStateChanged(playerClass, true);
                                } else {
                                    pseudoClassStateChanged(ignoredClass, false);
                                    pseudoClassStateChanged(playerClass, false);
                                }
                            }
                        };
                    }
                };

        buyOfferTable.setRowFactory(offerRowFactory);

        // Sell table
        TableColumn<CurrencyOffer, String> sellValueColumn = new TableColumn<>();
        sellValueColumn.setText("Price");
        sellValueColumn.setCellValueFactory(param -> new SimpleStringProperty(prettyFloat(param.getValue().getSellAmount() / param.getValue().getBuyAmount())));
        sellValueColumn.setPrefWidth(50);
        sellValueColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<CurrencyOffer, String> sellPercentageColumn = new TableColumn<>();
        sellPercentageColumn.setText("%");
        sellPercentageColumn.setCellValueFactory(param -> {
            float amount = param.getValue().getSellAmount() / param.getValue().getBuyAmount();
            String percentage = tradeManager.getCurrencyValuePercentage(amount, PropertyManager.getInstance().getPrimaryCurrency(), param.getValue().getSellID());
            return new SimpleStringProperty(percentage);
        });
        sellPercentageColumn.setPrefWidth(50);
        sellPercentageColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<CurrencyOffer, Number> sellStockColumn = new TableColumn<>();
        sellStockColumn.setText("Stock");
        sellStockColumn.setCellValueFactory(stockCellFactory);
        sellStockColumn.setPrefWidth(50);
        sellStockColumn.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<CurrencyOffer, String> sellPlayerColumn = new TableColumn<>();
        sellPlayerColumn.setText("Character");
        sellPlayerColumn.setCellValueFactory(playerCellFactory);
        sellPlayerColumn.setPrefWidth(140);

        sellOfferTable.getColumns().clear();
        sellOfferTable.getColumns().addAll(sellValueColumn, sellPercentageColumn, sellStockColumn, sellPlayerColumn);
        sellOfferTable.setContextMenu(new OfferContextMenu(sellOfferTable, offerSecondary));

        sellOfferTable.setRowFactory(offerRowFactory);

        refreshBtn.setOnAction(event -> {
            CurrencyID newValue = offerSecondary.getValue();
            if (newValue != null) {
                tradeManager.updateOffersForCurrency(newValue, true);
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
                    setText(prettyFloat(number.floatValue(), false) + "c");
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
        valueInputCB.setValue(CurrencyID.EXALTED);
        valueOutputCB.setItems(currencies);
        valueOutputCB.setValue(CurrencyID.CHAOS);
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
            buyOfferTable.setItems(null);
            sellOfferTable.setItems(null);
        });

        filterOutOfStock.setSelected(PropertyManager.getInstance().getFilterOutOfStock());
        filterOutOfStock.setTooltip(new Tooltip("Ignore all offers that do have a stock value but not enough on stock to sell"));
        filterOutOfStock.setOnAction(event -> {
            PropertyManager.getInstance().setFilterOutOfStock(filterOutOfStock.isSelected());
            tradeManager.parseDeals();
        });

        filterWithoutStockInfo.setSelected(PropertyManager.getInstance().getFilterNoStockInfo());
        filterWithoutStockInfo.setTooltip(new Tooltip("Ignore all offers that don't have stock information"));
        filterWithoutStockInfo.setOnAction(event -> {
            PropertyManager.getInstance().setFilterNoStockInfo(filterWithoutStockInfo.isSelected());
            tradeManager.parseDeals();
        });

        filterExcessive.setSelected(PropertyManager.getInstance().getFilterExcessive());
        filterExcessive.setTooltip(new Tooltip("Ignore all offers that have an insane buy to sell value ratio"));
        filterExcessive.setOnAction(event -> {
            PropertyManager.getInstance().setFilterExcessive(filterExcessive.isSelected());
            tradeManager.parseDeals();
        });

//        excessiveTresholdSlider.setTooltip(new Tooltip("If \"Filter Excessive\" is active, all deals where the buy and sell value difference exceeds a given percentage of the higher value are ignored.\nE.g. 50% means all offers will be ignored where the buy and sell difference is more than 50% of the primary currency value"));
        excessiveTresholdSlider.setTooltip(new Tooltip("If \"Filter Excessive\" is active, offers with a buy to sell value difference more than % of the primary currency value will be ignored.\n\n100% - No Filtering, 0% - Only show 1-1 deals"));
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

        removePlayerBtn.setTooltip(new Tooltip("Reset Window to default size"));
        resetWindowBtn.setOnAction(event -> {
            currentStage.setWidth(PropertyManager.defaultWindowWidth);
            currentStage.setHeight(PropertyManager.defaultWindowHeight);
            splitPane.setDividerPosition(0, 0.8615);
            OverlayManager.getInstance().setOverlayLocation(new Point(-1, -1));
        });

        webParsingCB.setItems(FXCollections.observableArrayList(PoeTradeWebParser.IDENTIFIER, PoeTradeApiParser.IDENTIFIER));
        String defaultParser = PropertyManager.getInstance().getCurrentWebParser();
        if (defaultParser.equals(PoeTradeApiParser.IDENTIFIER)) {
            filterWithoutStockInfo.setSelected(true);
            filterOutOfStock.setSelected(true);
        }
        webParsingCB.setValue(defaultParser);
        webParsingCB.setOnAction(event -> setWebParser(webParsingCB.getValue()));

        // Setup parse
        parseActive.setOnAction(event -> {
            if (parseActive.isSelected()) {
                String newPath = poePath.getText();
                PropertyManager.getInstance().setPathOfExilePath(newPath);
                poeLogReader.startLogParsing();
            } else {
                poeLogReader.stopLogParsing();
            }
        });
        parseActive.setSelected(PropertyManager.getInstance().getBooleanProp("do_log_parsing", false));

        overlayIn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().setProp(PropertyManager.NOTIFY_INCOMING, String.valueOf(overlayIn.isSelected()));
            }
        });
        overlayIn.setSelected(PropertyManager.getInstance().getBooleanProp(PropertyManager.NOTIFY_INCOMING, true));

        overlayOut.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                PropertyManager.getInstance().setProp(PropertyManager.NOTIFY_OUTGOING, String.valueOf(overlayOut.isSelected()));
            }
        });
        overlayOut.setSelected(PropertyManager.getInstance().getBooleanProp(PropertyManager.NOTIFY_OUTGOING, true));

        // Setup TTS
        poeLogReader.setWordIncludeTextField(voiceShoutoutWords);
        poeLogReader.setWordExcludeTextField(voiceExcludeWords);
        voiceActive.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (poeLogReader.isBalconAvailable()) {
                    if (voiceSpeakerCB.getItems().isEmpty()) {
                        List<String> supportedVoices = poeLogReader.getSupportedVoices();
                        voiceSpeakerCB.setItems(FXCollections.observableArrayList(supportedVoices));
                    }
                    poeLogReader.setUseTTS(voiceActive.isSelected());
                    setDisableVoiceControls(false);
                } else {
                    File jarDir = new File("");
                    LogManager.getInstance().log(Main.class, "Balcon not found! To enable TTS notifications, download balcon.exe from http://balabolka.site/balcon.zip place it in " + jarDir.getAbsolutePath() + " and restart the application.");
                    setDisableVoiceControls(true);
                }
                voiceActive.setSelected(poeLogReader.doUseTTS());
            }
        });

        if (!poeLogReader.isBalconAvailable()) {
            voiceLayout.setVisible(false);
            openTtsConfigBtn.setDisable(true);
        }
        voiceSpeakerCB.setValue(poeLogReader.getVoice());
        voiceSpeakerCB.setOnAction(event -> {
            String selected = voiceSpeakerCB.getValue();
            if (selected != null && !selected.isEmpty()) {
                poeLogReader.setVoice(selected);
            }
        });

        volumeLabel.setTooltip(new Tooltip("Set volume of the voice speaker"));

        int volume = PropertyManager.getInstance().getVoiceVolume();
        volumeLabel.setText(volume + " %");
        volumeSlider.setValue(volume);
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> volumeLabel.setText(newValue.intValue() + " %"));
        volumeSlider.valueChangingProperty().addListener((observable, changeEnds, changeStarts) -> {
            if (changeEnds) {
                int newVolume = (int) volumeSlider.getValue();
                poeLogReader.setVolume(newVolume);
                PropertyManager.getInstance().setVoiceVolume(String.valueOf(newVolume));
                if (poeLogReader.isBalconAvailable()) {
                    poeLogReader.testSpeech();
                } else {
                    File jarDir = new File("");
                    LogManager.getInstance().log(getClass(), "Balcon not found! To enable TTS notifications, download balcon.exe from http://balabolka.site/balcon.zip place it in " + jarDir.getAbsolutePath() + " and restart the application.");
                }
            }
        });

        // Who knew daniel was used in Pendulum's Bloodsugar?
        volumeTopicLabel.setOnMouseClicked(event -> {
            if (poeLogReader.getVoice() != null && poeLogReader.getVoice().contains("Daniel")) {
                String msg;
                volumeClicked++;
                if (volumeClicked >= 5) {
                    msg = "Ok, Fuck it, I lied! It's Drum and Bass, what you gonna do!";
                } else {
                    return;
                }
                try {
                    poeLogReader.textToSpeech(msg, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        openTtsConfigBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                poeLogReader.openTTSConfig();
            }
        });

        // Setup Overlay
        parseTestButton.setOnAction(event -> poeLogReader.processTestMessage(true));
        parseOutTestButton.setOnAction(event -> poeLogReader.processTestMessage(false));
        reloadConfigBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                OverlayManager.getInstance().loadConfig();
                if (poeLogReader.doUseTTS()) {
                    poeLogReader.loadTTSConfig();
                }
            }
        });

        openConfigBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                OverlayManager.getInstance().openOverlayConfig();
            }
        });

        poePath.setText(PropertyManager.getInstance().getPathOfExilePath());

        // Auto Update checkbox
        autoUpdate.setOnAction(event -> {
            tradeManager.setAutoUpdate(autoUpdate.isSelected());
            autoUpdate.setSelected(tradeManager.isAutoUpdating());
        });
        autoUpdatePlayerOffers.setSelected(PropertyManager.getInstance().getBooleanProp("auto_update_player_offers", false));
        autoUpdatePlayerOffers.setOnAction(event -> {
            boolean enable = !PropertyManager.getInstance().getBooleanProp("auto_update_player_offers", false);
            PropertyManager.getInstance().setProp("auto_update_player_offers", String.valueOf(enable));
            autoUpdatePlayerOffers.setSelected(PropertyManager.getInstance().getBooleanProp("auto_update_player_offers", false));
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

        if (!PropertyManager.getInstance().getBooleanProp("auto_update_enabled", false)) {
            autoUpdateLayout.setVisible(false);
        }
    }

    private void setWebParser(String newValue) {
        if (newValue.equals(PoeTradeApiParser.IDENTIFIER)) {
            //pathofexile.com does this by default.
            filterOutOfStock.setSelected(true);
            filterWithoutStockInfo.setSelected(true);
            PropertyManager.getInstance().setFilterOutOfStock(false);
            PropertyManager.getInstance().setFilterNoStockInfo(false);
            filterOutOfStock.setDisable(true);
            filterWithoutStockInfo.setDisable(true);
        } else {
            filterOutOfStock.setSelected(false);
            filterWithoutStockInfo.setSelected(false);
            filterOutOfStock.setDisable(false);
            filterWithoutStockInfo.setDisable(false);
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

        if (inString != null && !inString.isEmpty()) {
            try {
                float inAmount = dFormat.parse(inString).floatValue();
                String result;
                if (outputReversed) {
                    result = prettyFloat(inAmount * tradeManager.getCurrencyValue(valueOutputCB.getValue(), valueInputCB.getValue()), false, false);
                } else {
                    result = prettyFloat(inAmount * tradeManager.getCurrencyValue(valueInputCB.getValue(), valueOutputCB.getValue()), false, false);
                }

                if (outputReversed) {
                    valueInputText.setText(result);
                    valueOutputText.setText(prettyFloat(inAmount, false, false));
                } else {
                    valueOutputText.setText(result);
                    valueInputText.setText(prettyFloat(inAmount, false, false));
                }
            } catch (ParseException pex) {
                LogManager.getInstance().log(getClass(), "Conversion failed! " + pex.getMessage());
            }
        }
    }

    private void setDisableVoiceControls(boolean val) {
        volumeLabel.setDisable(val);
        voiceShoutoutWords.setDisable(val);
        voiceExcludeWords.setDisable(val);
        voiceSpeakerCB.setDisable(val);
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
    }

    @Override
    public void onUpdateFinished() {
        LogManager.getInstance().debug(TradeManager.class, "Update took " + prettyFloat((System.currentTimeMillis() - updateStart) / 1000f) + " seconds");
        CurrencyID newValue = offerSecondary.getValue();
        if (newValue != null) {
            buyOfferTable.setItems(tradeManager.getBuyOffers(newValue));
            sellOfferTable.setItems(tradeManager.getSellOffers(newValue));
        }
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

        updateButton.setText("Update");
        updateButton.setDisable(false);

        currencyFilterChanged = false;
    }

    @Override
    public void onParsingFinished() {
        if (!TradeManager.getInstance().getCurrentDeals().isEmpty()) {
            float time = System.currentTimeMillis() - parseStartTime;
            LogManager.getInstance().log(TradeManager.class, "Parsing took " + prettyFloat(time, true, false) + " milliseconds");
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
        } else if (PropertyManager.POE_PATH.equals(key)) {
            poePath.setText(value);
        } else if (PropertyManager.VOICE_SPEAKER.equals(key)) {
            voiceSpeakerCB.setValue(value);
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
        return prettyFloat(in, true, false);
    }

    public static String prettyFloat(float in, boolean hideEmptyDecimal) {
        return prettyFloat(in, hideEmptyDecimal, false);
    }

    public static String prettyFloat(float in, boolean hideEmptyDecimal, boolean hideZero) {
        if (hideZero && in == 0) {
            return "---";
        }

        DecimalFormat format;
        if (hideEmptyDecimal) {
            format = dFormat;
        } else {
            format = valueFormat;
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
        if (query == null || query.isEmpty() || query.equals("null")) {
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

    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null || searchStr == null) return false;

        final int length = searchStr.length();
        if (length == 0)
            return true;

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(true, i, searchStr, 0, length))
                return true;
        }
        return false;
    }

    public static Boolean isOnWindowsOS = com.sun.jna.Platform.isWindows();
    public static String poeWindowClass = "POEWindowClass";
    public static String poeWindowName = "Path of Exile";
    public static boolean poeToForeground() {
        if (isOnWindowsOS) {
            WinDef.HWND poeWindow = User32.INSTANCE.FindWindow(poeWindowClass, null); // window class
            if (poeWindow == null) {
                poeWindow = User32.INSTANCE.FindWindow(null, poeWindowName); // window title
            }
            if (poeWindow != null) {
                WinDef.HWND foregroundWindow = User32.INSTANCE.GetForegroundWindow();
                if (poeWindow.equals(foregroundWindow)) {
                    return true;
                }
                boolean show = User32.INSTANCE.ShowWindow(poeWindow, 9);        // SW_RESTORE
                boolean foreground = User32.INSTANCE.SetForegroundWindow(poeWindow);   // bring to front
                return show & foreground;
            }
            return false;
        }
        return true;
    }
}