#!/bin/bash
# Runs the Autobahn|Testsuite fuzzing client against this server's websocket
# support, on whichever arm is asked for.
#
#   ws-conformance.sh [--arm javase|native] [--write-manifest]
#
# Autobahn is the only thing that really tests a frame parser. Its ~500 cases are
# almost entirely frames no conformant client produces -- reserved bits, truncated
# UTF-8 split across fragments, close codes outside the permitted set, lengths
# spelled in a wider form than they need -- and each one is checked both for the
# answer and for how the connection ended.
#
# The gate is in conformance/check-autobahn.py and has no per-case tolerance:
# every case must be strictly green, NON-STRICT included, and the set of cases
# that ran has to match a committed manifest so the suite cannot quietly shrink to
# the ones that happen to pass.
#
# Environment:
#   CN1_BACKEND_JAVA   the JDK to compile and run the Java SE arm with
#   JDK_8_HOME         required by build.sh for the native arm
#   CN1_WS_PORT        the port the echo server binds (default 9001)
#   CN1_WS_HOST        the address the CONTAINER dials (default 127.0.0.1)
#
# That last one exists because `--network host` does not mean the same thing
# everywhere. On Linux -- which is what CI runs -- the container shares the host's
# network stack and 127.0.0.1 is the server. On macOS, podman and Docker Desktop
# both run in a VM, so `--network host` is the VM's loopback and the server is
# unreachable: the fuzzing client connects to nothing, writes no report, and exits
# 0. Set CN1_WS_HOST=host.containers.internal (podman) or host.docker.internal
# (Docker Desktop) to run it there.
set -euo pipefail
cd "$(dirname "$0")"

ARM="javase"
WRITE_MANIFEST=""
while [ $# -gt 0 ]; do
    case "$1" in
        --arm) ARM="$2"; shift 2 ;;
        --write-manifest) WRITE_MANIFEST="--write-manifest"; shift ;;
        *) echo "usage: ws-conformance.sh [--arm javase|native] [--write-manifest]" >&2; exit 2 ;;
    esac
done

# Autobahn's 9.* cases send messages up to 16MB to measure throughput, and the
# server's default ceiling is 8MB -- so without this the echo server correctly
# answers 1009 and Autobahn correctly calls that a failure. The limit is a
# deployment choice, not a protocol rule, and a conformance run is testing the
# protocol.
export CN1_WS_MAX_MESSAGE_MB="${CN1_WS_MAX_MESSAGE_MB:-32}"

PORT="${CN1_WS_PORT:-9001}"
TARGET_HOST="${CN1_WS_HOST:-127.0.0.1}"
# SIZED FOR THE SUITE, not left at a default. On the thread-pool arm a websocket
# holds its worker for the life of the connection, so the worker count is the
# ceiling on concurrent connections -- and Autobahn leaves some of its ~300 open,
# each holding a worker until the idle timeout. With eight workers the run reached
# case 9.4.4 and then every remaining case was REFUSED, with the server still
# running and still reporting itself healthy. That is not a protocol failure and
# the report cannot tell you so.
WORKERS="${CN1_WS_WORKERS:-64}"
# Bound to every interface when the client is dialling in from a VM, and to
# loopback otherwise -- a conformance run should not expose a port to the network
# unless reaching it requires that.
BIND_HOST="127.0.0.1"
if [ "$TARGET_HOST" != "127.0.0.1" ] && [ "$TARGET_HOST" != "localhost" ]; then
    BIND_HOST="0.0.0.0"
fi
REPORTS="$(pwd)/target/autobahn/$ARM"
MANIFEST="$(pwd)/conformance/autobahn-cases.txt"
rm -rf "$REPORTS"
mkdir -p "$REPORTS"

# Probed with `info`, not just `command -v`. An installed docker binary whose
# daemon is not running is on PATH exactly like a working one, and `docker run`
# then fails with exit 125 -- which, under set -e, ends this script immediately
# after the line that says it is about to start, and looks like a hang.
DOCKER=""
for candidate in "${CN1_CONTAINER_RUNTIME:-}" docker podman; do
    [ -n "$candidate" ] || continue
    if command -v "$candidate" >/dev/null 2>&1 && "$candidate" info >/dev/null 2>&1; then
        DOCKER="$candidate"
        break
    fi
done
if [ -z "$DOCKER" ]; then
    echo "ws-conformance: no working container runtime (tried docker and podman)." >&2
    echo "  Both may be installed without a running daemon; start one and retry." >&2
    exit 3
fi
echo "ws-conformance: container runtime: $DOCKER"

SERVER_PID=""
cleanup() {
    if [ -n "$SERVER_PID" ]; then
        kill "$SERVER_PID" 2>/dev/null || true
        wait "$SERVER_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

case "$ARM" in
    javase)
        JAVA_HOME_DIR="${CN1_BACKEND_JAVA:-${JAVA17_HOME:-}}"
        if [ -n "$JAVA_HOME_DIR" ] && [ -x "$JAVA_HOME_DIR/bin/javac" ]; then
            JAVAC="$JAVA_HOME_DIR/bin/javac"; JAVA="$JAVA_HOME_DIR/bin/java"
        else
            JAVAC="$(command -v javac)"; JAVA="$(command -v java)"
        fi
        OUT="$(pwd)/target/wsecho-classes"
        rm -rf "$OUT"; mkdir -p "$OUT"
        find src impl/javase demo/wsecho -name '*.java' -print0 \
            | xargs -0 "$JAVAC" -nowarn -d "$OUT"
        "$JAVA" -cp "$OUT" com.demo.WsEcho --port "$PORT" --host "$BIND_HOST" \
            --workers "$WORKERS" &
        SERVER_PID=$!
        ;;
    native)
        : "${JDK_8_HOME:?build.sh requires JDK_8_HOME by name}"
        BIN="$(pwd)/target/wsecho-native"
        CN1_BACKEND_DEMO=demo/wsecho CN1_BACKEND_SQLITE=0 \
            CN1_BACKEND_STANDALONE_DEMO=1 ./build.sh WsEcho com.demo "$BIN"
        "$BIN" --port "$PORT" --host "$BIND_HOST" --workers "$WORKERS" &
        SERVER_PID=$!
        ;;
    *)
        echo "ws-conformance: unknown arm '$ARM'" >&2
        exit 2
        ;;
esac

# Wait for the listener rather than sleeping: a fixed delay is either too short on
# a loaded runner or wasted on an idle one.
for attempt in $(seq 1 100); do
    if (exec 3<>"/dev/tcp/127.0.0.1/$PORT") 2>/dev/null; then
        exec 3<&- 2>/dev/null || true
        break
    fi
    sleep 0.1
done

# Sections 12 and 13 are permessage-deflate (RFC 7692), which this server does not
# implement. Autobahn reports all 216 of them as UNIMPLEMENTED -- an accurate
# description of a documented limitation, not a defect.
#
# EXCLUDED rather than tolerated, and the difference matters. Accepting
# UNIMPLEMENTED as a pass would also accept it for a case that USED to work, which
# is exactly the regression this suite exists to catch. Excluding them means every
# case that runs must be strictly green, and the committed manifest records which
# ones those are -- so when deflate lands, deleting this line grows the manifest by
# 216 entries in a diff somebody reads.
EXCLUDED='"12.*", "13.*"'
if [ "${CN1_WS_DEFLATE:-0}" = "1" ]; then
    EXCLUDED=""
fi

# WRITTEN HERE rather than committed: the port and the host are both settable, and
# a spec file with them baked in disagrees with the server the moment either moves
# -- silently, because the fuzzing client reports "no cases run" as success.
cat > "$REPORTS/fuzzingclient.json" <<JSON
{
   "outdir": "/reports",
   "servers": [
      {
         "agent": "codenameone-backend-$ARM",
         "url": "ws://$TARGET_HOST:$PORT"
      }
   ],
   "cases": ["*"],
   "exclude-cases": [$EXCLUDED],
   "exclude-agent-cases": {}
}
JSON

echo "ws-conformance: running the Autobahn fuzzing client against the $ARM arm at ws://$TARGET_HOST:$PORT"
# --network host ONLY for the loopback case. On Linux it is what makes 127.0.0.1
# inside the container mean the server outside it. On macOS it does the opposite:
# joining the VM's network namespace removes the host.containers.internal alias
# that is the only way in, and the run then produces no report and exits 0.
#
# Expanded with the ${ARR[@]+...} guard rather than a bare "${ARR[@]}": macOS ships
# bash 3.2, where an EMPTY array expanded under `set -u` is an unbound variable and
# aborts the script. CI runs bash 5 and would never have shown it -- the run simply
# stopped after the line above, on a developer machine only.
NETWORK_ARGS=()
if [ "$TARGET_HOST" = "127.0.0.1" ] || [ "$TARGET_HOST" = "localhost" ]; then
    NETWORK_ARGS=(--network host)
fi
"$DOCKER" run --rm ${NETWORK_ARGS[@]+"${NETWORK_ARGS[@]}"} \
    -v "$REPORTS:/reports" \
    crossbario/autobahn-testsuite \
    wstest -m fuzzingclient -s /reports/fuzzingclient.json

# BEFORE the report is read. A server that exits mid-suite shows up in the report
# as a run of "connection refused" and in the gate as "the suite shrank", which is
# true but describes the symptom two steps from the cause. Observed once on a
# developer machine at load average 12.8, not reproduced in the two full runs
# after it -- and a one-off that reports itself as a coverage failure is exactly
# the kind of thing that gets explained away. Say it plainly instead.
if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "ws-conformance: the $ARM server exited DURING the suite." >&2
    echo "  Cases after that point could not connect, so the gate below will also" >&2
    echo "  report the suite as shrunken. The server's death is the real finding." >&2
    SERVER_DIED=1
else
    SERVER_DIED=0
fi

cleanup
SERVER_PID=""

python3 conformance/check-autobahn.py "$REPORTS" "$MANIFEST" $WRITE_MANIFEST
GATE_STATUS=$?
if [ "$SERVER_DIED" = "1" ]; then
    exit 1
fi
exit $GATE_STATUS
