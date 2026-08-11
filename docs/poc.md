# OPCD Android — Proof of Concept (Milestone 2)

This document describes the POC for running OpenCode locally on Android using PRoot + Alpine Linux.

## Goal

Prove that OpenCode can be started locally on Android and its web UI can be loaded inside an Android WebView.

## Components

### 1. Linux Runtime Scripts (`scripts/`)

| Script | Purpose |
|--------|---------|
| `setup-alpine-proot.sh` | Downloads PRoot and Alpine Linux rootfs into app private storage. |
| `proot-shell.sh` | Enters the Alpine environment via PRoot. |
| `install-nodejs.sh` | Installs Node.js, npm, Python, pip, Git inside Alpine. |
| `install-opencode.sh` | Installs the OpenCode `musl` ARM64 binary inside Alpine. |
| `start-opencode.sh` | Starts `opencode serve` on `127.0.0.1:4096`. |
| `health-check.sh` | Verifies the OpenCode server is reachable. |

### 2. Android Host (`android/`)

A minimal Android Studio project with:

- `MainActivity` containing a `WebView`.
- WebView configured to load `http://127.0.0.1:4096`.
- Polling logic that waits for the server to become reachable.
- Required permissions in `AndroidManifest.xml`.

## How to Test the POC

### Prerequisites

- Android device or emulator running Android 8.0+ (API 26+), ARM64.
- ADB access (for manual testing).
- Internet access during setup.

### Step 1: Push scripts to the device

```bash
adb shell mkdir -p /data/local/tmp/opcd-scripts
adb push scripts/*.sh /data/local/tmp/opcd-scripts/
adb shell chmod +x /data/local/tmp/opcd-scripts/*.sh
```

### Step 2: Run setup

```bash
adb shell "OPCD_BASE_DIR=/data/data/com.opcd.android/files/runtime /data/local/tmp/opcd-scripts/setup-alpine-proot.sh"
```

### Step 3: Enter Alpine and install tools

```bash
adb shell "OPCD_BASE_DIR=/data/data/com.opcd.android/files/runtime /data/local/tmp/opcd-scripts/proot-shell.sh /data/local/tmp/opcd-scripts/install-nodejs.sh"
adb shell "OPCD_BASE_DIR=/data/data/com.opcd.android/files/runtime /data/local/tmp/opcd-scripts/proot-shell.sh /data/local/tmp/opcd-scripts/install-opencode.sh"
```

### Step 4: Start OpenCode server

```bash
adb shell "OPCD_BASE_DIR=/data/data/com.opcd.android/files/runtime /data/local/tmp/opcd-scripts/proot-shell.sh /data/local/tmp/opcd-scripts/start-opencode.sh"
```

### Step 5: Verify with health check

```bash
adb shell /data/local/tmp/opcd-scripts/health-check.sh
```

### Step 6: Build and install the Android app

```bash
cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Open the OPCD Android app. It should poll until the server is reachable and then load the OpenCode UI.

## Known Issues and Solutions

### Issue 1: Android is not in OpenCode platform map

**Problem:** The `opencode-ai` npm package selects a binary based on `process.platform`. On Android, `os.platform()` returns `android`, which is not mapped.

**Solution:** Force platform to `linux` and architecture to `arm64` during npm install, or download the `opencode-linux-arm64-musl` binary directly. See `install-opencode.sh`.

### Issue 2: glibc binary does not run on Android

**Problem:** The default `opencode-linux-arm64` binary is linked against glibc and fails on Android (Bionic libc).

**Solution:** Use the `musl` binary, which is statically linked against musl libc.

### Issue 3: APK size

**Problem:** Bundling the full Alpine rootfs, Node.js, and OpenCode can exceed 100 MB.

**Solution:** Download the rootfs and tools on first run instead of bundling them in the APK. This is the approach used by the setup scripts.

### Issue 4: Android 12+ background process limits

**Problem:** Android may kill the PRoot / OpenCode process when the app goes to the background.

**Solution:** Use a Foreground Service in the production app to keep the server alive.

### Issue 5: Storage access for projects

**Problem:** Modern Android restricts direct file system access.

**Solution:** Use the Storage Access Framework (SAF) to let users select a project folder, then bind-mount it into the Alpine rootfs.

### Issue 6: WebView cleartext traffic

**Problem:** By default, Android 9+ blocks cleartext HTTP traffic.

**Solution:** Set `android:usesCleartextTraffic="true"` in the manifest for local development. For production, consider a local HTTPS reverse proxy or loopback exemption.

## POC Limitations

- The Android app in this milestone does not yet start the server automatically. It expects the server to be running (started manually via ADB for testing).
- Process lifecycle is not managed.
- No foreground service yet.
- Storage access uses a simple bind mount to `/sdcard`.
- Error handling is minimal.

## Next Steps

Milestone 3 will integrate the server lifecycle into the Android app, add a foreground service, and improve error handling.
