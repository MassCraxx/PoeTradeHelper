package de.crass.poetradehelper.model;

import java.util.LinkedList;
import java.util.List;

public class OverlayConfig {
    List<ResponseButton> incomingButtons = new LinkedList<>();
    List<ResponseButton> outgoingButtons = new LinkedList<>();

    boolean notifyIncoming = true;
    boolean notifyOutgoing = false;

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

    public boolean getNotifyIncoming() {
        return notifyIncoming;
    }

    public void setNotifyIncoming(boolean notifyIncoming) {
        this.notifyIncoming = notifyIncoming;
    }

    public boolean getNotifyOutgoing() {
        return notifyOutgoing;
    }

    public void setNotifyOutgoing(boolean notifyOutgoing) {
        this.notifyOutgoing = notifyOutgoing;
    }
}
