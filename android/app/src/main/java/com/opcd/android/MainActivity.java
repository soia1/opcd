package com.opcd.android;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

/**
 * MainActivity for OPCD Android.
 * Manages permissions, runtime setup, binds to OpenCodeService, and loads the OpenCode web UI.
 */
public class MainActivity extends AppCompatActivity
        implements OpenCodeService.ServiceListener, RuntimeManager.SetupListener {

    private static final String TAG = "OPCD-Main";
    private static final int DEFAULT_PORT = 4096;

    private View setupContainer;
    private View mainContainer;
    private WebView webView;
    private ProgressBar progressBar;
    private ProgressBar setupProgressBar;
    private TextView statusText;
    private TextView setupStatusText;
    private Button setupButton;

    private RuntimeManager runtimeManager;
    private OpenCodeService openCodeService;
    private boolean serviceBound = false;
    private boolean setupInProgress = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            OpenCodeService.LocalBinder binder = (OpenCodeService.LocalBinder) service;
            openCodeService = binder.getService();
            openCodeService.setListener(MainActivity.this);
            serviceBound = true;

            if (openCodeService.isServerRunning()) {
                showMainUi();
                loadOpenCodeUi();
            } else {
                updateStatus("Waiting for server...");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            openCodeService = null;
            serviceBound = false;
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupContainer = findViewById(R.id.setupContainer);
        mainContainer = findViewById(R.id.mainContainer);
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        setupProgressBar = findViewById(R.id.setupProgressBar);
        statusText = findViewById(R.id.statusText);
        setupStatusText = findViewById(R.id.setupStatusText);
        setupButton = findViewById(R.id.setupButton);

        runtimeManager = new RuntimeManager(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });

        PermissionHelper.requestPermissions(this);
        checkRuntimeState();
    }

    private void checkRuntimeState() {
        if (runtimeManager.isRuntimeReady()) {
            showMainUi();
            bindToService();
        } else {
            showSetupUi();
            setupStatusText.setText("Linux runtime is not installed. Tap Setup to download and install.");
            setupButton.setOnClickListener(v -> startSetup());
        }
    }

    private void showSetupUi() {
        setupContainer.setVisibility(View.VISIBLE);
        mainContainer.setVisibility(View.GONE);
    }

    private void showMainUi() {
        setupContainer.setVisibility(View.GONE);
        mainContainer.setVisibility(View.VISIBLE);
    }

    private void startSetup() {
        if (setupInProgress) return;
        setupInProgress = true;
        setupButton.setEnabled(false);
        setupProgressBar.setVisibility(View.VISIBLE);
        setupStatusText.setText("Starting setup...");

        // Run setup on a background thread.
        runtimeManager.setupRuntime(this);
    }

    @Override
    public void onProgress(String message) {
        runOnUiThread(() -> setupStatusText.setText(message));
    }

    @Override
    public void onError(String error) {
        setupInProgress = false;
        runOnUiThread(() -> {
            setupButton.setEnabled(true);
            setupProgressBar.setVisibility(View.GONE);
            setupStatusText.setText("Setup failed: " + error);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Setup Error")
                    .setMessage(error)
                    .setPositiveButton("Retry", (dialog, which) -> startSetup())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public void onComplete() {
        setupInProgress = false;
        runOnUiThread(() -> {
            setupProgressBar.setVisibility(View.GONE);
            setupStatusText.setText("Setup complete.");
            showMainUi();
            bindToService();
        });
    }

    private void bindToService() {
        Intent intent = new Intent(this, OpenCodeService.class);
        intent.setAction(OpenCodeService.ACTION_START);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!PermissionHelper.hasAllPermissions(this)) {
            Toast.makeText(this, "Some permissions were denied. The app may not work correctly.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_reload) {
            loadOpenCodeUi();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_stop_server) {
            stopServer();
            return true;
        } else if (id == R.id.action_terminal) {
            startActivity(new Intent(this, TerminalActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopServer() {
        if (serviceBound && openCodeService != null) {
            openCodeService.stopServer();
            updateStatus("Server stopped");
        }
    }

    private void loadOpenCodeUi() {
        int port = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("server_port", DEFAULT_PORT);
        String url = "http://127.0.0.1:" + port;
        Log.i(TAG, "Loading OpenCode UI: " + url);
        webView.loadUrl(url);
    }

    private void updateStatus(final String status) {
        runOnUiThread(() -> {
            if (statusText != null) {
                statusText.setText(status);
                statusText.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onStatusChanged(String status) {
        updateStatus(status);
    }

    @Override
    public void onLogReceived(String log) {
        Log.d(TAG, log);
    }

    @Override
    public void onServerReady() {
        updateStatus("Server ready");
        loadOpenCodeUi();
    }

    @Override
    public void onError(String error) {
        updateStatus("Error: " + error);
        new AlertDialog.Builder(this)
                .setTitle("OpenCode Error")
                .setMessage(error)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        boolean keepAlive = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("keep_alive", true);
        if (!keepAlive) {
            Intent stopIntent = new Intent(this, OpenCodeService.class);
            stopIntent.setAction(OpenCodeService.ACTION_STOP);
            startService(stopIntent);
        }
        if (webView != null) {
            webView.destroy();
        }
    }
}
