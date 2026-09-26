#!/bin/bash
# THE STANDING PERFORMANCE MATRIX: every workload, at several core counts,
# against a JDK 25 that has been given its best shot.
#
#   run-matrix.sh [rounds] [corelist]     defaults: 3 "1 2 4 8 16"
#
# WHY A MATRIX AND NOT A BISECT. Two regressions (stringBuilding +45%,
# hashMapChurn +36%) sat undetected in this branch for weeks because the geomean
# stayed flat -- a 1.8x win on arraySequential cancelled them. An aggregate hides
# exactly the thing it is watched for. And hunting them afterwards by bisection
# is worse: the host column on these short benchmarks moves ~50% run to run, so a
# bisect over dozens of commits is a coin-flip per step. Print every row, every
# session, and a regression shows up the day it lands.
#
# THREE THINGS THIS DOES THAT run-benchmark.sh DOES NOT:
#
# 1. CORE COUNTS. A number from a 16-core box says nothing about a phone. Both
#    sides are held to the same count: -XX:ActiveProcessorCount=N for HotSpot,
#    CN1_GC_MARK_THREADS=N for us. macOS has no taskset, so without the second
#    one a "scaling curve" would throttle the JVM and leave us unthrottled.
#
# 2. A WARM, AOT BASELINE. Comparing an AOT translator against a cold JVM
#    flatters us and is not the question. JDK 25's AOT cache (JEP 483/514) is
#    recorded and built once per workload, so HotSpot starts with classes loaded
#    and linked -- and it removes most of the host-side variance that made the
#    ratios unreadable.
#
# 3. THE REAL WORKLOAD. The eleven microbenchmarks are proxies; translating the
#    hello corpus is what ParparVM is for, and it is the only arm here that
#    exercises the whole VM at once.
#
# READ THE parpar COLUMN, NOT ONLY THE RATIO. The host column is the noisier of
# the two even with AOT; a ratio that moves while parpar ms is flat is the JVM
# wandering, not us.
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-3}"
CORES="${2:-1 2 4 8 16}"
J25="${JDK_25_HOME:?set JDK_25_HOME}"
J8="${JDK_8_HOME:?set JDK_8_HOME}"
REPO="$(cd ../.. && pwd)"
OUT="target/matrix"
mkdir -p "$OUT"
export CN1_BENCH_CFLAGS="${CN1_BENCH_CFLAGS--flto=thin}"

# MACHINE CANARY. This table is only meaningful across sessions if the machine was
# in the same state, and it frequently is not: an identical binary measured
# intArithmetic at 56.7ms and 88.8ms an hour apart on this host (thermal, after a
# long benchmarking run). Every matrix therefore records load and a fixed-work
# canary, and says plainly when the two tables cannot be compared.
CANARY_REF="${CN1_MATRIX_CANARY_REF:-0}"
echo "== machine state =="
uptime | sed 's/^/   /'
echo "== building arms =="
./translate-and-build.sh Bench "$OUT/bench-bin" >/dev/null
./translate-and-build.sh IoBench "$OUT/iobench-bin" >/dev/null
mkdir -p "$OUT/host-classes"
"$J8/bin/javac" -nowarn -encoding UTF-8 -d "$OUT/host-classes" \
    src/com/bench/Bench.java src/com/bench/IoBench.java \
    common/src/main/java/com/bench/CommonWorkloads.java
# The AOT cache REFUSES an exploded directory on the classpath ("Cannot have
# non-empty directory in paths"), so the host arm runs from a jar. Both the record
# and the use step must see the same classpath or the cache is rejected at load.
HOSTJAR="$OUT/host.jar"
"$J8/bin/jar" cf "$HOSTJAR" -C "$OUT/host-classes" .

# --- JDK 25 AOT cache, one per entry point -------------------------------------
# Recorded from a real run of the same workload, then linked. Failure to build a
# cache is REPORTED, never silently skipped: a matrix that quietly fell back to a
# cold JVM would show us winning for the wrong reason.
aot_for() {
    local tag=$1 main=$2 arg=$3
    local conf="$OUT/$tag.aotconf" cache="$OUT/$tag.aot"
    if [ -f "$cache" ]; then echo "$cache"; return; fi
    "$J25/bin/java" -XX:AOTMode=record -XX:AOTConfiguration="$conf" \
        -cp "$HOSTJAR" "$main" $arg >/dev/null 2>&1 || true
    if [ ! -f "$conf" ]; then echo "AOT-RECORD-FAILED" ; return; fi
    "$J25/bin/java" -XX:AOTMode=create -XX:AOTConfiguration="$conf" \
        -XX:AOTCache="$cache" -cp "$HOSTJAR" >/dev/null 2>&1 || true
    [ -f "$cache" ] && echo "$cache" || echo "AOT-CREATE-FAILED"
}
echo "== recording JDK 25 AOT caches =="
BENCH_AOT=$(aot_for bench com.bench.Bench "")
mkdir -p /tmp/cn1-matrix-aotrec
IO_AOT=$(aot_for iobench com.bench.IoBench "/tmp/cn1-matrix-aotrec")
echo "   bench cache: $BENCH_AOT"
echo "   iobench cache: $IO_AOT"

jdk_opts() { # $1 = cores, $2 = aot cache path
    local o="-XX:ActiveProcessorCount=$1"
    case "$2" in /*) o="$o -XX:AOTCache=$2";; esac
    echo "$o"
}

# --- run one (benchmark binary, host main) pair at N cores ---------------------
run_pair() {
    local cores=$1 pbin=$2 pargs=$3 hmain=$4 hargs=$5 aot=$6 label=$7
    for r in $(seq 1 "$ROUNDS"); do
        local pd hd
        # .noindex: macOS Spotlight skips directories with that suffix. The IO arms
        # emit thousands of files per round and mdworker was measured at 24-44% CPU
        # indexing them -- the harness was loading the machine it was measuring.
        pd=/tmp/cn1m-p-$$-$r.noindex; hd=/tmp/cn1m-h-$$-$r.noindex
        mkdir -p "$pd" "$hd"
        # INTERLEAVED, AND THE ORDER ALTERNATES. Both arms of a round run adjacent
        # in time, so thermal drift and background load hit them together and divide
        # out of the ratio. Alternating which goes first cancels the residual bias
        # from one arm always warming the machine for the other.
        if [ $(( r % 2 )) -eq 1 ]; then
            CN1_GC_MARK_THREADS="$cores" "$pbin" ${pargs:+$pd} > "$OUT/p.txt" 2>/dev/null || true
            "$J25/bin/java" $(jdk_opts "$cores" "$aot") -cp "$HOSTJAR" "$hmain" ${hargs:+$hd} > "$OUT/h.txt" 2>/dev/null || true
        else
            "$J25/bin/java" $(jdk_opts "$cores" "$aot") -cp "$HOSTJAR" "$hmain" ${hargs:+$hd} > "$OUT/h.txt" 2>/dev/null || true
            CN1_GC_MARK_THREADS="$cores" "$pbin" ${pargs:+$pd} > "$OUT/p.txt" 2>/dev/null || true
        fi
        rm -rf "$pd" "$hd"
        # One PAIRED sample per round. Taking min(parpar) and min(host) independently
        # across rounds -- what this used to do -- can pair round 1's parpar with
        # round 5's host and call the result a ratio, which compares two different
        # machine states. An identical binary measured intArithmetic at 56.7ms and
        # 88.8ms on this host an hour apart; that is the error mispairing admits.
        python3 - "$OUT/p.txt" "$OUT/h.txt" "$OUT/pairs-$label-$cores.txt" "$r" <<'PYIN'
import re,sys
def best(f):
    d={}
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+)', open(f).read()):
        k,v=m.group(1),int(m.group(2))
        d[k]=min(d.get(k,1<<62),v)
    return d
p,h=best(sys.argv[1]),best(sys.argv[2])
with open(sys.argv[3],'a') as f:
    for k in sorted(set(p)&set(h)):
        f.write(f'{k} {sys.argv[4]} {p[k]} {h[k]}\n')
PYIN
    done
}

# --- the selfhost translation arm ---------------------------------------------
# The only arm that runs the whole VM on the workload ParparVM exists for.
selfhost_arm() {
    local cores=$1
    local script="$REPO/vm/selfhost/bench-selfhost.sh"
    [ -x "$script" ] || { echo "   (selfhost: $script missing, SKIPPED)"; return; }
    # Same four positional arguments perf-guard.sh passes; calling it with only a
    # round count silently measured nothing and reported NA.
    # The HELLO corpus, with exactly the arguments perf-guard.sh derives for it.
    # Not the translator corpus: perf-guard's own comment records that the same
    # binary wins on translator and LOSES 1.263x/1.349x on hello, so measuring the
    # easier corpus would certify the drift rather than catch it.
    local T="$REPO/vm/selfhost/target"
    # bench-selfhost.sh REFUSES a stale native build, and that refusal lands in a
    # log this function only greps -- so without the rebuild the row silently
    # reported NA. Build once, before the first core count.
    # NO CACHED BUILD MARKER. translate-and-build.sh above runs a Maven clean that
    # wipes the translator target, which invalidates the selfhost native build --
    # the same trap that makes verify-selfhost.sh exit 0 having run nothing. A
    # marker file from a previous invocation therefore certifies a build that no
    # longer exists, and the arm silently reports NA. Build it here, once per
    # matrix run, after the bench builds have done their cleaning.
    if [ "$SELFHOST_BUILT" != "1" ]; then
        ( cd "$REPO/vm/selfhost" && ./build-selfhost.sh -O3 ) > "$OUT/selfhost-build.log" 2>&1 \
            && SELFHOST_BUILT=1 \
            || { echo "   (selfhost: build FAILED, see $OUT/selfhost-build.log; SKIPPED)"; return; }
    fi
    ( cd "$REPO/vm/selfhost" && \
      CN1_GC_MARK_THREADS="$cores" CN1_SELFHOST_JDK_OPTS="-XX:ActiveProcessorCount=$cores" \
      ./bench-selfhost.sh "$T/javaapi-classes;$T/hello-corpus" \
          com_codenameone_examples_hellocodenameone_HelloCodenameOneStub \
          com.codenameone.examples.hellocodenameone "$ROUNDS" ) \
        > "$OUT/selfhost-$cores.log" 2>&1 || true
    python3 - "$OUT/selfhost-$cores.log" "$cores" <<'PY'
import re,sys,json
txt=open(sys.argv[1]).read()
m=re.search(r'"parpar":\s*\{[^}]*"elapsed_min":\s*([0-9.]+)', txt)
j=re.search(r'"jdk25":\s*\{[^}]*"elapsed_min":\s*([0-9.]+)', txt)
pm=re.search(r'"parpar":\s*\{[^}]*"peak_max":\s*([0-9]+)', txt)
jm=re.search(r'"jdk25":\s*\{[^}]*"peak_max":\s*([0-9]+)', txt)
if m and j:
    print(f"SELFHOST {sys.argv[2]} {float(m.group(1))*1000:.0f} {float(j.group(1))*1000:.0f} "
          f"{(int(pm.group(1))>>20) if pm else 0} {(int(jm.group(1))>>20) if jm else 0}")
else:
    print(f"SELFHOST {sys.argv[2]} NA NA NA NA")
PY
}

rm -f "$OUT"/pairs-*.txt "$OUT"/.selfhost-built
SELFHOST_BUILT=0
: > "$OUT/selfhost.txt"
for c in $CORES; do
    echo "== cores=$c =="
    run_pair "$c" "$PWD/$OUT/bench-bin" ""  com.bench.Bench   ""  "$BENCH_AOT" micro
    run_pair "$c" "$PWD/$OUT/iobench-bin" d com.bench.IoBench d   "$IO_AOT"    io
    selfhost_arm "$c" | tee -a "$OUT/selfhost.txt"
done

python3 - "$OUT" "$CORES" <<'PYRPT'
import sys,os,glob,statistics
out,cores=sys.argv[1],sys.argv[2].split()
# Each line is ONE ROUND's matched pair, so the ratio is formed within a round and
# the median is over ratios -- not over two independently-minimised columns. A
# machine that drifts 50% mid-run moves both halves of every pair together and the
# ratio survives; that is what makes this table readable without a quiet machine.
data={}
for f in glob.glob(os.path.join(out,'pairs-*.txt')):
    label,c=os.path.basename(f)[6:-4].rsplit('-',1)
    for line in open(f):
        k,r,a,b=line.split()
        a,b=int(a),int(b)
        if b>0: data.setdefault(k,{}).setdefault(c,[]).append((a/1e6,a/b))
sh={}
pth=os.path.join(out,'selfhost.txt')
if os.path.exists(pth):
    for line in open(pth):
        t=line.split()
        # Only a well-formed SELFHOST line counts. The arm also tee's human text
        # ("(selfhost: build FAILED...)") into this file, and parsing that as a
        # float crashed the whole report -- losing 17 good benchmark rows to one
        # skipped arm.
        if len(t)==6 and t[0]=='SELFHOST' and t[2]!='NA':
            try: sh[t[1]]=(float(t[2]),float(t[3]),int(t[4]),int(t[5]))
            except ValueError: pass
def med(v): return statistics.median(v)
print("\n"+"="*86)
print("PARPARVM PERFORMANCE MATRIX -- median of PER-ROUND ratios vs JDK 25 (AOT, warm)")
print("="*86)
hdr="%-20s"%"benchmark"
for c in cores: hdr+="%20s"%("cores=%s"%c)
print(hdr); print("-"*86)
worst=[]
for k in sorted(data):
    row="%-20s"%k[:20]
    for c in cores:
        v=data[k].get(c)
        if v:
            ms=med([x[0] for x in v]); rs=[x[1] for x in v]
            rm=med(rs)
            spread=(max(rs)-min(rs))/min(rs)*100 if min(rs)>0 else 0
            row+="%20s"%("%.1fms %.2fx%s"%(ms,rm,"!" if spread>15 else ""))
            if rm>1.15: worst.append((rm,k,c))
        else: row+="%20s"%"-"
    print(row)
if sh:
    print("-"*86)
    for nm,idx,unit in (("selfhost(hello)",0,"ms"),("selfhost peakMB",2,"MB")):
        row="%-20s"%nm
        for c in cores:
            if c in sh:
                a,b=sh[c][idx],sh[c][idx+1]
                row+="%20s"%("%.0f%s %.2fx"%(a,unit,a/b if b else 0))
            else: row+="%20s"%"-"
        print(row)
canary=None
cf=os.path.join(out,'pairs-micro-%s.txt'%cores[0])
if os.path.exists(cf):
    v=[int(l.split()[2])/1e6 for l in open(cf) if l.split()[0]=='intArithmetic']
    if v: canary=med(v)
print("="*86)
if canary:
    ref=float(os.environ.get('CN1_MATRIX_CANARY_REF','0') or 0)
    note=""
    if ref:
        d=100.0*(canary-ref)/ref
        note=(" -- %+.0f%% vs reference %.1fms; ABSOLUTE ms NOT COMPARABLE to that run"%(d,ref)) if abs(d)>10 else " -- within 10%% of reference"
    print("machine canary (intArithmetic @%s core): %.1f ms%s"%(cores[0],canary,note))
    print("  record with: export CN1_MATRIX_CANARY_REF=%.1f"%canary)
print("cell = median parpar ms + median of per-round ratios.  >1.00x = slower than JDK 25.")
print('"!" marks a ratio spread >15% across rounds -- that cell is not settled, add rounds.')
if worst:
    worst.sort(reverse=True)
    print("worst: " + ", ".join("%s@%s %.2fx"%(k,c,r) for r,k,c in worst[:4]))
PYRPT
