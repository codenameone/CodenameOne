#!/usr/bin/env bash
# Fails unless each named test class ran at least one test and skipped none.
# A test that finds its server's address unset skips itself, and a skipped
# suite is green, so a job that exists to reach real servers checks this.
#
# Usage: require-no-skipped-tests.sh <surefire-reports-dir> <SimpleClassName>...
set -euo pipefail
dir="$1"
shift
status=0
for t in "$@"; do
    f=$(ls "$dir"/*."$t".txt 2>/dev/null | head -1 || true)
    if [ -z "$f" ]; then
        echo "::error::$t produced no report"
        status=1
        continue
    fi
    line=$(grep -m1 "Tests run:" "$f" || true)
    if ! echo "$line" | grep -q "Skipped: 0," || echo "$line" | grep -q "Tests run: 0,"; then
        echo "::error::$t did not run in full: $line"
        status=1
    fi
done
exit $status
