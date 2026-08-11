#!/system/bin/sh
# proot-shell.sh
# Enter the Alpine Linux environment via PRoot.
# Usage: proot-shell.sh [command...]

set -e

BASE_DIR="${OPCD_BASE_DIR:-/data/data/com.opcd.android/files/runtime}"
ROOTFS_DIR="$BASE_DIR/alpine-rootfs"
BIN_DIR="$BASE_DIR/bin"
PROOT="$BIN_DIR/proot"

if [ ! -f "$PROOT" ]; then
  echo "ERROR: PRoot not found. Run setup-alpine-proot.sh first."
  exit 1
fi

if [ ! -f "$ROOTFS_DIR/.setup-done" ]; then
  echo "ERROR: Alpine rootfs not configured. Run setup-alpine-proot.sh first."
  exit 1
fi

# Mount internal storage into the rootfs so OpenCode can access projects.
mkdir -p "$ROOTFS_DIR/root/projects"

exec "$PROOT" \
  -0 \
  -r "$ROOTFS_DIR" \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/root/projects \
  -w /root \
  -H \
  /bin/sh -c "${*:-/bin/sh}"
