package de.crass.poetradehelper.ui;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.crass.poetradehelper.LogManager;
import de.crass.poetradehelper.PropertyManager;
import de.crass.poetradehelper.model.OverlayConfig;
import de.crass.poetradehelper.model.ResponseButton;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class OverlayManager {
    public static OverlayManager instance;

    List<OverlayFrame> overlayFrames = new LinkedList<>();
    private int x = Integer.parseInt(PropertyManager.getInstance().getProp("overlay_x", "-1"));
    private int y = Integer.parseInt(PropertyManager.getInstance().getProp("overlay_y", "-1"));

    private OverlayConfig currentConfig;
    private File configFile = new File("overlayconfig.json");

    OverlayManager() {
        loadConfig();
    }

    public static OverlayManager getInstance() {
        if (instance == null) {
            instance = new OverlayManager();
        }
        return instance;
    }

    public void showNotificationOverlay(boolean in, String playerName, String item, String price, String stashTab, int stashX, int stashY, String msg) {
        overlayFrames.add(new OverlayFrame(currentConfig, in, playerName, item, price, x, y, stashTab, stashX, stashY, msg));
    }

    public void shutdown() {
        disposeAll();
    }

    public void setOverlayLocation(Point point) {
        this.x = point.x;
        this.y = point.y;
        PropertyManager.getInstance().setProp("overlay_x", String.valueOf(x));
        PropertyManager.getInstance().setProp("overlay_y", String.valueOf(y));
    }

    private OverlayConfig getDefaultConfig() {
        List<ResponseButton> inButtons = new LinkedList<>();
        inButtons.add(new ResponseButton("kk", "kk", false));
        inButtons.add(new ResponseButton("sec", "Just a sec", false));
        inButtons.add(new ResponseButton("busy", "Sry pretty busy right now, try again later", true));
        inButtons.add(new ResponseButton("sold", "Sold already", true));
        inButtons.add(new ResponseButton("oos", "Sry, out of stock", true));
        inButtons.add(new ResponseButton("thx", "Thanks mate! Gl", false));
        List<ResponseButton> outButtons = new LinkedList<>();
        outButtons.add(new ResponseButton("kk", "kk", false));
        outButtons.add(new ResponseButton("u there", "Are you available for a trade? Please write me back when you are ready.", false));
        outButtons.add(new ResponseButton("nvm", "Sorry, nevermind. Gl", true));
        outButtons.add(new ResponseButton("omw", "Just a sec, I am on my way.", false));
        outButtons.add(new ResponseButton("thx", "Thanks mate! Gl", false));

        OverlayConfig config = new OverlayConfig();
        config.setIncomingButtons(inButtons);
        config.setOutgoingButtons(outButtons);
        return config;
    }

    public void openOverlayConfig() {
        File file = configFile;
        if (file.exists()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Overlay Config not found!");
            }
        } else {
            loadConfig();
        }
    }

    public void loadConfig() {
        File file = configFile;
        if (file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                OverlayConfig config = mapper.readValue(file, OverlayConfig.class);
                LogManager.getInstance().log(getClass(), "Overlay config '" + configFile.getName() + "' successfully loaded.");
                currentConfig = config;
            } catch (JsonMappingException j) {
                LogManager.getInstance().log(getClass(), "Loading overlay config '" + configFile.getName() + "' failed. Overlay config corrupted! " + j.getMessage());
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Loading overlay config '" + configFile.getName() + "' failed. Error while loading config.");
                e.printStackTrace();
            }
        } else {
            currentConfig = getDefaultConfig();
            storeConfig();
        }
    }

    private void storeConfig() {
        LogManager.getInstance().log(getClass(), "Storing overlay config file in " + configFile);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        ObjectWriter writer = mapper.writer();
        try {
            writer.writeValue(configFile, currentConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disposeAll() {
        if (overlayFrames != null && !overlayFrames.isEmpty()) {
            for (OverlayFrame overlayFrame : overlayFrames) {
                if (overlayFrame != null) {
                    overlayFrame.setVisible(false);
                    overlayFrame.dispose();
                }
            }
        }
    }

    public OverlayConfig getCurrentConfig() {
        return currentConfig;
    }
}
