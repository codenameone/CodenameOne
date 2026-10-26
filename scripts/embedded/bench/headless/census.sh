#!/usr/bin/env bash
# Prints the heap bytes the backend allocates per request, as one line:
#
#   bytes_per_request objects_per_request
#
#   census.sh <codenameone-binary built with CN1_BACKEND_CFLAGS=-DCN1_ALLOC_CENSUS> [requests]
#
# The census counts every object the program allocates and prints the totals
# when it exits (CN1_HEAP_REPORT asks for that report). It runs the server twice: once answering a single request and
# once answering that one plus [requests] more (default 200,000) over the same
# 32 connections run.sh's peak load uses. The difference is what the requests
# cost, with startup and connection setup cancelled out, so the figure is exact
# rather than sampled and does not depend on how fast the machine is.
set -uo pipefail
BIN="$1"
REQUESTS="${2:-200000}"
PORT=8080
URL="http://127.0.0.1:$PORT/json"

# Prints "<objects> <bytes>" from the census the server writes when it exits.
serve() {
    local count="$1" pid line
    CN1_HEAP_REPORT=1 PORT=$PORT "$BIN" >/tmp/census.log 2>&1 &
    pid=$!
    until curl -sf -o /dev/null --max-time 1 "$URL"; do
        kill -0 $pid 2>/dev/null || { echo "census.sh: server died: $(tail -3 /tmp/census.log)" >&2; exit 1; }
        sleep 0.01
    done
    if [ "$count" -gt 0 ]; then
        oha -n "$count" -c 32 --no-tui "$URL" >/dev/null 2>&1 \
            || { echo "census.sh: load generator failed" >&2; exit 1; }
    fi
    kill -TERM $pid
    wait $pid 2>/dev/null
    line=$(grep -m1 '^\[ALLOC:exit\] ' /tmp/census.log) \
        || { echo "census.sh: no exit census; was the binary built with -DCN1_ALLOC_CENSUS?" >&2; exit 1; }
    echo "$line" | awk '{print $2, $4}'
}

# Plain assignments, not process substitution, so a failed run stops the script.
base=$(serve 0) || exit 1
loaded=$(serve "$REQUESTS") || exit 1
read -r base_objects base_bytes <<<"$base"
read -r objects bytes <<<"$loaded"
awk -v b="$((bytes - base_bytes))" -v o="$((objects - base_objects))" -v n="$REQUESTS" \
    'BEGIN { printf "%.1f %.2f\n", b / n, o / n }'
