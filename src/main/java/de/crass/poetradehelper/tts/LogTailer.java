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

    int skipped = 0;
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
        try (RandomAccessFile readWriteFileAccess = new RandomAccessFile(logFile, "r")) {
            readWriteFileAccess.seek(lastKnownPosition);
            // Reading and writing file
            while (running) {
                Thread.sleep(delay);
                long fileLength = readWriteFileAccess.length();
                if (skipped >= 5) {
                    LogManager.getInstance().log(getClass(), "LogFile is definitely smaller, resetting to last line.");
                    lastKnownPosition = fileLength;
                    readWriteFileAccess.seek(lastKnownPosition);
                } else if (fileLength < lastKnownPosition) {
                    // Fix for programs rewriting whole file...
                    LogManager.getInstance().log(getClass(), "LogFile got smaller, skipping read. (" + fileLength + " < " + lastKnownPosition+")");
                    skipped++;
                } else if (fileLength > lastKnownPosition) {
                    skipped = 0;
                    String currentLine;
                    while ((currentLine = readWriteFileAccess.readLine()) != null) {
                        currentLine = new String(currentLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        if (!currentLine.trim().isEmpty()) {
                            String finalCurrentLine = currentLine;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.onNewLine(logFile, finalCurrentLine);
                                }
                            }).start();
                        }
                    }
                    lastKnownPosition = readWriteFileAccess.getFilePointer();
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
