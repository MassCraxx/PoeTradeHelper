package de.crass.poetradehelper.model;

import java.util.HashMap;

public class OverlayButtonConfig {
    HashMap<String,String> incomingButtons = new HashMap<>();
    HashMap<String,String> outgoingButtons = new HashMap<>();

    public HashMap<String, String> getIncomingButtons() {
        return incomingButtons;
    }

    public void setIncomingButtons(HashMap<String, String> incomingButtons) {
        this.incomingButtons = incomingButtons;
    }

    public HashMap<String, String> getOutgoingButtons() {
        return outgoingButtons;
    }

    public void setOutgoingButtons(HashMap<String, String> outgoingButtons) {
        this.outgoingButtons = outgoingButtons;
    }
}
