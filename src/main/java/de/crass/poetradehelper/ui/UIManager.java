package de.crass.poetradehelper.ui;

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

public class UIManager {
    public static UIManager instance;

    private Robot robot;
    JFrame overlayFrame;
    Stage newWindow;
    boolean overlayActive = false;

    public static UIManager getInstance() {
        if (instance == null) {
            instance = new UIManager();
        }
        return instance;
    }

    public UIManager() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
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
        JButton button = new JButton();
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendTextMessage("This is a test");
            }
        });
        overlayFrame.getContentPane().add(button);
        overlayFrame.pack();

        overlayFrame.setVisible(true);
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

        if (newWindow != null) {
            newWindow.close();
            newWindow = null;
        }
    }
}
