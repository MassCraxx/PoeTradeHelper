package de.crass.poetradehelper.ui;

import de.crass.poetradehelper.model.OverlayButtonConfig;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class OverlayManager {
    public static OverlayManager instance;

    private Robot robot;
    JFrame overlayFrame;
    Stage newWindow;
    boolean overlayActive = false;
    private JFrame markerFrame;
    private OverlayButtonConfig currentConfig;
    private float screenSize;

    public static OverlayManager getInstance() {
        if (instance == null) {
            instance = new OverlayManager();
        }
        return instance;
    }

    public OverlayManager() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        //Ge
        screenSize = 1920;
    }

    public void showOverlayWindow() {
        if (overlayActive) {
            overlayActive = false;
        } else {
            overlayActive = true;

            showSwingOverlay();
        }
    }

    public void showSwingOverlay() {
        overlayFrame = new JFrame("Transparent Window");

        overlayFrame.getRootPane().setOpaque(false);
        overlayFrame.setUndecorated(true);
        overlayFrame.setBackground(new Color(0, 0, 0, 0));
        overlayFrame.setLocationRelativeTo(null);
        overlayFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        overlayFrame.setFocusableWindowState(false);
        overlayFrame.setFocusable(false);
        overlayFrame.setAlwaysOnTop(true);
        overlayFrame.setVisible(false);

        // Without this, the window is draggable from any non transparent
        // point, including points  inside textboxes.
        overlayFrame.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);

        overlayFrame.getContentPane().setLayout(new BorderLayout());

        // In buttons
        if (currentConfig == null) {
            currentConfig = getDefaultButtonConfig();
        }
        for (Map.Entry<String, String> entry : currentConfig.getIncomingButtons().entrySet()) {
            JButton button = new JButton();
            button.setText(entry.getKey());
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    sendTextMessage(entry.getValue());
                }
            });
            overlayFrame.getContentPane().add(button);
        }

        overlayFrame.pack();

        overlayFrame.setSize(230, 100);
        overlayFrame.setLocation(new Point(Math.round(screenSize / 2f) - 50, 0));
        overlayFrame.setVisible(true);

//        for(int x = 1; x <= 12; x++){
//            for(int y = 1; y <= 12; y++){
//                renderStashMarker(x,y);
//            }
//        }
    }

    public void showFXOverlay() {
        //FIXME: Will not be unfocusable...
        javafx.scene.control.Button testButton = new Button();
        testButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                sendTextMessage("this is a test");
            }
        });

        StackPane secondaryLayout = new StackPane();
        secondaryLayout.setFocusTraversable(false);
        secondaryLayout.getChildren().add(testButton);

        Scene secondScene = new Scene(secondaryLayout, 230, 100);

        // New window (Stage)
        newWindow = new Stage();
        newWindow.setTitle("Second Stage");
        newWindow.setScene(secondScene);
        newWindow.initStyle(StageStyle.TRANSPARENT);
        newWindow.setAlwaysOnTop(true);
        newWindow.setIconified(false);

        // Set position of second window, related to primary window.
        int screenSize = 1920;
//            newWindow.setX(screenSize / 2f - newWindow.getWidth() / 2f);
        newWindow.setX(screenSize / 2f - 50);
        newWindow.setY(0);

        newWindow.show();
    }

    public void sendTextMessage(String msg) {
        StringSelection selection = new StringSelection(msg);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        // Remove old text window content
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyPress(KeyEvent.VK_BACK_SPACE);
        robot.keyRelease(KeyEvent.VK_BACK_SPACE);

        // Paste
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }

    public void shutdown() {
        if (overlayFrame != null) {
            overlayFrame.setVisible(false);
            overlayFrame.dispose();
            overlayFrame = null;
        }

        if (markerFrame != null) {
            markerFrame.setVisible(false);
            markerFrame.dispose();
            markerFrame = null;
        }

        if (newWindow != null) {
            newWindow.close();
            newWindow = null;
        }
    }

    public void renderStashMarker(int x, int y) {
        if (markerFrame == null) {
            markerFrame = new JFrame("Transparent Window");

            markerFrame.getRootPane().setOpaque(false);
            markerFrame.setUndecorated(true);
            markerFrame.setBackground(new Color(0, 0, 0, 0));
            markerFrame.setLocationRelativeTo(null);
            markerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            markerFrame.setFocusableWindowState(false);
            markerFrame.setFocusable(false);
            markerFrame.setAlwaysOnTop(true);
            markerFrame.setVisible(false);

            // Without this, the window is draggable from any non transparent
            // point, including points  inside textboxes.
            markerFrame.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);

            markerFrame.getContentPane().setLayout(new BorderLayout());

            markerFrame.getContentPane().add(getCellPlaceholder());
            markerFrame.pack();
        }

        int size = 53;
        markerFrame.setSize(size, size);
        int screenSize = 1920;
        int xLoc = 14 + size * (x - 1);
        int yLoc = 158 + size * (y - 1);
        markerFrame.setLocation(xLoc, yLoc);
        markerFrame.setVisible(true);
    }

    // Mercury trade creates whole grid and sets visible what is active...
    private JPanel getCellPlaceholder() {
        JPanel cell = new JPanel();
        cell.setOpaque(true);
        cell.setBackground(new Color(50, 0, 0, 0));
        cell.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255)));
        return cell;
    }

    private OverlayButtonConfig getDefaultButtonConfig() {
        HashMap<String, String> inButtons = new HashMap<>();
        inButtons.put("thx", "Thanks mate! Gl");
        inButtons.put("sec", "Just a sec");
        inButtons.put("busy", "Sry pretty busy right now");
        inButtons.put("sold", "Sold already");
        inButtons.put("oos", "Sry out of stock");
        inButtons.put("kk", "kk");
        OverlayButtonConfig config = new OverlayButtonConfig();
        config.setIncomingButtons(inButtons);
        return config;
    }
}
