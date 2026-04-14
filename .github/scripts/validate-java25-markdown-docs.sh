#!/usr/bin/env bash
set -euo pipefail
cd "$(cd "$(dirname "$0")/../.." && pwd)"
[ -d CodenameOne ] && [ -d Ports/CLDC11 ] || { echo 'ERROR: Expected CodenameOne and Ports/CLDC11 directories.' >&2; exit 1; }
failed=0

if rg -n '/\*\*' CodenameOne Ports/CLDC11; then echo 'ERROR: Found classic Javadoc markers (/**). Use /// markdown comments.' >&2; failed=1; fi
if rg --files CodenameOne Ports/CLDC11 | rg '/package\.html$'; then echo 'ERROR: Found package.html files. Use package-info.java with /// markdown comments.' >&2; failed=1; fi

[ "$failed" -eq 0 ] && echo 'Validation passed: no /** markers and no package.html files found in CodenameOne or Ports/CLDC11.'
exit "$failed"
