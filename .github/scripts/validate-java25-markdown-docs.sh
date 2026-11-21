#!/usr/bin/env bash
# Every documented source tree writes /// markdown documentation comments, never
# classic /** */ javadoc, and describes its packages in package-info.java rather
# than package.html.
#
# The trees: the client API (CodenameOne and the CLDC java.* classes it ships)
# and the backend runtime (vm/backend/src, the shared server code, and
# vm/backend/impl, its per-target classes). The backend is published as its own
# API reference by the same doclet, so it is held to the same rule; before it
# was, 77 of its files were classic javadoc and nothing noticed.
set -euo pipefail
cd "$(cd "$(dirname "$0")/../.." && pwd)"
TREES=(CodenameOne Ports/CLDC11 vm/backend/src vm/backend/impl)
for tree in "${TREES[@]}"; do
  [ -d "$tree" ] || { echo "ERROR: Expected directory $tree." >&2; exit 1; }
done
failed=0

javadoc_hits="$(mktemp)" && package_hits="$(mktemp)"
if grep -R -nE --include='*.java' '^[[:space:]]*/\*\*' "${TREES[@]}" >"$javadoc_hits"; then
  cat "$javadoc_hits"; echo 'ERROR: Found classic Javadoc markers (/**). Use /// markdown comments.' >&2; failed=1
else
  grep_status=$?
  [ "$grep_status" -eq 1 ] || { echo 'ERROR: Failed while scanning for /** markers.' >&2; exit "$grep_status"; }
fi
find "${TREES[@]}" -type f -name 'package.html' >"$package_hits"
if [ -s "$package_hits" ]; then cat "$package_hits"; echo 'ERROR: Found package.html files. Use package-info.java with /// markdown comments.' >&2; failed=1; fi

[ "$failed" -eq 0 ] && echo "Validation passed: no /** markers and no package.html files found in ${TREES[*]}."
rm -f "$javadoc_hits" "$package_hits"
exit "$failed"
