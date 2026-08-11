package com.opcd.android.terminal;

import android.util.Log;
import com.opcd.android.RuntimeManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A persistent interactive shell session running inside the Alpine Linux
 * runtime via PRoot.
 *
 * <p>The session spawns one long-lived {@code /bin/sh} process. Commands are
 * sent line-by-line through {@link #sendCommand(String)} and output chunks are
 * streamed back through {@link OutputListener#onOutput(String)}.
 *
 * <p>Note: this is pipe-based, not a real PTY. Interactive full-screen
 * programs (vim, htop) will not render correctly. That is acceptable for the
 * POC; a PTY-backed implementation can replace this class later.
 */
public class TerminalSession {

    private static final String TAG = "TerminalSession";

    /**
     * Callback interface for session events. Methods may be called from
     * background threads.
     */
    public interface OutputListener {
        /** Called when new output (stdout/stderr) is available. */
        void onOutput(String text);

        /** Called when the shell process exits. */
        void onExit(int code);
    }

    private final RuntimeManager runtimeManager;

    private Process process;
    private OutputStream stdin;
    private Thread readerThread;
    private OutputListener listener;
    private volatile boolean running = false;

    private final Object lock = new Object();

    public TerminalSession(RuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    /**
     * Starts the interactive shell. Idempotent: does nothing if a session is
     * already running.
     */
    public void start(OutputListener listener) {
        synchronized (lock) {
            if (running) {
                return;
            }
            this.listener = listener;
            try {
                File projectsDir = new File(runtimeManager.getBaseDir(), "projects");
                projectsDir.mkdirs();

                ProcessBuilder pb = new ProcessBuilder(
                        runtimeManager.getProotBin().getAbsolutePath(),
                        "-0",
                        "-r", runtimeManager.getRootfsDir().getAbsolutePath(),
                        "-b", "/dev",
                        "-b", "/proc",
                        "-b", "/sys",
                        "-b", projectsDir.getAbsolutePath() + ":/root/projects",
                        "-w", "/root",
                        "/bin/sh"
                );
                pb.redirectErrorStream(true);
                pb.environment().put("HOME", "/root");
                pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/sbin");
                // Plain TextView rendering: no ANSI escape support, so use a
                // dumb terminal to keep programs from emitting escape codes.
                pb.environment().put("TERM", "dumb");

                process = pb.start();
                stdin = process.getOutputStream();
                running = true;

                readerThread = new Thread(this::readLoop, "terminal-reader");
                readerThread.start();

                Log.i(TAG, "Shell session started");
            } catch (IOException e) {
                Log.e(TAG, "Failed to start shell session", e);
                running = false;
                if (listener != null) {
                    listener.onOutput("[Failed to start shell: " + e.getMessage() + "]\n");
                    listener.onExit(-1);
                }
            }
        }
    }

    /**
     * Sends a command to the shell. A newline is appended, so the command is
     * executed immediately. Safe to call when the session is not running
     * (no-op).
     */
    public void sendCommand(String command) {
        OutputStream out;
        synchronized (lock) {
            if (!running || stdin == null) {
                return;
            }
            out = stdin;
        }
        try {
            out.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to shell", e);
        }
    }

    /**
     * Kills the shell process and releases resources. The session can be
     * started again with {@link #start(OutputListener)} afterwards.
     */
    public void destroy() {
        Process p;
        Thread reader;
        synchronized (lock) {
            running = false;
            p = process;
            reader = readerThread;
            process = null;
            stdin = null;
            readerThread = null;
        }
        if (p != null) {
            p.destroy();
            try {
                if (!p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
            }
        }
        if (reader != null) {
            reader.interrupt();
        }
    }

    /** Returns true while the shell process is alive. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Reads output from the shell in chunks and forwards it to the listener.
     * Exits when the stream closes, then reports the process exit code.
     */
    private void readLoop() {
        int exitCode = -1;
        try {
            InputStream in;
            Process p;
            synchronized (lock) {
                if (process == null) {
                    return;
                }
                in = process.getInputStream();
                p = process;
            }

            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                OutputListener l = listener;
                if (l != null) {
                    l.onOutput(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            }

            exitCode = p.waitFor();
        } catch (IOException e) {
            Log.w(TAG, "Shell reader stream closed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (lock) {
                running = false;
            }
            OutputListener l = listener;
            if (l != null) {
                l.onExit(exitCode);
            }
        }
    }
}
