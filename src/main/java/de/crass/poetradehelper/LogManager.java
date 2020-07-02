package de.crass.poetradehelper;

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

    @SuppressWarnings("rawtypes")
    public void log(Class clazz, String log) {
        log(clazz.getSimpleName(), log, false);
    }

    public void log(String clazz, String log, boolean debug) {
        String timestamp = '>' + getTime() + '<';
        String consoleLog = timestamp + ' ' + '[' + clazz + ']' + ' ' + log;
        System.out.println(consoleLog);

        String msg = timestamp + ' ' + log;

        if (!debug && console != null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    boolean firstMsg = console.getLength() == 0;
                    console.appendText((firstMsg?"":'\n') + msg);
                    console.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
                }
            });
        }
    }

    @SuppressWarnings("rawtypes")
    public void debug(Class clazz, String log){
        log(clazz.getSimpleName(), log, true);
    }

    public String getTime() {
        Date date = new Date();
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
//        formatter.setTimeZone(TimeZone.getTimeZone("UTC+1"));
        return formatter.format(date);
    }
}
