#!/usr/bin/env bash
#
# Runs bench.sh over the samples that live in this repository and writes the two
# files CI uploads:
#
#   OUT/benchmarks-<os>.json   every sample's result.json, in one document
#   OUT/benchmarks-<os>.md     the same, as tables (report.py)
#
# Usage: bench-samples.sh --work DIR --out DIR [--stage all|build|measure]
#                         [--startup-runs N] [--session-runs N]
#
# The environment is bench.sh's (JAVA17_HOME, BENCH_JDK21_HOME, ...). Only the
# in-tree samples are built: third-party applications are never vendored, and a
# benchmark anyone can re-run must not depend on a checkout nobody else has. To
# measure another application, call bench.sh on it directly.
set -euo pipefail

HERE="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
ROOT="$(CDPATH='' cd -- "$HERE/../.." && pwd)"
SAMPLES="$ROOT/scripts/desktop-compat-samples"

WORK="" OUT="" PASS=()
while [ "$#" -gt 0 ]; do
  case "$1" in
    --work) WORK="$2"; shift 2 ;;
    --out) OUT="$2"; shift 2 ;;
    --stage|--startup-runs|--session-runs|--heaps) PASS+=("$1" "$2"); shift 2 ;;
    -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
    *) echo "bench-samples.sh: unknown argument $1" >&2; exit 2 ;;
  esac
done
if [ -z "$WORK" ] || [ -z "$OUT" ]; then
  echo "bench-samples.sh: --work and --out are required" >&2
  exit 2
fi
mkdir -p "$WORK" "$OUT"

case "$(uname -s)" in
  Linux) OS_NAME="linux" ;;
  Darwin) OS_NAME="macos" ;;
  *) OS_NAME="windows" ;;
esac

# name | kind | main class
SAMPLE_LIST="swing-gallery|swing|com.example.gallery.GalleryApp
javafx-gallery|javafx|com.example.fxgallery.GalleryApp"

RESULTS=()
while IFS='|' read -r NAME KIND MAIN; do
  [ -n "$NAME" ] || continue
  # A sample that fails to build is a row with empty cells and a line under
  # "Failures", not a missing row: bench.sh records the reason and goes on.
  bash "$HERE/bench.sh" --app "$SAMPLES/$NAME" --name "$NAME" --kind "$KIND" --main "$MAIN" \
    --work "$WORK" ${PASS[@]+"${PASS[@]}"} || echo "bench-samples.sh: bench.sh failed outright for $NAME" >&2
  if [ -f "$WORK/$NAME/result.json" ]; then
    RESULTS+=("$WORK/$NAME/result.json")
  fi
done <<EOF
$SAMPLE_LIST
EOF

if [ "${#RESULTS[@]}" -eq 0 ]; then
  echo "bench-samples.sh: no sample produced a result" >&2
  exit 1
fi
python3 "$HERE/benchutil.py" collect "$OUT/benchmarks-$OS_NAME.json" "${RESULTS[@]}"
python3 "$HERE/report.py" --out "$OUT/benchmarks-$OS_NAME.md" "${RESULTS[@]}"
echo "wrote $OUT/benchmarks-$OS_NAME.json and $OUT/benchmarks-$OS_NAME.md"
