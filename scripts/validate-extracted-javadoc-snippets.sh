#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: scripts/validate-extracted-javadoc-snippets.sh <snippets.jsonl>" >&2
  exit 2
fi

JSONL_FILE="$1"
if [[ ! -f "$JSONL_FILE" ]]; then
  echo "JSONL file not found: $JSONL_FILE" >&2
  exit 2
fi

EXCLUSIONS_FILE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)/java-snippet-validation-exclusions.jsonl"

python3 - "$JSONL_FILE" "$EXCLUSIONS_FILE" <<'PY'
import json
import pathlib
import subprocess
import sys
import tempfile

jsonl_path = pathlib.Path(sys.argv[1])
exclusions_path = pathlib.Path(sys.argv[2])
failures = []
count = 0
excluded_count = 0

exclusions = {}
if exclusions_path.exists():
    for line in exclusions_path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        item = json.loads(line)
        key = f"{item.get('sourceFile')}#{item.get('snippetIndex')}"
        exclusions[key] = item.get("reason", "Explicitly excluded.")

for line in jsonl_path.read_text(encoding="utf-8").splitlines():
    if not line.strip():
        continue
    record = json.loads(line)
    count += 1
    with tempfile.NamedTemporaryFile("w", suffix=".java", delete=False, encoding="utf-8") as tmp:
        tmp.write(record.get("code", ""))
        tmp_path = tmp.name
    proc = subprocess.run(
        ["scripts/java-snippet-to-playground-uri.sh", "--file", tmp_path],
        text=True,
        capture_output=True,
        check=False,
    )
    output = (proc.stdout or "").strip()
    if proc.returncode != 0 or not output.startswith("/playground/?code="):
        key = f"{record.get('sourceFile')}#{record.get('snippetIndex')}"
        if key in exclusions:
            excluded_count += 1
            continue
        failures.append(
            {
                "sourceFile": record.get("sourceFile"),
                "symbol": record.get("symbol"),
                "snippetIndex": record.get("snippetIndex"),
                "result": output or (proc.stderr or "").strip(),
            }
        )

if failures:
    print(f"Snippet validation failed for {len(failures)} snippets out of {count}.")
    print(f"Excluded {excluded_count} snippets via explicit exclusion list.")
    for failure in failures[:50]:
        print(json.dumps(failure, ensure_ascii=False))
    sys.exit(1)

print(f"Validated {count - excluded_count} snippets successfully.")
print(f"Excluded {excluded_count} snippets via explicit exclusion list.")
PY
