package de.crass.poetradehelper.model;

import java.util.LinkedList;
import java.util.List;

public class OverlayConfig {
    List<ResponseButton> incomingButtons = new LinkedList<>();
    List<ResponseButton> outgoingButtons = new LinkedList<>();

    public List<ResponseButton> getIncomingButtons() {
        return incomingButtons;
    }

    public void setIncomingButtons(List<ResponseButton> incomingButtons) {
        this.incomingButtons = incomingButtons;
    }

    public List<ResponseButton> getOutgoingButtons() {
        return outgoingButtons;
    }

    public void setOutgoingButtons(List<ResponseButton> outgoingButtons) {
        this.outgoingButtons = outgoingButtons;
    }
}
