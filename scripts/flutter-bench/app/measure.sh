#!/usr/bin/env bash
# Measures one platform from the two artifacts build_apps.sh reported.
#
#   measure.sh <platform> <cn1-artifact> <flutter-artifact> <out-dir>
#
# Writes <out-dir>/result-<platform>.json and, when the platform was measured,
# <out-dir>/baseline-<platform>.json -- a baseline recorded from this run,
# which is what gets committed to arm or re-arm the regression gate.
#
# One script for every CI leg, because the legs cannot share a workflow step:
# Android has to run inside the emulator action, whose `script:` runs each line
# in a separate shell, and Linux has to run under Xvfb. Keeping the invocation
# here means those wrappers only ever differ in what they wrap.
set -Eeuo pipefail
trap 'rc=$?; echo "measure.sh: FAILED at line $LINENO (exit $rc)" >&2' ERR

[ $# -eq 4 ] || { echo "usage: measure.sh <platform> <cn1> <flutter> <out-dir>" >&2; exit 2; }
PLATFORM="$1"; CN1_APP="$2"; FLUTTER_APP="$3"; OUT="$4"
HERE="$(cd "$(dirname "$0")" && pwd -P)"
mkdir -p "$OUT"

PYTHON=python3
command -v python3 >/dev/null 2>&1 || PYTHON=python

# The launcher activities differ between the sides. A Codename One Android
# app's launcher is <MainClass>Stub (AndroidGradleBuilder writes it into the
# manifest), not the MainActivity Flutter's template uses; launching
# .MainActivity there fails before anything is timed.
exec "$PYTHON" "$HERE/../run_bench.py" \
    --platform "$PLATFORM" \
    --cn1-app "$CN1_APP" \
    --flutter-app "$FLUTTER_APP" \
    --cn1-bundle com.example.bench \
    --flutter-bundle com.codenameone.bench.gallery \
    --cn1-activity .BenchStub \
    --flutter-activity .MainActivity \
    --json "$OUT/result-$PLATFORM.json" \
    --baseline-out "$OUT/baseline-$PLATFORM.json" \
    --gate
