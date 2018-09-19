package de.crass.poetradehelper;/**
 * Created by mcrass on 19.07.2018.
 */

import de.crass.poetradehelper.model.CurrencyDeal;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.parser.ParseListener;
import de.crass.poetradehelper.parser.TradeManager;
import de.crass.poetradehelper.tts.PoeChatTTS;
import de.crass.poetradehelper.ui.CurrencyOfferCell;
import de.crass.poetradehelper.ui.PlayerTradeCell;
import javafx.application.Application;
import javafx.application.Platform;
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
    public static final String versionText = "v0.3.1-SNAPSHOT";

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

    private static Stage currentStage;

    private TradeManager tradeManager;

    private static PoeChatTTS poeChatTTS;

    private Timer updateTimer;
    private int updateDelay = 1000 * 60 * 5;

    private boolean currencyFilterChanged = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("layout.fxml"));
        Scene scene = new Scene(root, 640, 650);
        scene.getStylesheets().add(getClass().getResource("stylesheet.css").toExternalForm());
        primaryStage.setMinWidth(580);
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
                if(tradeManager.isUpdating()){
                    tradeManager.cancelUpdate();
                    updateButton.setDisable(true);
                } else{
                    tradeManager.updateOffers(currencyFilterChanged);
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
                if(newCurrency != null) {
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
                if(focus != null) {
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
        }
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

        currencyFilterChanged = false;
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
        DecimalFormat df = new DecimalFormat("#.##");
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

    private void startUpdateTimer() {
        if(updateTimer == null) {
            updateTimer = new Timer("poeTradeHelperUpdateTimer");
            updateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    LogManager.getInstance().log(getClass(), "Invoke Automatic Update");
                    TradeManager.getInstance().updateOffers(currencyFilterChanged);
                }
            }, updateDelay, updateDelay);
        }
    }

    private void stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }
}
