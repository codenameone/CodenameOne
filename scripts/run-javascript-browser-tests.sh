#!/usr/bin/env bash
set -euo pipefail

rjb_log() { echo "[run-javascript-browser-tests] $1"; }
usage() {
  cat <<'EOF' >&2
Usage: run-javascript-browser-tests.sh <bundle_dir_or_archive> [reference_dir]

Serves a ParparVM JavaScript bundle, injects browser-side CN1SS log capture,
and then hands the resulting browser log to run-javascript-screenshot-tests.sh.

Environment:
  BROWSER_CMD   Optional command used to launch the browser. It is executed as:
                  URL="<served_url>" LOG_FILE="<browser_log>" sh -c "$BROWSER_CMD"
                The command is expected to keep running long enough for the app
                to emit CN1SS logs.
  CN1_JS_TIMEOUT_SECONDS  Timeout while waiting for CN1SS:SUITE:FINISHED
EOF
}

if [ $# -lt 1 ]; then
  usage
  exit 2
fi

BUNDLE_INPUT="$1"
REFERENCE_DIR="${2:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/javascript/screenshots}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TIMEOUT_SECONDS="${CN1_JS_TIMEOUT_SECONDS:-120}"
PARPAR_DIAG_ENABLED="${CN1_PARPAR_DIAG:-1}"
CLASSIFIER="$SCRIPT_DIR/classify-javascript-parpar-blocker.py"

TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
WORK_DIR="$(mktemp -d "${TMPDIR}/cn1-js-browser-XXXXXX" 2>/dev/null || echo "${TMPDIR}/cn1-js-browser")"
SERVE_DIR="$WORK_DIR/served"
BUNDLE_DIR="$WORK_DIR/bundle"
URL_FILE="$WORK_DIR/url.txt"
LOG_FILE="$WORK_DIR/browser.log"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-${GITHUB_WORKSPACE:-$REPO_ROOT}/artifacts/javascript-browser-tests}"

cleanup() {
  if [ -n "${SERVER_PID:-}" ]; then
    kill "$SERVER_PID" >/dev/null 2>&1 || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
  rm -rf "$WORK_DIR" 2>/dev/null || true
}
trap cleanup EXIT

mkdir -p "$SERVE_DIR" "$ARTIFACTS_DIR" "$BUNDLE_DIR"

write_top_blocker() {
  local source_log="$1"
  if [ ! -f "$source_log" ]; then
    echo "TOP_BLOCKER=missing_log|none|none" >"$ARTIFACTS_DIR/top-blocker.txt"
    return 0
  fi
  if [ -x "$CLASSIFIER" ]; then
    "$CLASSIFIER" "$source_log" >"$ARTIFACTS_DIR/top-blocker.txt" 2>"$ARTIFACTS_DIR/top-blocker.stderr.log" || \
      echo "TOP_BLOCKER=classifier_error|none|none" >"$ARTIFACTS_DIR/top-blocker.txt"
  else
    echo "TOP_BLOCKER=classifier_missing|none|none" >"$ARTIFACTS_DIR/top-blocker.txt"
  fi
}

materialize_bundle() {
  local input="$1"
  local dest="$2"
  if [ -d "$input" ]; then
    cp -R "$input"/. "$dest"/
    return 0
  fi
  if [ ! -f "$input" ]; then
    rjb_log "Bundle input not found: $input" >&2
    return 1
  fi
  case "$input" in
    *.zip|*.war|*.jar)
      unzip -qq "$input" -d "$dest"
      ;;
    *)
      rjb_log "Unsupported bundle input type: $input" >&2
      return 1
      ;;
  esac
}

materialize_bundle "$BUNDLE_INPUT" "$BUNDLE_DIR"

locate_index_root() {
  local root="$1"
  if [ -f "$root/index.html" ]; then
    printf '%s\n' "$root"
    return 0
  fi
  local candidate
  candidate="$(find "$root" -type f -name index.html | head -n 1 || true)"
  if [ -z "$candidate" ]; then
    return 1
  fi
  dirname "$candidate"
}

BUNDLE_ROOT="$(locate_index_root "$BUNDLE_DIR" || true)"
if [ -z "$BUNDLE_ROOT" ]; then
  rjb_log "Could not locate index.html in bundle input $BUNDLE_INPUT" >&2
  exit 3
fi

cp -R "$BUNDLE_ROOT"/. "$SERVE_DIR"/

INDEX_HTML="$SERVE_DIR/index.html"
if [ ! -f "$INDEX_HTML" ]; then
  rjb_log "Bundle directory does not contain index.html: $INDEX_HTML" >&2
  exit 3
fi

python3 - "$INDEX_HTML" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text(encoding="utf-8")
probe = '<script src="/__cn1__/probe.js"></script>'
bridge = '<script src="browser_bridge.js"></script>'
if probe in text:
    raise SystemExit(0)
if bridge in text:
    text = text.replace(bridge, probe + "\n" + bridge)
elif "</body>" in text:
    text = text.replace("</body>", probe + "\n</body>")
else:
    text += "\n" + probe + "\n"
path.write_text(text, encoding="utf-8")
PY

python3 "$SCRIPT_DIR/javascript_browser_harness.py" \
  --serve-dir "$SERVE_DIR" \
  --log-file "$LOG_FILE" \
  --url-file "$URL_FILE" \
  >"$ARTIFACTS_DIR/browser-harness-url.txt" 2>"$ARTIFACTS_DIR/browser-harness-stderr.log" &
SERVER_PID=$!

for _ in $(seq 1 50); do
  [ -s "$URL_FILE" ] && break
  sleep 0.2
done

if [ ! -s "$URL_FILE" ]; then
  rjb_log "Failed to start browser harness server" >&2
  exit 4
fi

URL="$(cat "$URL_FILE")"
EXTRA_QUERY="${CN1_JS_URL_QUERY:-}"
if [ -n "$EXTRA_QUERY" ]; then
  EXTRA_QUERY="${EXTRA_QUERY#\?}"
  if [[ "$URL" == *\?* ]]; then
    URL="${URL}&${EXTRA_QUERY}"
  else
    URL="${URL}?${EXTRA_QUERY}"
  fi
fi
if [ "$PARPAR_DIAG_ENABLED" != "0" ]; then
  if [[ "$URL" == *\?* ]]; then
    URL="${URL}&parparDiag=1"
  else
    URL="${URL}?parparDiag=1"
  fi
fi
rjb_log "Browser harness serving $URL"

if [ -n "${BROWSER_CMD:-}" ]; then
  URL="$URL" LOG_FILE="$LOG_FILE" sh -c "$BROWSER_CMD" \
    >"$ARTIFACTS_DIR/browser-launch.log" 2>&1 &
  BROWSER_PID=$!
else
  rjb_log "BROWSER_CMD is not set. Open $URL manually, then rerun with a browser command for automation." >&2
  cp -f "$ARTIFACTS_DIR/browser-harness-url.txt" "$ARTIFACTS_DIR/browser-url.txt" 2>/dev/null || true
  exit 2
fi

START_TIME="$(date +%s)"
while true; do
  if [ -f "$LOG_FILE" ] && grep -q "CN1SS:SUITE:FINISHED" "$LOG_FILE"; then
    rjb_log "Detected CN1SS completion marker"
    break
  fi
  NOW="$(date +%s)"
  if [ $((NOW - START_TIME)) -ge "$TIMEOUT_SECONDS" ]; then
    rjb_log "Timed out waiting for CN1SS:SUITE:FINISHED" >&2
    cp -f "$LOG_FILE" "$ARTIFACTS_DIR/browser.log" 2>/dev/null || true
    write_top_blocker "$ARTIFACTS_DIR/browser.log"
    rjb_log "Top blocker: $(cat "$ARTIFACTS_DIR/top-blocker.txt" 2>/dev/null || echo 'TOP_BLOCKER=unavailable|none|none')"
    # Dump tail diagnostics so root cause is visible without downloading the 80MB browser log.
    {
      echo "=== LAST 200 CN1SS lines ==="
      grep -E 'CN1SS:' "$ARTIFACTS_DIR/browser.log" 2>/dev/null | tail -200
      echo "=== LAST 50 PARPAR FALLBACK lines ==="
      grep -E 'PARPAR:DIAG:FALLBACK:' "$ARTIFACTS_DIR/browser.log" 2>/dev/null | tail -50
      echo "=== LAST 30 VIRTUAL_FAIL lines ==="
      grep -E 'PARPAR:DIAG:VIRTUAL_FAIL' "$ARTIFACTS_DIR/browser.log" 2>/dev/null | tail -30
    } >"$ARTIFACTS_DIR/timeout-tail.log" 2>&1 || true
    rjb_log "Timeout tail diagnostics written to $ARTIFACTS_DIR/timeout-tail.log"
    exit 5
  fi
  sleep 1
done

wait "$BROWSER_PID" 2>/dev/null || true
cp -f "$LOG_FILE" "$ARTIFACTS_DIR/browser.log" 2>/dev/null || true
write_top_blocker "$ARTIFACTS_DIR/browser.log"
rjb_log "Top blocker: $(cat "$ARTIFACTS_DIR/top-blocker.txt" 2>/dev/null || echo 'TOP_BLOCKER=unavailable|none|none')"

"$SCRIPT_DIR/run-javascript-screenshot-tests.sh" "$LOG_FILE" "$REFERENCE_DIR"
