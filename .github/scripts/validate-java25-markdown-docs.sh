#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "$0")/../.." && pwd)"
[ -d CodenameOne ] && [ -d Ports/CLDC11 ] || { echo 'ERROR: Expected CodenameOne and Ports/CLDC11 directories.' >&2; exit 1; }
failed=0

javadoc_hits="$(mktemp)" && package_hits="$(mktemp)"
if grep -R -nE --include='*.java' '^[[:space:]]*/\*\*' CodenameOne Ports/CLDC11 >"$javadoc_hits"; then
  cat "$javadoc_hits"; echo 'ERROR: Found classic Javadoc markers (/**). Use /// markdown comments.' >&2; failed=1
else
  grep_status=$?
  [ "$grep_status" -eq 1 ] || { echo 'ERROR: Failed while scanning for /** markers.' >&2; exit "$grep_status"; }
fi
find CodenameOne Ports/CLDC11 -type f -name 'package.html' >"$package_hits"
if [ -s "$package_hits" ]; then cat "$package_hits"; echo 'ERROR: Found package.html files. Use package-info.java with /// markdown comments.' >&2; failed=1; fi

[ "$failed" -eq 0 ] && echo 'Validation passed: no /** markers and no package.html files found in CodenameOne or Ports/CLDC11.'
rm -f "$javadoc_hits" "$package_hits"
exit "$failed"
