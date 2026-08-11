#!/system/bin/sh
# setup-alpine-proot.sh
# Setup PRoot + Alpine Linux rootfs for OPCD Android POC.
# Run this script inside the Android app data directory.

set -e

# Base directory inside Android app private storage.
BASE_DIR="${OPCD_BASE_DIR:-/data/data/com.opcd.android/files/runtime}"
ROOTFS_DIR="$BASE_DIR/alpine-rootfs"
BIN_DIR="$BASE_DIR/bin"
PROOT_VERSION="5.3.0"
ALPINE_VERSION="v3.19"
ALPINE_ARCH="aarch64"
ALPINE_TARBALL="alpine-minirootfs-${ALPINE_VERSION##v}-$ALPINE_ARCH.tar.gz"

mkdir -p "$BASE_DIR" "$ROOTFS_DIR" "$BIN_DIR"

log() {
  echo "[OPCD-POC] $1"
}

# Download PRoot binary if missing.
if [ ! -f "$BIN_DIR/proot" ]; then
  log "Downloading PRoot $PROOT_VERSION for ARM64..."
  curl -L -o "$BIN_DIR/proot" \
    "https://github.com/proot-me/proot/releases/download/v$PROOT_VERSION/proot-$PROOT_VERSION-aarch64-static"
  chmod +x "$BIN_DIR/proot"
  log "PRoot downloaded."
else
  log "PRoot already exists."
fi

# Download Alpine rootfs if missing.
if [ ! -f "$ROOTFS_DIR/.setup-done" ]; then
  log "Downloading Alpine Linux $ALPINE_VERSION rootfs..."
  curl -L -o "$BASE_DIR/$ALPINE_TARBALL" \
    "https://dl-cdn.alpinelinux.org/alpine/$ALPINE_VERSION/releases/$ALPINE_ARCH/$ALPINE_TARBALL"

  log "Extracting rootfs..."
  tar -xzf "$BASE_DIR/$ALPINE_TARBALL" -C "$ROOTFS_DIR"
  rm "$BASE_DIR/$ALPINE_TARBALL"

  log "Configuring Alpine..."
  # Set up DNS
  printf 'nameserver 8.8.8.8\nnameserver 1.1.1.1\n' > "$ROOTFS_DIR/etc/resolv.conf"

  # Configure apk repositories
  printf 'https://dl-cdn.alpinelinux.org/alpine/%s/main\n' "$ALPINE_VERSION" > "$ROOTFS_DIR/etc/apk/repositories"
  printf 'https://dl-cdn.alpinelinux.org/alpine/%s/community\n' "$ALPINE_VERSION" >> "$ROOTFS_DIR/etc/apk/repositories"

  touch "$ROOTFS_DIR/.setup-done"
  log "Alpine rootfs ready."
else
  log "Alpine rootfs already configured."
fi

log "Setup complete. Rootfs: $ROOTFS_DIR"
