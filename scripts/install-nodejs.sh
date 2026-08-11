#!/bin/sh
# install-nodejs.sh
# Install Node.js and npm inside Alpine Linux rootfs.
# This script runs inside PRoot (Alpine shell).

set -e

echo "[OPCD-POC] Updating apk indexes..."
apk update

echo "[OPCD-POC] Installing Node.js, npm, Python, pip, Git..."
apk add --no-cache nodejs npm python3 py3-pip git bash curl wget

echo "[OPCD-POC] Verifying installations..."
node --version
npm --version
python3 --version
git --version

echo "[OPCD-POC] Node.js and tools installed successfully."
