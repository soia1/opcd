# Changelog

All notable changes to OPCD Android will be documented in this file.

## [Unreleased]

### Added
- Initial project setup.
- Product Requirements Document (PRD).
- Task plan with milestones.
- Research notes for Milestone 1.
- Proof of Concept (Milestone 2):
  - Scripts to set up PRoot + Alpine Linux on Android.
  - Scripts to install Node.js, npm, Python, pip, Git.
  - Script to install OpenCode musl ARM64 binary.
  - Script to start OpenCode server on localhost.
  - Health check script.
  - Minimal Android project with WebView to load OpenCode UI.
  - POC documentation (`docs/poc.md`).
- Android Host Application (Milestone 3):
  - `OpcdApplication` with notification channel setup.
  - `OpenCodeService` foreground service for Linux runtime lifecycle.
  - `MainActivity` with WebView, service binding, and status UI.
  - `PermissionHelper` for runtime permissions.
  - `SettingsActivity` with basic preferences.
  - Menu actions: reload, stop server, settings.
- Linux Runtime Integration (Milestone 4):
  - `RuntimeManager`: downloads PRoot, Alpine rootfs, Node.js, npm, Python, Git, OpenCode.
  - `CommandRunner`: execute commands inside Alpine runtime.
  - Setup flow in `MainActivity` for first-run installation.
  - `OpenCodeService` updated to use `RuntimeManager`.
- Terminal Implementation (Milestone 5):
  - `TerminalActivity`: dark monospace terminal UI with output cap and auto-scroll.
  - `TerminalSession`: persistent interactive `/bin/sh` session over PRoot pipes.
  - `ShellHistory`: persisted command history (max 500 entries).
  - Special-key row: Tab, Esc, Ctrl, Up, Down, Ctrl+C.
  - Terminal entry added to main menu and manifest.

## [0.0.1] - 2026-08-11

### Added
- Created repository structure.
- Added initial documentation files.
