#!/usr/bin/env bash
# Runs the headless comparison and prints one line per run:
#
#   <runtime> <cold|warm> start_ms idle_pss_kb idle_private_kb mapped_kb mapped_files \
#       steady_cpu_pct steady_pss_kb peak_rps peak_p99_ms peak_pss_kb
#
#   run.sh <codenameone-binary> [runs]
#
# RUNTIMES (default: every one) names the runtimes to run, space separated; they
# always run in the order below. CI's performance gate runs "codenameone rust go".
#
# Needs a privileged container (docker run --privileged) to drop the page cache
# before a cold start.
#
#   start     process start to the first answered GET /json
#   idle      memory two seconds after that, with nothing to do
#   steady    a constant 200 requests per second for 10 s, which is a busy
#             gateway: CPU over the window, memory at its end
#   peak      as many requests as 32 connections can drive for 10 s
#
# Every server is pinned to cores 0-1 (a small board's share) and the load
# generator to the rest, so the two never compete for a core. Memory is PSS and
# private from /proc/<pid>/smaps_rollup, summed over the process tree; mapped is
# every distinct file the tree maps, counted once at full size.
set -uo pipefail
RUNS="${2:-3}"
RUNTIMES="${RUNTIMES:-codenameone go rust node python spring}"
PORT=8080
URL="http://127.0.0.1:$PORT/json"
SERVER_CPUS=0-1
LOAD_CPUS=2-$(( $(nproc) - 1 ))

# Read from the container's own disk, not a bind mount (see ../run.sh).
mkdir -p /opt/cn1 && cp "$1" /opt/cn1/server

now() { date +%s%3N; }

drop_caches() {
    sync
    echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || { echo "run.sh: cannot drop caches; use --privileged" >&2; exit 2; }
}

tree() {
    local c
    echo "$1"
    for c in $(pgrep -P "$1"); do tree "$c"; done
}

memory() {
    local files="" p
    for p in "$@"; do [ -r /proc/$p/smaps_rollup ] && files="$files /proc/$p/smaps_rollup"; done
    [ -n "$files" ] || { echo "0 0"; return; }
    awk '/^Pss:/{pss+=$2} /^Private_(Clean|Dirty):/{priv+=$2} END{printf "%d %d", pss, priv}' $files
}

footprint() {
    local p
    for p in "$@"; do awk '$6 ~ /^\// {print $6}' /proc/$p/maps 2>/dev/null; done | sort -u \
        | while read -r f; do [ -f "$f" ] && stat -c %s "$f"; done \
        | awk '{s+=$1; n++} END{printf "%d %d", s/1024, n}'
}

cpu_ticks() {
    local p t=0
    for p in "$@"; do t=$((t + $(awk '{print $14 + $15}' /proc/$p/stat 2>/dev/null || echo 0))); done
    echo $t
}

run_one() {
    local name="$1" mode="$2"; shift 2
    case " $RUNTIMES " in *" $name "*) ;; *) return ;; esac
    local t0 pid start pids idle fp c0 c1 steady peak rps p99
    [ "$mode" = cold ] && drop_caches
    t0=$(now)
    PORT=$PORT taskset -c $SERVER_CPUS "$@" >/tmp/server.log 2>&1 &
    pid=$!
    until curl -sf -o /dev/null --max-time 1 "$URL"; do
        kill -0 $pid 2>/dev/null || { echo "$name $mode failed: $(tail -3 /tmp/server.log)"; return; }
        sleep 0.005
    done
    start=$(( $(now) - t0 ))
    sleep 2
    pids=$(tree $pid)
    idle=$(memory $pids)
    fp=$(footprint $pids)

    c0=$(cpu_ticks $(tree $pid))
    taskset -c $LOAD_CPUS oha -q 200 -z 10s --no-tui -c 4 "$URL" >/dev/null 2>&1
    c1=$(cpu_ticks $(tree $pid))
    steady="$(( (c1 - c0) * 100 / 10 / 100 )).$(( (c1 - c0) % 10 ))"
    local steadyMem; steadyMem=$(memory $(tree $pid) | cut -d' ' -f1)

    taskset -c $LOAD_CPUS oha -z 10s --no-tui -c 32 --output-format json "$URL" >/tmp/peak.json 2>/dev/null &
    local loader=$!
    sleep 8
    local peakMem; peakMem=$(memory $(tree $pid) | cut -d' ' -f1)
    wait $loader
    rps=$(python3 -c "import json;print(int(json.load(open('/tmp/peak.json'))['summary']['requestsPerSec']))" 2>/dev/null || echo '?')
    p99=$(python3 -c "import json;print('%.2f' % (json.load(open('/tmp/peak.json'))['latencyPercentiles']['p99'] * 1000))" 2>/dev/null || echo '?')

    for p in $(tree $pid); do kill -TERM $p 2>/dev/null; done
    wait $pid 2>/dev/null
    while kill -0 $pid 2>/dev/null; do sleep 0.2; done
    sleep 2
    echo "$name $mode $start $idle $fp $steady $steadyMem $rps $p99 $peakMem"
}

for i in $(seq "$RUNS"); do
    for mode in cold warm; do
        # The demo's handler is written as an application's is, with no switches.
        run_one codenameone $mode /opt/cn1/server
        run_one go $mode /bench/go/gobench
        run_one rust $mode /bench/rust/target/release/axumbench
        run_one node $mode node /bench/node/server.js
        run_one python $mode /opt/py/bin/uvicorn --app-dir /bench/python --host 127.0.0.1 --port $PORT --log-level warning app:app
        run_one spring $mode /opt/jdk/bin/java -jar /bench/spring/target/springbench.jar --server.port=$PORT
    done
done
