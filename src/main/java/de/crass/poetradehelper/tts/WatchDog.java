package de.crass.poetradehelper.tts;

import de.crass.poetradehelper.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class WatchDog implements Runnable {
    private Listener listener;
    private Path path;
    private boolean running = false;
    private boolean stop = false;

    private long lastLineCount = -1;
    private String lastFileChecked = "";

    WatchDog(Path path, Listener listener) {
        this.path = path;
        this.listener = listener;
    }

    @Override
    public void run() {
        running = true;
//        Main.LogManager.getInstance().log(getClass(), "Watching files in: " + path);
        WatchService watchService = null;
        WatchKey wk = null;
        try {
            watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            while (!stop) {
                wk = watchService.take();
                if(stop){
                    break;
                }
                List<WatchEvent<?>> events = wk.pollEvents();
                for (WatchEvent<?> event : events) {
                    //we only register "ENTRY_MODIFY" so the context is always a Path.
                    final Path changed = (Path) event.context();
                    String filePath = path + File.separator + changed;
                    //Main.LogManager.getInstance().log(getClass(), "File changed: " + changed);
                    File file = new File(filePath);
                    onFileChanged(file);
                    listener.onFileChanged(file);
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    LogManager.getInstance().log(getClass(), "!!! Key has been unregistered !!!");
                }
            }
        } catch (IOException e) {
            LogManager.getInstance().log(getClass(), "IOException! " + e);
        } catch (InterruptedException e) {
            LogManager.getInstance().log(getClass(), "Interrupted!");
        } catch (Exception e) {
            LogManager.getInstance().log(getClass(), "Random Exception! " + e);
        } finally {
            LogManager.getInstance().log(getClass(), "Shutting down.");
            if (wk != null) {
                wk.cancel();
                wk = null;
            }
            if (watchService != null) {
                try {
                    watchService.close();
                    watchService = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        running = false;
        listener.onShutDown();
    }

    private void onFileChanged(File file) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String sCurrentLine;
            long lineCounter = 0;
            while ((sCurrentLine = br.readLine()) != null) {
                if (!lastFileChecked.equals(file.getName())) {
                    // If new file, skip existing lines.
                    long lastLine = 0;
                    while (br.readLine() != null) {
                        lastLine++;
                    }
                    lastLineCount = lastLine;

                    lastFileChecked = file.getName();
                    br = new BufferedReader(new FileReader(file));
                    continue;
                }
                lineCounter++;
                // Only check lines not checked before
                if (lineCounter > lastLineCount) {
                    listener.onNewLine(file, sCurrentLine);
                }
            }
            // Not sure why this would happen...
            if (lineCounter != 0) {
                lastLineCount = lineCounter;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public interface Listener {
        void onFileChanged(File path);

        void onNewLine(File file, String newLine);

        void onShutDown();
    }

    public void stop(){
        stop = true;
    }
}
