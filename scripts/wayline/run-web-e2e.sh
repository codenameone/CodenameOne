#!/usr/bin/env bash
#
# The web app, end to end: builds the Wayline server and the browser build of
# the app, starts the server with the app staged where it serves it from, and
# drives the hosted app in a headless browser (web-e2e.mjs).
#
#   run-web-e2e.sh [--native] [--prebuilt]
#
# --native runs the compiled server; --prebuilt skips both builds and uses what
# `backend/server.sh build --web` left.
#
# What it proves: a person who opens the server's address gets the app, can sign
# in as the demo rider and reaches the home screen, with the live channel open.
# Screenshots of each step and the browser's log go to target/web-e2e.
#
# Needs JAVA_HOME on a JDK 17, the Codename One artifacts this project builds
# against in the local Maven repository, and Node with Playwright and its
# Chromium: `npm ci && npx playwright install chromium` in scripts/, or a
# NODE_PATH that has it. Nothing here opens a window.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
PORT="${WAYLINE_WEB_PORT:-18081}"
OUT="${WAYLINE_WEB_OUT:-$HERE/target/web-e2e}"
LOG="$OUT/server.log"
MODE="--jvm"
PREBUILT=0
for arg in "$@"; do
  case "$arg" in
    --native) MODE="--native" ;;
    --prebuilt) PREBUILT=1 ;;
    *) echo "[wayline-web] unknown argument $arg" >&2; exit 2 ;;
  esac
done

if ! command -v node >/dev/null 2>&1; then
  echo "[wayline-web] node is not on the PATH; the browser is driven with Playwright" >&2
  exit 2
fi

rm -rf "$OUT"
mkdir -p "$OUT"

echo "[wayline-web] java: $(java -version 2>&1 | head -1)"
if [ "$PREBUILT" = 0 ]; then
  "$HERE/backend/server.sh" build "$MODE" --web
fi
if [ ! -f "$HERE/backend/target/webapp/index.html" ]; then
  echo "[wayline-web] no web app is staged in backend/target/webapp" >&2
  exit 1
fi

echo "[wayline-web] starting the server on port $PORT"
CN1_PROFILE=test "$HERE/backend/server.sh" run "$MODE" --prebuilt --port "$PORT" >"$LOG" 2>&1 &
SERVER_PID=$!
cleanup() {
  kill "$SERVER_PID" >/dev/null 2>&1 || true
  wait "$SERVER_PID" 2>/dev/null || true
}
trap cleanup EXIT

ready=0
for _ in $(seq 1 90); do
  if curl -fs -o /dev/null "http://localhost:$PORT/oauth2/jwks" 2>/dev/null; then
    ready=1
    break
  fi
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done
if [ "$ready" != "1" ]; then
  echo "[wayline-web] the server did not start; its log:" >&2
  cat "$LOG" >&2 || true
  exit 1
fi

fail() {
  echo "[wayline-web] FAILED: $*" >&2
  exit 1
}

# What no browser is needed to see: the app is at /, the API kept its paths, and
# the script that is most of the download goes out compressed.
headers="$(curl -fsS -D - -o /dev/null "http://localhost:$PORT/")" \
  || fail "the server did not answer at /"
echo "$headers" | grep -qi '^content-type: text/html' || fail "/ is not served as a page"
echo "$headers" | grep -qi '^cache-control: no-cache' || fail "/ is not revalidated"
headers="$(curl -fsS -H 'Accept-Encoding: gzip' -D - -o /dev/null \
    "http://localhost:$PORT/translated_app.js")" || fail "translated_app.js is not served"
echo "$headers" | grep -qi '^content-encoding: gzip' \
  || fail "translated_app.js was not served compressed"
status="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT/api/me")"
[ "$status" = "401" ] || fail "/api/me answered $status without a token, not 401"

set +e
URL="http://localhost:$PORT/" OUT="$OUT" node "$HERE/web-e2e.mjs"
STATUS=$?
set -e

if [ "$STATUS" -ne 0 ]; then
  echo "[wayline-web] the end of the browser's log:" >&2
  tail -n 40 "$OUT/browser.log" >&2 || true
  echo "[wayline-web] the end of the server's log:" >&2
  tail -n 40 "$LOG" >&2 || true
  exit "$STATUS"
fi
echo "[wayline-web] passed; screenshots are in $OUT"
