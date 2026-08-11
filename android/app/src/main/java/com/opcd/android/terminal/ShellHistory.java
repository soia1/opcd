package com.opcd.android.terminal;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists terminal command history to a plain text file in the app's private
 * storage. Thread-safe; keeps at most {@link #MAX_ENTRIES} entries.
 */
public class ShellHistory {

    private static final String TAG = "ShellHistory";
    private static final String FILE_NAME = "terminal_history.txt";
    private static final int MAX_ENTRIES = 500;

    private final File file;
    private final List<String> entries = new ArrayList<>();

    public ShellHistory(Context context) {
        this.file = new File(context.getFilesDir(), FILE_NAME);
        load();
    }

    /**
     * Adds a command to the history. Blank commands and duplicates of the most
     * recent command are ignored. Persists immediately.
     */
    public synchronized void add(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        String trimmed = command.trim();
        if (!entries.isEmpty() && entries.get(entries.size() - 1).equals(trimmed)) {
            return;
        }
        entries.add(trimmed);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        save();
    }

    /** Returns all history entries, oldest first. */
    public synchronized List<String> getAll() {
        return new ArrayList<>(entries);
    }

    /** Clears the history and deletes the backing file. */
    public synchronized void clear() {
        entries.clear();
        if (file.exists() && !file.delete()) {
            Log.w(TAG, "Failed to delete history file");
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    entries.add(line);
                }
            }
            while (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load history", e);
        }
    }

    private void save() {
        try (Writer writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            for (String entry : entries) {
                writer.write(entry);
                writer.write("\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save history", e);
        }
    }
}
