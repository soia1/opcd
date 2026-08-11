package com.opcd.android;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.opcd.android.terminal.ShellHistory;
import com.opcd.android.terminal.TerminalSession;

import java.util.List;

/**
 * Terminal screen for OPCD Android.
 *
 * <p>Renders the output of a {@link TerminalSession} in a dark, monospace,
 * scrollable view and sends user input back to the session. The output buffer
 * is capped at {@link #MAX_OUTPUT_CHARS} characters to avoid OOM on very long
 * command output.
 *
 * <p>The backend contract is line-based ({@link TerminalSession#sendCommand}
 * appends a newline), so special keys use the following workarounds:
 * <ul>
 *   <li>Tab: a raw tab character cannot be written through the line-based API,
 *       so the Tab button inserts a literal tab into the input field at the
 *       cursor position instead.</li>
 *   <li>Esc: clears the input field (closest useful behavior available).</li>
 *   <li>Ctrl+C: sent via sendCommand(CTRL_C); the trailing newline appended by
 *       the backend is harmless for an interrupt.</li>
 *   <li>Ctrl: toggles a modifier state; the next typed letter is converted to
 *       its control code (e.g. 'c' becomes 0x03) and sent the same way.</li>
 *   <li>Up/Down: navigate {@link ShellHistory} inside the input field.</li>
 * </ul>
 */
public class TerminalActivity extends AppCompatActivity {

    private static final int MAX_OUTPUT_CHARS = 200000;
    private static final String CTRL_C = "\u0003";

    private RuntimeManager runtimeManager;
    private TerminalSession session;
    private ShellHistory history;

    private View rootView;
    private ScrollView outputScroll;
    private TextView outputView;
    private EditText inputView;
    private Button ctrlButton;

    private boolean ctrlActive = false;
    private boolean sessionExited = false;
    private boolean closing = false;
    private boolean editingForCtrl = false;

    private int historyIndex = -1;
    private String historyDraft = "";

    private final TerminalSession.OutputListener outputListener = new TerminalSession.OutputListener() {
        @Override
        public void onOutput(final String text) {
            runOnUiThread(() -> appendOutput(text));
        }

        @Override
        public void onExit(final int code) {
            runOnUiThread(() -> handleSessionExit(code));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        runtimeManager = new RuntimeManager(this);
        if (!runtimeManager.isRuntimeReady()) {
            Toast.makeText(this,
                    "Linux runtime is not set up yet. Run setup from the main screen first.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rootView = findViewById(R.id.terminalRoot);
        outputScroll = findViewById(R.id.terminalScroll);
        outputView = findViewById(R.id.terminalOutput);
        inputView = findViewById(R.id.terminalInput);
        ctrlButton = findViewById(R.id.keyCtrl);

        history = new ShellHistory(this);

        setupOutputArea();
        setupSpecialKeys();
        setupInputRow();

        startSession();
        inputView.requestFocus();
    }

    private void setupOutputArea() {
        outputScroll.setOnClickListener(v -> {
            if (sessionExited) {
                restartSession();
            }
        });
    }

    private void setupSpecialKeys() {
        findViewById(R.id.keyTab).setOnClickListener(v -> insertAtCursor("\t"));
        findViewById(R.id.keyEsc).setOnClickListener(v -> inputView.setText(""));

        ctrlButton.setOnClickListener(v -> setCtrlActive(!ctrlActive));

        findViewById(R.id.keyUp).setOnClickListener(v -> navigateHistory(-1));
        findViewById(R.id.keyDown).setOnClickListener(v -> navigateHistory(1));

        findViewById(R.id.keyCtrlC).setOnClickListener(v -> {
            setCtrlActive(false);
            sendToSession(CTRL_C);
        });
    }

    private void setupInputRow() {
        findViewById(R.id.sendButton).setOnClickListener(v -> submitInput());

        inputView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitInput();
                return true;
            }
            return false;
        });

        // Hardware keyboard: Ctrl modifier produces control codes directly.
        inputView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && ctrlActive
                    && keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
                setCtrlActive(false);
                sendToSession(String.valueOf((char) (keyCode - KeyEvent.KEYCODE_A + 1)));
                return true;
            }
            return false;
        });

        // Soft keyboard: intercept the next typed letter while Ctrl is active.
        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handleCtrlCombo(s);
            }
        });
    }

    private void handleCtrlCombo(Editable s) {
        if (!ctrlActive || editingForCtrl || s.length() == 0) {
            return;
        }
        char c = s.charAt(s.length() - 1);
        int controlCode = -1;
        if (c >= 'a' && c <= 'z') {
            controlCode = c - 'a' + 1;
        } else if (c >= 'A' && c <= 'Z') {
            controlCode = c - 'A' + 1;
        }
        if (controlCode == -1) {
            return;
        }
        editingForCtrl = true;
        s.delete(s.length() - 1, s.length());
        editingForCtrl = false;
        setCtrlActive(false);
        sendToSession(String.valueOf((char) controlCode));
    }

    private void setCtrlActive(boolean active) {
        ctrlActive = active;
        ctrlButton.setSelected(active);
        ctrlButton.setText(active ? "Ctrl *" : "Ctrl");
    }

    private void insertAtCursor(String text) {
        int start = Math.max(inputView.getSelectionStart(), 0);
        int end = Math.max(inputView.getSelectionEnd(), 0);
        inputView.getText().replace(Math.min(start, end), Math.max(start, end), text);
    }

    private void navigateHistory(int direction) {
        List<String> items = history.getAll();
        if (items.isEmpty()) {
            return;
        }
        if (direction < 0) {
            if (historyIndex == -1) {
                historyDraft = inputView.getText().toString();
                historyIndex = items.size() - 1;
            } else if (historyIndex > 0) {
                historyIndex--;
            }
            inputView.setText(items.get(historyIndex));
        } else {
            if (historyIndex == -1) {
                return;
            }
            if (historyIndex < items.size() - 1) {
                historyIndex++;
                inputView.setText(items.get(historyIndex));
            } else {
                historyIndex = -1;
                inputView.setText(historyDraft);
            }
        }
        inputView.setSelection(inputView.getText().length());
    }

    private void submitInput() {
        if (session == null || !session.isRunning()) {
            restartSession();
            return;
        }
        String command = inputView.getText().toString();
        if (!command.isEmpty()) {
            history.add(command);
        }
        session.sendCommand(command);
        inputView.setText("");
        historyIndex = -1;
        historyDraft = "";
    }

    private void startSession() {
        sessionExited = false;
        session = new TerminalSession(runtimeManager);
        session.start(outputListener);
    }

    private void restartSession() {
        if (session != null) {
            session.destroy();
            session = null;
        }
        appendOutput("[Restarting shell...]\n");
        startSession();
    }

    private void sendToSession(String text) {
        if (session != null && session.isRunning()) {
            session.sendCommand(text);
        }
    }

    private void appendOutput(String text) {
        if (closing || isDestroyed()) {
            return;
        }
        StringBuilder buffer = new StringBuilder(outputView.getText());
        buffer.append(text);
        if (buffer.length() > MAX_OUTPUT_CHARS) {
            buffer.delete(0, buffer.length() - MAX_OUTPUT_CHARS);
        }
        outputView.setText(buffer);
        outputScroll.post(() -> outputScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void handleSessionExit(int code) {
        if (closing || isDestroyed()) {
            return;
        }
        sessionExited = true;
        appendOutput("\n[Process exited (code " + code + ")] - tap output to restart\n");
        Snackbar.make(rootView, "Shell exited (code " + code + ")", Snackbar.LENGTH_LONG)
                .setAction(R.string.terminal_restart, v -> restartSession())
                .show();
    }

    @Override
    protected void onDestroy() {
        closing = true;
        if (session != null) {
            session.destroy();
            session = null;
        }
        super.onDestroy();
    }
}
