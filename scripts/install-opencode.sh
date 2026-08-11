#!/bin/sh
# install-opencode.sh
# Install OpenCode inside Alpine Linux rootfs for Android ARM64.
# This script runs inside PRoot (Alpine shell).

set -e

OPCD_HOME="${OPCD_HOME:-/root}"
NODE_MODULES_DIR="$OPCD_HOME/.opencode-node-modules"
VERSION="${OPENCODE_VERSION:-1.18.16}"

echo "[OPCD-POC] Installing OpenCode $VERSION for Android ARM64 (musl)..."

# Method 1: Try npm install with forced platform/arch override.
# The opencode-ai wrapper uses optionalDependencies with platform-specific binaries.
# Android is not in the platform map, so we force linux + arm64 and then fix the binary.
mkdir -p "$NODE_MODULES_DIR"
cd "$NODE_MODULES_DIR"

npm install opencode-ai@$VERSION --no-save --foreground-scripts \
  --platform=linux --arch=arm64 --libc=musl 2>/dev/null || {
    echo "[OPCD-POC] npm install with flags failed, trying direct binary download..."
}

# Method 2: Fallback to direct binary download.
BIN_DIR="$NODE_MODULES_DIR/.bin"
mkdir -p "$BIN_DIR"

if [ ! -f "$BIN_DIR/opencode" ]; then
  BINARY_NAME="opencode-linux-arm64-musl"
  DOWNLOAD_URL="https://registry.npmjs.org/opencode-ai/-/${BINARY_NAME}-${VERSION}.tgz"
  echo "[OPCD-POC] Downloading $BINARY_NAME..."
  curl -L -o "/tmp/opencode-bin.tgz" "$DOWNLOAD_URL" || {
    echo "[OPCD-POC] Could not download from npm registry. Trying GitHub releases..."
    curl -L -o "/tmp/opencode-bin.tgz" \
      "https://github.com/anomalyco/opencode/releases/download/opencode-v${VERSION}/${BINARY_NAME}.tar.gz" || true
  }

  if [ -f "/tmp/opencode-bin.tgz" ]; then
    tar -xzf "/tmp/opencode-bin.tgz" -C "$BIN_DIR"
    chmod +x "$BIN_DIR/opencode"
    rm "/tmp/opencode-bin.tgz"
  fi
fi

if [ -f "$BIN_DIR/opencode" ]; then
  "$BIN_DIR/opencode" --version
  echo "[OPCD-POC] OpenCode installed at $BIN_DIR/opencode"
else
  echo "[OPCD-POC] ERROR: OpenCode binary could not be installed."
  exit 1
fi

# Create a wrapper script in /usr/local/bin for easier access.
mkdir -p /usr/local/bin
cat > /usr/local/bin/opencode <<EOF
#!/bin/sh
exec "$BIN_DIR/opencode" "\$@"
EOF
chmod +x /usr/local/bin/opencode

echo "[OPCD-POC] OpenCode wrapper installed at /usr/local/bin/opencode"
