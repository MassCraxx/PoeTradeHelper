package de.crass.poetradehelper.tts;

import java.io.File;

public interface FileListener {
    void onFileChanged(File path, boolean newFile);

    void onNewLine(File file, String newLine);

    void onShutdown();
}
