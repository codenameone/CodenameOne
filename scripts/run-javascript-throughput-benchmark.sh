#!/usr/bin/env bash
#
# Dispatch-shaped throughput benchmark for the JavaScript target.
#
# Translates vm/benchmarks/javascript/JsThroughputBench.java to JavaScript and
# runs it under Node (same V8 the browser uses), reporting per-workload times
# and a checksum per workload.
#
# It exists because nothing else in the tree can price a JS backend change.
# The bundle barely moves when generator density does (``yield* `` is seven
# characters), the screenshot suite is pass/fail, and the lifecycle harness
# reports milestones rather than time -- so a real improvement and a no-op
# were indistinguishable.
#
# Usage:
#   scripts/run-javascript-throughput-benchmark.sh [--json OUT] [--baseline IN]
#
#   --json OUT       write results as JSON
#   --baseline IN    compare against an earlier --json file and print deltas.
#                    REFUSES the comparison if any checksum differs, because a
#                    workload that changed what it computes cannot be compared
#                    on time.
#
# Exit codes: 0 clean, 1 benchmark/comparison failure, 2 misconfiguration.
set -euo pipefail

bench_log() { echo "[js-throughput] $1" >&2; }

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

JSON_OUT=""
BASELINE=""
while [ $# -gt 0 ]; do
  case "$1" in
    --json) JSON_OUT="$2"; shift 2 ;;
    --baseline) BASELINE="$2"; shift 2 ;;
    -h|--help) sed -n '2,24p' "$0" >&2; exit 0 ;;
    *) bench_log "unknown argument: $1"; exit 2 ;;
  esac
done

# CN1_JS_BENCH_SRC lets a probe run through the identical pipeline, which is
# how you tell "the benchmark is wrong" from "the harness is wrong".
BENCH_SRC="${CN1_JS_BENCH_SRC:-$REPO_ROOT/vm/benchmarks/javascript/JsThroughputBench.java}"
APP_NAME="$(basename "$BENCH_SRC" .java)"
# Overridable so two translator builds can be A/B'd by INTERLEAVING runs
# against saved jars, rather than by rebuilding between arms and comparing
# numbers taken minutes apart on a host whose mood has changed.
COMPILER_JAR="${CN1_JS_BENCH_COMPILER_JAR:-$REPO_ROOT/maven/parparvm/target/bundle/parparvm-compiler.jar}"
JAVA_API_JAR="$REPO_ROOT/maven/parparvm/target/bundle/parparvm-java-api.jar"

for required in "$BENCH_SRC" "$COMPILER_JAR" "$JAVA_API_JAR"; do
  if [ ! -e "$required" ]; then
    bench_log "missing $required"
    bench_log "build it with: mvn -f maven/pom.xml -pl parparvm -am -DskipTests package"
    exit 2
  fi
done

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"; [ -x "${JAVA_BIN:-}" ] || JAVA_BIN="$(command -v java)"
JAVAC_BIN="${JAVA_HOME:+$JAVA_HOME/bin/javac}"; [ -x "${JAVAC_BIN:-}" ] || JAVAC_BIN="$(command -v javac)"
NODE_BIN="$(command -v node || true)"
if [ -z "$NODE_BIN" ]; then bench_log "node is required"; exit 2; fi

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/cn1-js-throughput-XXXXXX")"
[ -n "${KEEP_BENCH_DIR:-}" ] && bench_log "keeping $WORK_DIR" || trap "rm -rf $WORK_DIR" EXIT

STAGE="$WORK_DIR/stage"
OUT="$WORK_DIR/out"
mkdir -p "$STAGE" "$OUT"

# The translator consumes ONE class tree holding both the JavaAPI and the
# application, exactly as the hellocodenameone build stages it.
( cd "$STAGE" && "${JAVA_HOME:-/usr}/bin/jar" xf "$JAVA_API_JAR" 2>/dev/null || unzip -qo "$JAVA_API_JAR" -d "$STAGE" )
rm -rf "$STAGE/META-INF"

bench_log "compiling $APP_NAME against the ParparVM JavaAPI"
"$JAVAC_BIN" -source 8 -target 8 -Xlint:-options -nowarn \
  -bootclasspath "$STAGE" -d "$STAGE" "$BENCH_SRC" >&2

bench_log "translating to JavaScript"
# Identifier minification off, as JavascriptTargetIntegrationTest does it.
# The whole-bundle renamer rewrites the ``cn1_*`` names that bindNative
# registers its natives under, and without port.js's fallback dance to repair
# the lookup, System.out.println resolves to nothing -- the benchmark then
# runs to completion and prints NOTHING, which reads as a harness bug rather
# than a broken lookup. It is also irrelevant to what is being measured here:
# this benchmark prices call dispatch, not identifier length.
"$JAVA_BIN" -cp "$COMPILER_JAR" \
  ${CN1_TRANSLATOR_OPTS:-} \
  -Dparparvm.js.minify.idents.off=1 \
  -Dparparvm.js.alias.off=1 \
  -Dcodename1.javascriptport.webapp="$REPO_ROOT/Ports/JavaScriptPort/src/main/webapp" \
  com.codename1.tools.translator.ByteCodeTranslator \
  javascript "$STAGE" "$OUT" "$APP_NAME" "com.codenameone.bench" "$APP_NAME" "1.0" "ios" "none" >&2

DIST="$OUT/dist/$APP_NAME-js"
[ -d "$DIST" ] || DIST="$(dirname "$(find "$OUT/dist" -name worker.js -print -quit)")"
if [ ! -d "$DIST" ]; then bench_log "translated bundle not found under $OUT/dist"; exit 1; fi

# Real timers and a real clock. The vm/tests harness deliberately stubs
# ``Date.now`` and drives a virtual clock so thread tests are deterministic --
# which is exactly wrong here, where the measurement IS wall time.
cat > "$WORK_DIR/harness.js" <<'HARNESS'
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const distDir = process.argv[2];
const timers = [];
let timerId = 1;
global.self = global;
global.window = global;
global.global = global;
// The VM reports a dead green thread by posting an error rather than by
// throwing, so a no-op postMessage turns a crashed benchmark into a silent
// empty result. Surface it and make the run fail.
let vmError = null;
global.postMessage = function(msg) {
  if (msg && msg.type === 'error') {
    vmError = msg;
    process.stderr.write('VM ERROR: ' + JSON.stringify(msg) + '\n');
  }
};
global.setTimeout = function(fn, millis) {
  const t = { id: timerId++, due: Date.now() + Math.max(0, millis | 0), fn: fn, cleared: false };
  timers.push(t);
  return t;
};
global.clearTimeout = function(t) { if (t) { t.cleared = true; } };
global.setInterval = function() { return { cleared: true }; };
global.clearInterval = function() {};
global.importScripts = function() {
  for (const script of arguments) {
    const p = path.join(distDir, String(script));
    vm.runInThisContext(fs.readFileSync(p, 'utf8'), { filename: p });
  }
};
importScripts('parparvm_runtime.js');
const chunks = fs.readdirSync(distDir)
  .filter((f) => /^translated_app_\d+\.js$/.test(f))
  .sort((a, b) => parseInt(a.match(/\d+/)[0], 10) - parseInt(b.match(/\d+/)[0], 10));
for (const c of chunks) { importScripts(c); }
importScripts('translated_app.js');
// Soundness probe. ``cn1_ivs*`` is the SYNC virtual dispatcher: the analysis
// selected it because it proved no implementation of that signature suspends.
// If the function it resolves turns out to be a generator anyway, cn1_ivsDrive
// silently steps it once and carries on -- correct for a body that does not
// actually yield, and a defect the benchmark would otherwise report as a
// speedup. Count those, and print the count next to the timings so a
// classification that is wrong-but-survivable cannot masquerade as one that
// is right.
if (typeof global.cn1_ivsDrive === 'function') {
  const inner = global.cn1_ivsDrive;
  global.__cn1SyncDroveGenerator = 0;
  global.cn1_ivsDrive = function(r, mid) {
    if (r && typeof r.next === 'function') { global.__cn1SyncDroveGenerator++; }
    return inner(r, mid);
  };
}
// worker.js does exactly this after its imports, and it is not optional: a
// bindNative that ran while jvm.classes was still empty could not compute the
// class-free dispatch id its callers use, so the native never reaches the
// method table. Skipping it leaves System.out.println resolving to the
// translated Java body, which prints nothing and reports no error -- the
// benchmark then runs to completion and produces no output at all.
if (typeof global.__parparInstallNativeBindings === 'function') {
  global.__parparInstallNativeBindings();
}
// Select a single workload, so each one is measured in a process that has
// run nothing else. See the comment on JsThroughputBench.only.
const only = process.argv[3] || '';
if (only) {
  const cls = jvm.classes[process.argv[4]];
  if (!cls || !cls.staticFields) { throw new Error('cannot reach bench class to set filter'); }
  cls.staticFields['only'] = jvm.createStringLiteral(only);
}
// Go through the runtime's own entry point rather than hand-spawning main:
// start() also installs the native overrides and the System print streams,
// and hand-rolling that is how a harness ends up measuring a VM the browser
// never runs.
jvm.start();
while (jvm.runnable.length || timers.length) {
  if (jvm.runnable.length) { jvm.drain(); continue; }
  timers.sort((a, b) => a.due - b.due || a.id - b.id);
  const t = timers.shift();
  if (!t || t.cleared) { continue; }
  t.fn();
}
console.log('BENCHPROBE syncDroveGenerator=' + (global.__cn1SyncDroveGenerator | 0));
if (vmError) {
  process.exitCode = 1;
}
HARNESS

bench_log "running under node $("$NODE_BIN" -v)"
RAW="$WORK_DIR/raw.txt"
: > "$RAW"

# One process per workload. Sharing a process makes every measurement depend
# on what ran before it, which reports phantom regressions in unchanged code.
# ``|| true`` is load bearing: with set -e and pipefail, grep's exit 1 on no
# match kills the script right here, so the probe fallback below was
# unreachable and a probe source produced no output at all.
WORKLOADS="$(grep -oE 'run\("[A-Za-z]+"' "$BENCH_SRC" | sed 's/run("//;s/"//' | sort -u || true)"
# A source with no run("...") calls is a PROBE, not the benchmark -- run it once
# unfiltered rather than refusing. Reproducing a translator bug in six seconds
# instead of a forty-seven-minute CI cycle is most of what this harness is for.
if [ -z "$WORKLOADS" ]; then
  bench_log "no run(\"...\") workloads found; running $APP_NAME once, unfiltered"
  WORKLOADS="__all__"
fi

for workload in $WORKLOADS; do
  filter="$workload"
  [ "$filter" = "__all__" ] && filter=""
  if ! "$NODE_BIN" "$WORK_DIR/harness.js" "$DIST" "$filter" "$APP_NAME" \
        >> "$RAW" 2>>"$WORK_DIR/stderr.txt"; then
    bench_log "workload $workload failed"
    sed -n '1,40p' "$WORK_DIR/stderr.txt" >&2
    exit 1
  fi
  # Exiting 0 is not the same as producing a measurement. A process that
  # reaches BENCHSUITE without emitting its BENCH row leaves that workload out
  # of the JSON, and the baseline-vs-new check cannot catch a workload that was
  # missing from the BASELINE in the first place -- every later comparison is
  # then blind to it. Assert the row here, where the omission is still visible.
  if [ "$filter" != "" ] && ! grep -q "^BENCH id=$workload " "$RAW"; then
    bench_log "workload $workload exited cleanly but emitted no BENCH row"
    sed -n '1,20p' "$WORK_DIR/stderr.txt" >&2
    exit 1
  fi
done
cat "$RAW" >&2

if ! grep -q '^BENCHSUITE ' "$RAW"; then
  bench_log "benchmark did not reach its final marker -- output follows"
  sed -n '1,40p' "$RAW" >&2
  sed -n '1,20p' "$WORK_DIR/stderr.txt" >&2
  exit 1
fi

SUSPENSION_REPORT="$DIST/suspension-report.txt"
[ -f "$SUSPENSION_REPORT" ] || SUSPENSION_REPORT="$(find "$OUT" -name suspension-report.txt -print -quit || true)"

BUNDLE_BYTES=$(cat "$DIST"/translated_app*.js | wc -c | tr -d ' ')
YIELDS=$(cat "$DIST"/translated_app*.js | grep -o 'yield\*' | wc -l | tr -d ' ')
GENERATORS=$(cat "$DIST"/translated_app*.js | grep -o 'function\*' | wc -l | tr -d ' ')

exec 3>&1
BASELINE="$BASELINE" JSON_OUT="$JSON_OUT" RAW="$RAW" REPORT="$SUSPENSION_REPORT" \
  BUNDLE_BYTES="$BUNDLE_BYTES" YIELDS="$YIELDS" GENERATORS="$GENERATORS" \
  python3 "$SCRIPT_DIR/lib/js_throughput_report.py" >&3
