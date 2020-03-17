package de.crass.poetradehelper.tts;

import de.crass.poetradehelper.LogManager;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class LogTailer implements Runnable {
    private int delay = 1000;
    private boolean onlyLogNewLines;
    private FileListener listener;
    private long lastKnownPosition = 0;
    private boolean running = true;
    private File logFile;

    public LogTailer(File myFile, boolean onlyNewLines, FileListener listener) {
        logFile = myFile;
        this.onlyLogNewLines = onlyNewLines;
        this.listener = listener;
    }

    public void stopRunning() {
        running = false;
        if(listener != null){
            listener.onShutdown();
        }
    }

    @Override
    public void run() {
        if(logFile == null || !logFile.exists()){
            LogManager.getInstance().log(getClass(), "LogTail could not be started, path not found: " + logFile);
            stopRunning();
            return;
        }

        LogManager.getInstance().log(getClass(), "LogTailer started for file " + logFile);
        if (onlyLogNewLines) {
            lastKnownPosition = logFile.length();
        }
        try {
            while (running) {
                Thread.sleep(delay);
                long fileLength = logFile.length();
                if (fileLength < lastKnownPosition) {
                    lastKnownPosition = fileLength;
                } else if (fileLength > lastKnownPosition) {

                    // Reading and writing file
                    RandomAccessFile readWriteFileAccess = new RandomAccessFile(logFile, "rw");
//                    RandomAccessFile readWriteFileAccess = new RandomAccessFile(logFile, "r");
                    readWriteFileAccess.seek(lastKnownPosition);
                    String currentLine;
                    while ((currentLine = readWriteFileAccess.readLine()) != null) {
                        currentLine = new String(currentLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        if (!currentLine.trim().isEmpty()) {
                            listener.onNewLine(logFile, currentLine);
                        }
                    }
                    lastKnownPosition = readWriteFileAccess.getFilePointer();
                    readWriteFileAccess.close();
                }
            }
        } catch (Exception e) {
            LogManager.getInstance().log(getClass(), "Exception! " + e.getMessage());
            stopRunning();
        }
        LogManager.getInstance().log(this.getClass(), "LogTailer exited.");
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
