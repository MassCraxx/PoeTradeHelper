package de.crass.poetradeparser;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        String timestamp = '>' + getTime() + '<';
        String consoleLog = timestamp + ' ' + '[' + clazz.getSimpleName() + ']' + ' ' + log;
        System.out.println(consoleLog);

        String msg = timestamp + ' ' + log;

        if (console != null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    String text = console.getText();
                    console.setText(msg + '\n' + text);
                }
            });
        }
    }

    public String getTime(){
        Date date = new Date();
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
//        formatter.setTimeZone(TimeZone.getTimeZone("UTC+1"));
        return formatter.format(date);
    }
}
