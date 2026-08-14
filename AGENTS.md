# AGENTS.md — OPCD Android

## Project Overview

OPCD Android is an independent Android application that runs OpenCode as a self-contained development environment on Android phones. It does not depend on Acode, Termux, UserLAnd, Ubuntu, VPS, PC, or external SSH.

## Project Structure

```
/public/1
├── AGENTS.md           # This file
├── OPCD_Android_PRD.md # Product Requirements Document
├── task.md             # Task plan and milestones
├── README.md           # Project overview
├── LICENSE             # MIT License
├── CHANGELOG.md        # Version history
├── CONTRIBUTING.md     # Contribution guidelines
├── docs/               # Documentation
├── src/                # Source code
│   ├── android/        # Android host application code
│   ├── linux-runtime/  # Linux runtime integration (PRoot, rootfs)
│   ├── opencode-integration/ # OpenCode server integration
│   ├── file-manager/   # File manager implementation
│   ├── code-editor/    # Code editor implementation
│   └── terminal/       # Terminal implementation
├── android/            # Android Studio project files
└── scripts/            # Build and setup scripts
```

## Key Technical Decisions

### Linux Runtime

- Use **PRoot** as the user-space chroot engine (no root required).
- PRoot + helper libs ship **inside the APK** as `jniLibs/arm64-v8a/*.so` (fetched by `scripts/fetch-runtime.sh` in CI): Termux's `proot 5.1.107.90` (dynamic), `libproot-loader.so`, `libtalloc.so`, `libandroid-shmem.so`. `libopcd-exec.so` is our own ~40-line MIT `LD_PRELOAD` shim (`android/app/src/main/cpp/opcd_exec_shim.c`, NDK/CMake build) that rewrites `execve()` of app-data paths to `/system/bin/linker64`. Do NOT use Termux's `libtermux-exec.so` — its hardcoded Termux path-mangling breaks PRoot's loader and has no off switch.
- Use **Alpine Linux** rootfs as the base distribution because:
  - Small size (good for mobile).
  - Uses musl libc, which matches the OpenCode `musl` ARM64 binary.
  - Package manager `apk` is fast and simple.
- Runtime components: Node.js, npm, Python, pip, Git, OpenCode.

### OpenCode

- Source: `anomalyco/opencode` (official, MIT licensed).
- Distribution: `opencode-ai` npm package (latest version 1.18.16).
- Binary: `opencode-linux-arm64-musl` is the target binary for Android ARM64.
- Start command: `opencode serve --hostname 127.0.0.1 --port 4096`.
- UI is served locally and displayed in Android WebView.

### Android Host

- Target platform: Android 8.0+ (API 26+).
- **`targetSdk` must stay at 28.** Apps targeting 29+ run in the `untrusted_app` SELinux domain, where Android 10+ neverallows `execute`/`execute_no_trans` on `app_data_file`. That blocks both `execve()` and `mmap(PROT_EXEC)` of files in `/data/data/<pkg>` — the latter is how PRoot's loader maps guest ELF binaries, so PRoot segfaults (exit 139) under targetSdk ≥29. targetSdk 28 keeps the legacy domain where app-private execution is allowed (same reason Termux ships targetSdk 28 on Android 15). We distribute via GitHub APK, so Play's targetSdk floor does not apply.
- Primary architecture: ARM64.
- UI layer: Android WebView loading OpenCode from localhost.
- Storage: Use Android Storage Access Framework (SAF) for project files.

## Development Workflow

1. Read `OPCD_Android_PRD.md` and `task.md` before making changes.
2. Update this file if technical decisions change.
3. Keep changes minimal and focused on the current milestone.
4. Test assumptions with POCs when possible.

## Communication

- Use Arabic for user-facing summaries.
- Use English for code, comments, and technical documentation.
- MIT License applies to all project code.
