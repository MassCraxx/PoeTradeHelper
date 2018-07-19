package de.crass.poetradeparser;/**
 * Created by mcrass on 19.07.2018.
 */

import de.crass.poetradeparser.model.CurrencyDeal;
import de.crass.poetradeparser.model.CurrencyID;
import de.crass.poetradeparser.parser.TradeManager;
import de.crass.poetradeparser.ui.CurrencyOfferCell;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Main extends Application {

    public static final String versionText = "0.2-SNAPSHOT";

    @FXML
    private TextField console;

    @FXML
    private ComboBox<CurrencyID> primaryComboBox;

    @FXML
    private ListView<CurrencyDeal> currencyList;

    @FXML
    private Button updateButton;

    @FXML
    private Text version;

//    private JPanel mainPanel;
//    private JButton updateButton;
//    private JTable currencyTable;
//    private JTabbedPane tabbedPane1;
//    private JComboBox<CurrencyID> currencyComboBox;
//    private JList<CurrencyID> currencyFilterList;
//    private DefaultListModel<CurrencyID> filterListModel;
//    private JComboBox<CurrencyID> currencyFilterCB;
//    private JButton currencyFilterAdd;
//    private JButton currencyFilterRem;
//    private CurrencyTableModel currencyTableModel;

    private TradeManager tradeManager;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        primaryStage.setTitle("PoeTradeParser");
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

    @FXML
    void initialize() {
        assert console != null : "fx:id=\"console\" was not injected: check your FXML file 'layout.fxml'.";
        assert currencyList != null : "fx:id=\"currencyList\" was not injected: check your FXML file 'layout.fxml'.";
        assert updateButton != null : "fx:id=\"updateButton\" was not injected: check your FXML file 'layout.fxml'.";
        assert version != null : "fx:id=\"version\" was not injected: check your FXML file 'layout.fxml'.";

        tradeManager = new TradeManager();
        setupUI();
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

        ObservableList<CurrencyDeal> currentDeals = tradeManager.getCurrentDeals();
        currencyList.setItems(currentDeals);

        updateButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                tradeManager.updateOffers();
            }
        });

//        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.values());
        ObservableList<CurrencyID> currencyList = FXCollections.observableArrayList(CurrencyID.EXALTED);
        primaryComboBox.setItems(currencyList);
        primaryComboBox.setValue(PropertyManager.getInstance().getPrimaryCurrency());
        primaryComboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                CurrencyID newValue = primaryComboBox.getValue();
                PropertyManager.getInstance().setPrimaryCurrency(newValue);
                tradeManager.parseDeals();
            }
        });
    }

    public static void log(Class clazz, String log) {
        System.out.println("[" + clazz.getSimpleName() + "]: " + log);
    }
}
