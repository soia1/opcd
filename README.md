# OPCD Android

> OpenCode, running as a self-contained development environment on Android.

OPCD Android is an open-source Android application that brings [OpenCode](https://github.com/anomalyco/opencode) to your phone without requiring Acode, Termux, UserLAnd, Ubuntu, a VPS, or a PC.

## Goal

Open your phone, open OPCD, and start coding with OpenCode — nothing else needed.

## Features (Planned)

- Embedded Linux runtime (no external apps).
- Local OpenCode server with official web UI.
- File manager for project files.
- Code editor with syntax highlighting.
- Real Linux terminal.
- Git support.
- Package management (apk, npm, pip).
- Offline development support.
- Automatic updates for OpenCode and OPCD Android.

## Architecture

```text
OPCD Android
├── Android Native Layer
├── Linux Runtime (PRoot + Alpine Linux)
│   ├── Node.js / npm
│   ├── Python / pip
│   ├── Git
│   └── OpenCode
└── OpenCode UI (via Android WebView)
```

## Status

This project is in the early research and planning phase. See [task.md](task.md) for milestones.

## License

[MIT License](LICENSE)
