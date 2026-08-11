# OPCD Android — Terminal Implementation

## Overview

The terminal runs a real interactive /bin/sh inside the Alpine Linux runtime via PRoot. Commands typed by the user are executed in the shell and output streams back into the UI.

## Architecture

    TerminalActivity (UI)
          |
    TerminalSession (persistent sh process via PRoot)
          |
    PRoot -> Alpine rootfs -> /bin/sh

- TerminalActivity: renders output in a monospace TextView inside a ScrollView; input via EditText; special-key row for Tab, Esc, Ctrl, Up, Down, Ctrl+C.
- TerminalSession: spawns one long-lived PRoot process running /bin/sh. sendCommand writes a line to stdin; a reader thread streams stdout/stderr chunks to the UI.
- ShellHistory: persists command history to a text file (max 500 entries).

## Design Decisions

Research compared four approaches:

1. Termux terminal-view — full VT100 emulator, but tightly coupled to Termux PTY/JNI model. Overkill for the POC.
2. JackPal Android-Terminal-Emulator — archived since 2022, also PTY-based. Not recommended.
3. WebView + xterm.js — good fidelity, but needs a bridge server. Possible future upgrade.
4. Plain Android views (chosen) — zero dependencies, works directly with process pipes, matches the POC goals.

Consequences of the plain-view approach:

- TERM is set to "dumb" so programs do not emit ANSI escape codes.
- No ANSI/cursor handling: interactive full-screen programs (vim, htop) will not render correctly.
- No real PTY: Ctrl+C is sent as a control character over the pipe rather than raising SIGINT in a foreground process group; this works for most line-oriented cases but not all.
- Output buffer is capped at 200,000 characters to avoid OOM.

## Special Keys

Because the backend API is line-based (sendCommand appends a newline):

- Tab: inserts a literal tab into the input field.
- Esc: clears the input field.
- Ctrl: toggles a modifier; the next typed letter is converted to its control code.
- Ctrl+C: sends the ETX control character (0x03).
- Up/Down: navigate command history.

## Known Limitations

- No tab completion (line-buffered shell without PTY).
- Interactive programs are not supported.
- Hardware keyboard support is basic.

## Future Work

- Add a real PTY via JNI (like Termux libtermux) for SIGINT and interactive apps.
- Optional xterm.js WebView renderer for full terminal fidelity.
- ANSI color stripping or rendering.
