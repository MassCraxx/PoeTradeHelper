package de.crass.poetradehelper.model;

public class ResponseButton {
    String label;
    String message;
    boolean closeSelf = false;

    public ResponseButton(){}

    public ResponseButton(String label, String message, boolean closeSelf){
        this.label = label;
        this.message = message;
        this.closeSelf = closeSelf;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCloseSelf() {
        return closeSelf;
    }

    public void setCloseSelf(boolean closeSelf) {
        this.closeSelf = closeSelf;
    }
}
