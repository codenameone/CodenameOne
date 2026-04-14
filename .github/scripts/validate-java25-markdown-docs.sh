#!/usr/bin/env bash
set -euo pipefail
failed=0

rg -n '/\*\*' CodenameOne Ports/CLDC11 && { echo 'ERROR: Found classic Javadoc markers (/**). Use /// markdown comments.' >&2; failed=1; } || true
rg --files CodenameOne Ports/CLDC11 | rg '/package\.html$' && { echo 'ERROR: Found package.html files. Use package-info.java with /// markdown comments.' >&2; failed=1; } || true

[ "$failed" -eq 0 ] && echo 'Validation passed: no /** markers and no package.html files found in CodenameOne or Ports/CLDC11.'
exit "$failed"
