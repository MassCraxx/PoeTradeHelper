package de.crass.poetradehelper.ui;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.crass.poetradehelper.LogManager;
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
    private int x = -1;
    private int y = -1;

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

    public void showNotificationOverlay(boolean in, String playerName, String item, String price, String stashTab, int stashX, int stashY) {
        overlayFrames.add(new OverlayFrame(in, playerName, item, price, x, y, stashTab, stashX, stashY));
    }

    public void shutdown() {
        disposeAll();
    }

    public void setOverlayLocation(Point point) {
        this.x = point.x;
        this.y = point.y;
    }

    public OverlayConfig getOverlayConfig() {
        if (currentConfig == null) {
            currentConfig = getDefaultConfig();
        }
        return currentConfig;
    }

    private OverlayConfig getDefaultConfig() {
        List<ResponseButton> inButtons = new LinkedList<>();
        inButtons.add(new ResponseButton("kk", "kk", false));
        inButtons.add(new ResponseButton("sec", "Just a sec", false));
        inButtons.add(new ResponseButton("busy", "Sry pretty busy right now, try again later", true));
        inButtons.add(new ResponseButton("sold", "Sold already", true));
        inButtons.add(new ResponseButton("oos", "Sry, out of stock", true));
        inButtons.add(new ResponseButton("thx", "Thanks mate! Gl", false));
        OverlayConfig config = new OverlayConfig();
        config.setIncomingButtons(inButtons);
        return config;
    }

    public boolean loadConfig() {
        File file = configFile;
        if (file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                OverlayConfig config = mapper.readValue(file, OverlayConfig.class);
                LogManager.getInstance().log(getClass(), "Config successfully loaded.");
                currentConfig = config;
                return true;
            } catch (JsonMappingException j) {
                LogManager.getInstance().log(getClass(), "Config corrupted! " + j.getMessage());
            } catch (IOException e) {
                LogManager.getInstance().log(getClass(), "Error while loading config.");
                e.printStackTrace();
            }
        } else {
            currentConfig = getDefaultConfig();
            storeConfig();
            return true;
        }
        LogManager.getInstance().log(getClass(), "Loading config failed.");
        return false;
    }

    private void storeConfig() {
        LogManager.getInstance().log(getClass(), "Storing config file in " + configFile);
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
}
