---
name: developer-guide-vale
description: Run Vale on docs/developer-guide using the repo's CI workflow, including style sync, JSON report generation, and HTML report conversion.
---

# Developer Guide Vale

Use this skill when you need to run, debug, or fix Vale alerts in `docs/developer-guide`.
Follow the same steps as `.github/workflows/developer-guide-docs.yml` so local results match CI.

## Workflow

Run from the repository root.

1. Use Vale `3.13.0` if you need to install the tool locally.
2. Sync the shared style packages against the developer guide config.
3. Run Vale on `docs/developer-guide` with JSON output.
4. Convert the JSON report to HTML.
5. Inspect the report, then fix the marked `.adoc` content.

## CI-Matching Commands

```bash
set -euo pipefail
VALE_VERSION="3.13.0"
VALE_ARCHIVE="vale_${VALE_VERSION}_Linux_64-bit.tar.gz"
curl -fsSL -o "$VALE_ARCHIVE" "https://github.com/errata-ai/vale/releases/download/v${VALE_VERSION}/${VALE_ARCHIVE}"
tar -xzf "$VALE_ARCHIVE"
sudo mv vale /usr/local/bin/vale
rm -f "$VALE_ARCHIVE"
```

```bash
vale sync --config docs/developer-guide/.vale.ini
```

```bash
REPORT_DIR="build/developer-guide/reports"
REPORT_FILE="${REPORT_DIR}/vale-report.json"
HTML_REPORT="${REPORT_DIR}/vale-report.html"
mkdir -p "$REPORT_DIR"
set +e
vale --config docs/developer-guide/.vale.ini --output=JSON docs/developer-guide > "$REPORT_FILE"
STATUS=$?
set -e
python3 scripts/developer-guide/vale_report_to_html.py --input "$REPORT_FILE" --output "$HTML_REPORT"
```

## What To Check

- `build/developer-guide/reports/vale-report.json`
- `build/developer-guide/reports/vale-report.html`
- The file, line, column, rule, and message in each alert

## Fix Strategy

- Prefer wording changes in the developer guide over suppressing rules.
- Keep changes local to the flagged text unless the style rule itself needs a targeted exception.
- If Vale exits non-zero, treat that as "alerts were found" and inspect the generated report rather than stopping at the first failure.
