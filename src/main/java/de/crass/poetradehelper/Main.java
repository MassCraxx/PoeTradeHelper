package de.crass.poetradehelper;
/*
  Created by mcrass on 19.07.2018.
 */

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import de.crass.poetradehelper.model.CurrencyID;
import de.crass.poetradehelper.model.CurrencyOffer;
import de.crass.poetradehelper.parser.PoeTradeApiParser;
import de.crass.poetradehelper.parser.PoeTradeWebParser;
import de.crass.poetradehelper.parser.TradeManager;
import de.crass.poetradehelper.tts.PoeLogReader;
import de.crass.poetradehelper.ui.OverlayManager;
import de.crass.poetradehelper.ui.UIManager;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.awt.*;
import java.net.URI;

@SuppressWarnings("unchecked")
public class Main extends Application{

    public static final String title = "PoeTradeHelper";
    public static final String versionText = "v0.9-SNAPSHOT";

    public static Stage currentStage;

    public static Boolean isOnWindowsOS = com.sun.jna.Platform.isWindows();
    public static String poeWindowClass = "POEWindowClass";
    public static String poeWindowName = "Path of Exile";
    public static String thisWindowClass = "GlassWndClass-GlassWindowClass-2";

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
                if (event.getCode() == KeyCode.TAB || event.getCode() == KeyCode.DEAD_CIRCUMFLEX) {
                    UIManager.getInstance().openLastTab();
                }
            }
        });

        updateTitle();
    }

    @Override
    public void stop() {
        LogManager.getInstance().log(getClass(), "Shutting down app.");
        OverlayManager.getInstance().shutdown();

//        PropertyManager.getInstance().setProp("window_divider", String.valueOf(splitPaneStatic.getDividerPositions()[0]));
        PropertyManager.getInstance().storeProperties();

        if (PoeLogReader.getInstance() != null) {
            PoeLogReader.getInstance().shutdown();
        }

        if (TradeManager.getInstance() != null) {
            TradeManager.getInstance().release();
        }

        LogManager.getInstance().log(getClass(), "Shutdown complete.");
    }

    public static void updateTitle() {
        if (currentStage == null) {
            return;
        }
        currentStage.setTitle(title + " - " + PropertyManager.getInstance().getCurrentLeague() + " League - " + PropertyManager.getInstance().getProp("trade_data_source"));
    }

    // UTIL

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

    public static boolean thisToForeground(int tabIndex) {
        UIManager.getInstance().openTab(tabIndex);
        if (isOnWindowsOS) {
            WinDef.HWND thisWindow = User32.INSTANCE.FindWindow(thisWindowClass, null); // window class
            if (thisWindow != null) {
                boolean show = User32.INSTANCE.ShowWindow(thisWindow, 9);        // SW_RESTORE
                boolean foreground = User32.INSTANCE.SetForegroundWindow(thisWindow);   // bring to front
                return show & foreground;
            } else {
                // Should never happen
                return false;
            }
        }
        return true;
    }
}