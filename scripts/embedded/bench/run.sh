#!/usr/bin/env bash
# Runs every workload and prints one line per run:
#
#   <framework> <cold|warm> first_frame_ms fps cpu_pct pss_kb private_kb rss_kb mapped_kb mapped_files
#
#   run.sh <codenameone-binary-dir> [seconds] [runs]
#
# Needs a privileged container (docker run --privileged): a cold start drops the
# page cache first, so every executable, library, plugin and font is read from
# disk again -- which is what the first start after boot costs, and where a large
# set of dynamic libraries shows. A warm start follows straight after.
#
# Memory is sampled from /proc/<pid>/smaps_rollup half way through the run:
#   PSS      every page the process maps, shared pages divided among the
#            processes sharing them -- the honest "how much RAM is this" number,
#            and the only one that sums correctly over a process tree;
#   private  pages no other process shares (heap, stacks, written data);
#   RSS      every resident page, shared ones counted in full.
# Footprint is every FILE the process has mapped at that moment (executable,
# shared libraries, dlopen'ed plugins, fonts, locale data), summed by size once
# per file -- what has to be on the device for it to run, whatever it is called.
# For Chromium all of these are taken over its whole process tree.
set -uo pipefail
SECS="${2:-10}"
RUNS="${3:-3}"
# Every other workload is built into the image; read Codename One from the
# container's own disk too, not from a bind mount, whose cold reads go through
# the host's file sharing and cost seconds a device's storage does not.
mkdir -p /opt/cn1 && cp "$1/EmbeddedDemo" /opt/cn1/
export QT_QPA_PLATFORM=offscreen CN1_DISPLAY=memory CN1_SIZE=800x480

now() { date +%s%3N; }

drop_caches() {
    sync
    echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || { echo "run.sh: cannot drop caches; use --privileged" >&2; exit 2; }
}

# smaps_rollup fields summed over the given pids: "pss private rss" in kB.
memory() {
    local files="" p
    for p in "$@"; do [ -r /proc/$p/smaps_rollup ] && files="$files /proc/$p/smaps_rollup"; done
    [ -n "$files" ] || { echo "0 0 0"; return; }
    awk '/^Pss:/{pss+=$2} /^Private_(Clean|Dirty):/{priv+=$2} /^Rss:/{rss+=$2}
         END{printf "%d %d %d", pss, priv, rss}' $files
}

# Distinct files mapped by the given pids: "total_kb count".
footprint() {
    local p
    for p in "$@"; do awk '$6 ~ /^\// {print $6}' /proc/$p/maps 2>/dev/null; done | sort -u \
        | while read -r f; do [ -f "$f" ] && stat -c %s "$f"; done \
        | awk '{s+=$1; n++} END{printf "%d %d", s/1024, n}'
}

# The pid and every descendant, however deep (Chromium's zygote forks renderers).
tree() {
    local c
    echo "$1"
    for c in $(pgrep -P "$1"); do tree "$c"; done
}

run_one() {
    local name="$1" mode="$2"; shift 2
    local log t0 pid ff line mem fp cpu0 cpu1 fps
    log=$(mktemp)
    [ "$mode" = cold ] && drop_caches
    t0=$(now)
    CN1_BENCH_SECONDS=$SECS "$@" >"$log" 2>&1 &
    pid=$!
    sleep $((SECS / 2))
    local pids; pids=$(tree $pid)
    mem=$(memory $pids)
    fp=$(footprint $pids)
    if [ "$name" = chromium ]; then
        # Chromium never exits by itself; CPU is read over the tree here.
        cpu0=0; for p in $pids; do cpu0=$((cpu0 + $(awk '{print $14 + $15}' /proc/$p/stat 2>/dev/null || echo 0))); done
        sleep $((SECS - SECS / 2 + 2))
        pkill -TERM -f -- '--user-data-dir=/tmp/chr-' 2>/dev/null
    fi
    wait $pid 2>/dev/null
    ff=$(grep -o 'first_frame_epoch_ms=[0-9]*' "$log" | head -1 | cut -d= -f2)
    line=$(grep 'CN1BENCH fps' "$log" | tail -1)
    fps=$(echo "$line" | grep -o 'fps=[0-9]*' | cut -d= -f2)
    local cpu; cpu=$(echo "$line" | grep -o 'cpu_pct=[0-9]*' | cut -d= -f2)
    # Chromium's CPU covers its whole life up to the sample, start-up included.
    [ "$name" = chromium ] && cpu="$((cpu0 * 100 / (SECS / 2) / 100))+"
    # Nothing of this run may still be alive when the next one is timed:
    # Chromium's tree takes seconds to unwind and skewed whatever followed it.
    pkill -KILL -f -- '--user-data-dir=/tmp/chr-' 2>/dev/null
    while pgrep -f -- '--user-data-dir=/tmp/chr-' >/dev/null || kill -0 $pid 2>/dev/null; do sleep 0.2; done
    sleep 2
    echo "$name $mode $(( ${ff:-0} - t0 )) ${fps:-?} ${cpu:-?} $mem $fp"
    rm -f "$log"
}

chromium_cmd() {
    exec chromium --headless=new --no-sandbox --disable-gpu --window-size=800,480 \
        --enable-logging=stderr --v=0 --user-data-dir=/tmp/chr-$RANDOM \
        "file:///bench/chromium/gauge.html?s=$SECS"
}

for i in $(seq "$RUNS"); do
    for mode in cold warm; do
        run_one codenameone $mode /opt/cn1/EmbeddedDemo
        run_one slint $mode /bench/slint/target/release/slintgauge
        run_one qt $mode /bench/qt/build/qtgauge
        run_one javafx $mode /opt/fxrt/bin/java -Dglass.platform=Headless -Dprism.order=sw \
            -cp /bench/javafx/classes Gauge
        run_one chromium $mode chromium_cmd
    done
done
