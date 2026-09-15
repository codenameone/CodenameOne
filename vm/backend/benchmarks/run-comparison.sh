#!/bin/bash
# Compares the Codename One backend against a Go net/http server of the same
# shape, on Linux, on the same machine, under the same load.
#
# Everything here is arranged so that what differs between the two runs is the
# runtime and nothing else:
#
#   Same host        Both binaries run on the same Linux kernel, from the same
#                    filesystem, one after the other -- never at the same time.
#   Same CPUs        Both are pinned to cores 0-1; the load generator is pinned
#                    to cores 2-3, so the client cannot steal the server's CPU
#                    and the two servers get the same share.
#   Same handlers    demo/bench and bench-server.go implement the same two
#                    routes, and both SERIALISE the JSON per request rather than
#                    returning a constant.
#   Same client      One wrk process, same thread and connection counts, same
#                    duration, keep-alive in both cases. wrk runs in a container
#                    on the host network -- it is a normal process on this
#                    machine, so taskset pins it like anything else.
#   Warm             Every measured run is preceded by an unmeasured one, so
#                    neither runtime is charged for its first-request costs.
#
# What is NOT equal, and is reported rather than hidden:
#   - Go's response omits `Connection: keep-alive` (it is the HTTP/1.1 default);
#     ours sends it, which is 24 bytes more per response.
#   - Go's JSON encoder appends a newline, so its body is one byte longer.
#   - Go's net/http is the standard library, not fasthttp. fasthttp is faster,
#     so nothing measured here supports a claim about "Go" in general -- only
#     about Go's standard HTTP server.
set -u
BENCH_DIR="${BENCH_DIR:-/var/tmp/cn1bench}"
DURATION="${DURATION:-20s}"
SERVER_CPUS="${SERVER_CPUS:-0,1}"
CLIENT_CPUS="${CLIENT_CPUS:-2,3}"
WORKERS="${WORKERS:-16}"
# One P per pinned core. The machine has more CPUs than this run is
# allowed to touch, and Go sizing itself to the machine rather than to the
# affinity mask would be a handicap this harness imposed on it.
GOMAXPROCS="${GOMAXPROCS:-2}"
cd "$BENCH_DIR"

# One measurement at a time, enforced rather than assumed. This script kills any
# running server by name before each reading, so a second copy started while this
# one is measuring does not merely compete for CPU -- it kills this one's server
# mid-run. The number that comes back is unremarkable (114k where a clean run
# gives 216k) and nothing in the output hints that anything went wrong, so a
# stray concurrent run silently rewrote a whole results table once.
exec 9>"$BENCH_DIR/.bench.lock"
if ! flock -n 9; then
    echo "another benchmark holds $BENCH_DIR/.bench.lock; refusing to measure" >&2
    exit 3
fi

report() { printf '%-14s %-11s %-6s %12s %10s %10s %10s %10s\n' "$@"; }

# wrk, in a container on the host network, pinned to the client cores. Latency
# percentiles come from wrk's own --latency output rather than being derived
# here, because a percentile computed from a summary is not a percentile.
LOAD_IMAGE="${LOAD_IMAGE:-cn1-bench-load}"
LOAD_THREADS="${LOAD_THREADS:-2}"
load() {
    local conns="$1" duration="$2" target="$3"
    podman run --rm --platform linux/arm64 --network host "$LOAD_IMAGE" -c \
        "taskset -c $CLIENT_CPUS wrk -t$LOAD_THREADS -c$conns -d$duration --latency http://127.0.0.1:$target"
}

start_server() {
    local bin="$1" port="$2"; shift 2
    env PORT="$port" "$@" taskset -c "$SERVER_CPUS" ./"$bin" > "$bin.out" 2>&1 &
    SERVER_PID=$!
    local tries=0
    while ! (exec 3<>/dev/tcp/127.0.0.1/"$port") 2>/dev/null; do
        tries=$((tries + 1))
        if [ $tries -gt 3000 ] || ! kill -0 $SERVER_PID 2>/dev/null; then
            echo "$bin did not come up"; cat "$bin.out"; return 1
        fi
    done
    return 0
}

stop_server() {
    kill $SERVER_PID 2>/dev/null
    wait $SERVER_PID 2>/dev/null
    sleep 1
}

rss_kb() {
    grep VmRSS "/proc/$SERVER_PID/status" 2>/dev/null | awk '{print $2}'
}

# One measured run. bombardier's own summary is parsed rather than re-derived.
measure() {
    local label="$1" bin="$2" port="$3" route="$4" conns="$5"; shift 5
    start_server "$bin" "$port" "$@" || return 1
    local idle
    idle="$(rss_kb)"
    # Unmeasured warm-up: first-request costs belong to neither runtime's steady
    # state, and both get one.
    load "$conns" 5s "$port$route" > /dev/null 2>&1
    local out
    out="$(load "$conns" "$DURATION" "$port$route" 2>&1)"
    local loaded
    loaded="$(rss_kb)"
    local rps p50 p99
    rps="$(echo "$out" | awk '/^Requests\/sec:/ {printf "%.0f", $2}')"
    p50="$(echo "$out" | awk '/^ *50%/ {print $2}')"
    p99="$(echo "$out" | awk '/^ *99%/ {print $2}')"
    stop_server
    report "$label" "$route" "$conns" "${rps:-?}" "${p50:-?}" "${p99:-?}" \
        "${idle:-?}kB" "${loaded:-?}kB"
}

echo "duration=$DURATION server-cpus=$SERVER_CPUS client-cpus=$CLIENT_CPUS workers=$WORKERS"
echo
report RUNTIME ROUTE CONNS REQS/SEC P50 P99 IDLE-RSS LOADED-RSS
report -------------- ----------- ------ ------------ ---------- ---------- ---------- ---------
port=9400
for route in /plaintext /json; do
    for conns in 16 64 256; do
        measure "go net/http" bench-go "$port" "$route" "$conns" "GOMAXPROCS=$GOMAXPROCS"
        port=$((port + 1))
        # fasthttp as well as net/http, and this arm is not optional.
        # net/http is what "Go" means to most people, but it is NOT what anyone
        # who cares about throughput deploys, so a number measured only against
        # the standard library is a favourable comparison rather than a result --
        # the first reader to run valyala/fasthttp themselves would take it
        # apart. Same two routes, same shape, same static-stripped build, and
        # GOMAXPROCS pinned like the other Go arm.
        measure "go fasthttp" bench-fasthttp "$port" "$route" "$conns" "GOMAXPROCS=$GOMAXPROCS"
        port=$((port + 1))
        # WORKERS=match gives one worker per connection, which is the shape Go
        # net/http has (a goroutine per connection). It is measured because it
        # sounds right and is not: past 64 connections the per-thread cost of the
        # conservative stack scan overwhelms it. See the README.
        w=$WORKERS; [ "$WORKERS" = match ] && w=$conns
        measure "codename one" bench-cn1 "$port" "$route" "$conns" "WORKERS=$w"
        port=$((port + 1))
    done
done
exit 0
