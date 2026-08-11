#!/system/bin/sh
# health-check.sh
# Check if OpenCode server is running and accessible.

set -e

HOST="${OPENCODE_HOST:-127.0.0.1}"
PORT="${OPENCODE_PORT:-4096}"
URL="http://$HOST:$PORT"

echo "[OPCD-POC] Checking OpenCode server at $URL ..."

if command -v curl >/dev/null 2>&1; then
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$URL" || echo "000")
elif command -v wget >/dev/null 2>&1; then
  if wget -qO- "$URL" >/dev/null 2>&1; then
    STATUS="200"
  else
    STATUS="000"
  fi
else
  echo "ERROR: curl or wget not found."
  exit 1
fi

if [ "$STATUS" = "200" ] || [ "$STATUS" = "302" ] || [ "$STATUS" = "401" ]; then
  echo "[OPCD-POC] Health check PASSED (HTTP $STATUS)."
  exit 0
else
  echo "[OPCD-POC] Health check FAILED (HTTP $STATUS)."
  exit 1
fi
