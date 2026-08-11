#!/bin/sh
# start-opencode.sh
# Start OpenCode server inside PRoot + Alpine.
# This script runs inside PRoot (Alpine shell).

set -e

HOST="${OPENCODE_HOST:-127.0.0.1}"
PORT="${OPENCODE_PORT:-4096}"
PROJECT_DIR="${OPENCODE_PROJECT_DIR:-/root/projects}"

echo "[OPCD-POC] Ensuring project directory exists: $PROJECT_DIR"
mkdir -p "$PROJECT_DIR"

echo "[OPCD-POC] Starting OpenCode server on $HOST:$PORT ..."
cd "$PROJECT_DIR"

exec opencode serve --hostname "$HOST" --port "$PORT"
