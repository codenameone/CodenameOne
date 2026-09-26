#!/bin/bash
# The ParparVM vs JDK 25 performance gate, as a step of a platform's own CI build.
#
#   ci-perf-gate.sh run <platform> <outDir> [--hello-workload FILE --hello-app NAME]
#   ci-perf-gate.sh verdict <outDir>
#
# `run` fetches JDK 25 into a private directory (JAVA_HOME and PATH are left alone, so the
# job's later steps see the JDKs they always did), builds the self-hosted translator and
# the Bench binary at -O3, measures every benchmark, and leaves in <outDir>:
#   perf-results.json   the ratios, baselines and verdicts
#   perf-comment.md     the table the platform's PR comment carries (CN1SS_EXTRA_MARKDOWN)
# It NEVER fails the step: a regression must not stop the screenshots and the comment
# that report it. A build that breaks is written into perf-comment.md as such.
#
# `verdict` is the job's last step and fails it on a regression, on a gate that could not
# complete, or on results that were never written.
#
# Needs JDK_8_HOME (a JDK 21 is accepted where no JDK 8 exists: the builds only ask javac
# for -source/-target 1.8 against an explicit bootclasspath), clang (clang-cl, cmake and
# ninja on Windows), and GNU time on Linux.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
PYTHON="$(command -v python3 || command -v python)"
MODE="${1:?usage: ci-perf-gate.sh run|verdict ...}"; shift

if [ "$MODE" = verdict ]; then
    OUT="${1:?usage: ci-perf-gate.sh verdict <outDir>}"
    exec "$PYTHON" - "$OUT/perf-results.json" <<'PY'
import json, sys
from pathlib import Path
path = Path(sys.argv[1])
if not path.is_file():
    sys.exit('ParparVM performance gate: no results at %s -- the gate did not run' % path)
report = json.loads(path.read_text())
if report.get('error'):
    sys.exit('ParparVM performance gate could not complete: %s' % report['error'])
if report.get('failures'):
    print('ParparVM performance gate: benchmarks failed to run on %s:' % report['platform'])
    for f in report['failures']:
        print('  %s at %s cores: %s' % (f['benchmark'], f['cores'], f['reason']))
    sys.exit(1)
if report.get('regression'):
    lines = []
    for bench, per_cores in report['results'].items():
        for cores, entry in per_cores.items():
            for metric in ('time', 'memory'):
                e = entry[metric]
                if e['verdict'] == 'regression':
                    lines.append('  %s at %s cores: %s %.2fx against %.2fx (%+.1f%%)' % (
                        report['labels'].get(bench, bench), cores,
                        'time' if metric == 'time' else 'RAM', e['median'], e['baseline'],
                        (e['median'] / e['baseline'] - 1) * 100))
    print('ParparVM performance REGRESSION on %s:' % report['platform'])
    print('\n'.join(lines))
    sys.exit(1)
print('ParparVM performance gate: no regression on %s' % report['platform'])
PY
fi

[ "$MODE" = run ] || { echo "ci-perf-gate.sh: unknown mode $MODE"; exit 2; }
PLATFORM="${1:?platform}"; OUT="${2:?outDir}"; shift 2
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
LOG="$OUT/perf-build.log"

fail_section() {
    # The gate could not measure; say so where the numbers would have been.
    "$PYTHON" - "$OUT" "$PLATFORM" "$1" <<'PY'
import json, sys
from pathlib import Path
out, platform, reason = Path(sys.argv[1]), sys.argv[2], sys.argv[3]
(out / 'perf-results.json').write_text(json.dumps(
    {'platform': platform, 'error': reason, 'regression': False, 'results': {}}) + '\n')
(out / 'perf-comment.md').write_text(
    '### ParparVM vs HotSpot (JDK 25): %s\n\n**The performance gate could not run:** %s. '
    'See `perf-build.log` in this job\'s artifacts.\n' % (platform, reason))
PY
    echo "ci-perf-gate: $1"
    tail -40 "$LOG" 2>/dev/null
    exit 0
}

echo "ci-perf-gate: the gate's own unit tests"
(cd "$HERE" && "$PYTHON" -m unittest test_perf_gate) >> "$LOG" 2>&1 \
    || fail_section "the gate's own unit tests (test_perf_gate.py) failed"
echo "ci-perf-gate: fetching JDK 25"
JDK_25_HOME="$("$PYTHON" "$HERE/fetch-jdk.py" 25 "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/cn1-jdk25" 2>>"$LOG")" \
    || fail_section "JDK 25 could not be downloaded"
export JDK_25_HOME
echo "ci-perf-gate: building the self-hosted translator (-O3)"
"$HERE/build-selfhost.sh" -O3 >> "$LOG" 2>&1 || fail_section "build-selfhost.sh -O3 failed"
echo "ci-perf-gate: building the Bench binary (-O3)"
"$HERE/build-bench.sh" -O3 >> "$LOG" 2>&1 || fail_section "build-bench.sh -O3 failed"
case " $* " in
    *" --hello-workload "*) ;;
    *)
        # No recorded translation: the macOS build's own HelloCodenameOne classes.
        BUILD="$(find "$REPO/scripts/hellocodenameone" -type d -path '*dist/macos-build' 2>/dev/null | head -1)"
        [ -n "$BUILD" ] || fail_section "no recorded translation and no macOS build to take the hello corpus from"
        "$PYTHON" "$HERE/prepare-hello-corpus.py" "$BUILD" >> "$LOG" 2>&1 \
            || fail_section "prepare-hello-corpus.py failed"
        ;;
esac
echo "ci-perf-gate: measuring (this takes a while; progress below)"
"$PYTHON" "$HERE/perf-gate.py" --platform "$PLATFORM" --rounds "${CN1_PERF_ROUNDS:-5}" \
    --out "$OUT/perf-results.json" --markdown "$OUT/perf-comment.md" "$@"
rc=$?
echo "ci-perf-gate: perf-gate.py exited $rc (the verdict step decides the job)"
if [ -f "$OUT/perf-comment.md" ] && [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    cat "$OUT/perf-comment.md" >> "$GITHUB_STEP_SUMMARY"
fi
exit 0
