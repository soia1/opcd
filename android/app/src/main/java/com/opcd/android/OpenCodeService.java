package com.opcd.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service that manages the Linux runtime (PRoot + Alpine)
 * and the OpenCode server lifecycle.
 */
public class OpenCodeService extends Service {

    private static final String TAG = "OpenCodeService";
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_START = "com.opcd.android.action.START";
    public static final String ACTION_STOP = "com.opcd.android.action.STOP";

    private final IBinder binder = new LocalBinder();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Process prootProcess;
    private volatile boolean isServerRunning = false;
    private ServiceListener listener;

    public interface ServiceListener {
        void onStatusChanged(String status);
        void onLogReceived(String log);
        void onServerReady();
        void onError(String error);
    }

    public class LocalBinder extends Binder {
        OpenCodeService getService() {
            return OpenCodeService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopServer();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        startServer();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setListener(ServiceListener listener) {
        this.listener = listener;
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    private RuntimeManager runtimeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        runtimeManager = new RuntimeManager(this);
        Log.i(TAG, "Service created");
    }

    /**
     * Starts the OpenCode server inside PRoot + Alpine.
     */
    private void startServer() {
        executor.execute(() -> {
            try {
                if (!runtimeManager.isRuntimeReady()) {
                    emitError("Linux runtime is not ready. Please run setup first.");
                    return;
                }

                emitStatus("Starting OpenCode server...");

                ProcessBuilder pb = new ProcessBuilder(
                        runtimeManager.buildProotBaseCommand()
                );
                pb.command().add("/bin/sh");
                pb.command().add("-c");
                pb.command().add("mkdir -p /root/projects && opencode serve --hostname 127.0.0.1 --port 4096");
                runtimeManager.configureProotEnv(pb);

                prootProcess = pb.start();

                // Read server output for logging.
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(prootProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        emitLog(line);
                    }
                }

                int exitCode = prootProcess.waitFor();
                isServerRunning = false;
                emitStatus("Server stopped (exit code: " + exitCode + ")");

            } catch (Exception e) {
                Log.e(TAG, "Failed to start server", e);
                emitError("Failed to start server: " + e.getMessage());
            }
        });

        // Wait for server to be reachable.
        executor.execute(() -> {
            for (int i = 0; i < 60; i++) {
                if (isServerReachable()) {
                    isServerRunning = true;
                    emitStatus("Server is running");
                    emitServerReady();
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            emitError("Server did not become reachable in time.");
        });
    }

    /**
     * Stops the OpenCode server and cleans up the process.
     */
    public void stopServer() {
        isServerRunning = false;
        executor.execute(() -> {
            if (prootProcess != null) {
                prootProcess.destroy();
                try {
                    if (!prootProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        prootProcess.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                prootProcess = null;
            }
            emitStatus("Server stopped");
        });
    }

    private boolean isServerReachable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 4096), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, OpenCodeService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(
                this, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, OpcdApplication.CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("OpenCode server is running")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(activityPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                .setOngoing(true)
                .build();
    }

    private void emitStatus(final String status) {
        Log.i(TAG, status);
        mainHandler.post(() -> {
            if (listener != null) listener.onStatusChanged(status);
        });
    }

    private void emitLog(final String log) {
        Log.d(TAG, log);
        mainHandler.post(() -> {
            if (listener != null) listener.onLogReceived(log);
        });
    }

    private void emitError(final String error) {
        Log.e(TAG, error);
        mainHandler.post(() -> {
            if (listener != null) listener.onError(error);
        });
    }

    private void emitServerReady() {
        mainHandler.post(() -> {
            if (listener != null) listener.onServerReady();
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopServer();
        executor.shutdown();
        if (runtimeManager != null) {
            runtimeManager.shutdown();
        }
    }
}
