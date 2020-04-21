package de.crass.poetradehelper.model;

public class ResponseButton {
    String label;
    String message;
    boolean close = false;

    public ResponseButton(){}

    public ResponseButton(String label, String message, boolean close){
        this.label = label;
        this.message = message;
        this.close = close;
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

    public boolean isClose() {
        return close;
    }

    public void setClose(boolean close) {
        this.close = close;
    }
}
