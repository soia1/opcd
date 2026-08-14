#!/bin/sh
# Fetch the PRoot static binary and place it as an APK native library so that
# Android extracts it to the app's nativeLibraryDir, the only app-owned storage
# from which execve() is allowed on Android 10+ (SELinux app_data_file neverallow).
#
# Usage: ./scripts/fetch-proot.sh
set -e

PROOT_VERSION="5.3.0"
ASSET="proot-v${PROOT_VERSION}-aarch64-static"
URL="https://github.com/proot-me/proot/releases/download/v${PROOT_VERSION}/${ASSET}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEST_DIR="${SCRIPT_DIR}/../android/app/src/main/jniLibs/arm64-v8a"
DEST="${DEST_DIR}/libproot.so"

mkdir -p "$DEST_DIR"
echo "Fetching PRoot from ${URL} ..."
if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$URL" -o "$DEST"
else
    wget -q "$URL" -O "$DEST"
fi
chmod +x "$DEST"
echo "Wrote ${DEST} ($(wc -c < "$DEST") bytes)"