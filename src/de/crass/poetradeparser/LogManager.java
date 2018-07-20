package de.crass.poetradeparser;

import javafx.scene.control.TextArea;

/**
 * Created by mcrass on 20.07.2018.
 */
public class LogManager {

    static LogManager instance;
    private TextArea console;

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    public void setConsole(TextArea console) {
        this.console = console;
    }

    public void log(Class clazz, String log) {
        String timestamp = "";
//                '>' + String.valueOf(System.currentTimeMillis()) + '<';
        String msg = timestamp + '[' + clazz.getSimpleName() + ']' + ' ' + log;
        System.out.println(msg);

        if (console != null) {
            String text = console.getText();
            console.setText(msg + '\n' + text);
        }
    }
}
