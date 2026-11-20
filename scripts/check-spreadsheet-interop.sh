#!/usr/bin/env bash
# Opens the spreadsheets Codename One writes with LibreOffice and openpyxl.
#
# The unit tests read back what our own writers wrote, which proves the two
# agree with each other and nothing about whether a spreadsheet accepts the
# file. This writes xlsx and ods with our writers and has two independent
# implementations open them, in a container so that no office suite has to be
# installed on the machine or the runner.
#
# Needs core and the core-unittests test classes compiled:
#
#   cd maven && mvn -Plocal-dev-javase -pl core install -DskipTests
#   mvn -DunitTests -pl core-unittests test-compile
#
# and podman or docker. CONTAINER_ENGINE names the one to use.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE="$ROOT/maven/core/target/classes"
TESTS="$ROOT/maven/core-unittests/target/test-classes"
for dir in "$CORE" "$TESTS"; do
  if [ ! -d "$dir/com/codename1/spreadsheet" ]; then
    echo "check-spreadsheet-interop: $dir is not built; see the head of this script" >&2
    exit 2
  fi
done

ENGINE="${CONTAINER_ENGINE:-}"
if [ -z "$ENGINE" ]; then
  if command -v podman >/dev/null 2>&1; then ENGINE=podman; else ENGINE=docker; fi
fi

OUT="$(mktemp -d "${TMPDIR:-/tmp}/cn1-spreadsheet-interop.XXXXXX")"
chmod 755 "$OUT"
java -Djava.awt.headless=true -cp "$CORE:$TESTS" com.codename1.spreadsheet.SpreadsheetInteropExport "$OUT"
chmod -R a+rX "$OUT"

IMAGE=cn1-spreadsheet-interop
"$ENGINE" build -q -t "$IMAGE" -f "$ROOT/scripts/spreadsheet-interop/Containerfile" "$ROOT/scripts/spreadsheet-interop" >/dev/null
"$ENGINE" run --rm -v "$ROOT/scripts/spreadsheet-interop:/work:ro" -v "$OUT:/out:ro" "$IMAGE" \
  python3 /work/check-interop.py /out
