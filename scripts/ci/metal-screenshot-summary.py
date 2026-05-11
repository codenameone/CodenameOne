#!/usr/bin/env python3
"""Render the Metal screenshot-comparison summary for a GitHub Actions job.

Called from .github/workflows/scripts-ios.yml's build-ios-metal job.
Reads the screenshot-compare.json produced by scripts/run-ios-ui-tests.sh
and emits either:

  --markdown : a markdown section (headline + per-test table) suitable
               for appending to $GITHUB_STEP_SUMMARY.
  --headline : a one-liner of the form "N/T matched against golden
               images" suitable for a ::notice title.

Kept as a separate file because embedding Python heredocs inside a
YAML "run: |" block is fragile -- any non-indented line breaks the
block scalar.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def load_results(path: Path) -> list[dict]:
    with path.open() as f:
        data = json.load(f)
    return data.get("results", [])


def counts(results: list[dict]) -> tuple[int, int, int, int]:
    total = len(results)
    matched = sum(1 for r in results if r.get("status") == "equal")
    different = sum(1 for r in results if r.get("status") == "different")
    other = total - matched - different
    return total, matched, different, other


def emit_markdown(results: list[dict]) -> None:
    total, matched, different, other = counts(results)
    print(f"**Headline:** {matched}/{total} matched, {different} differ, {other} other.")
    print()
    print("| Test | Status | Mismatch % |")
    print("| --- | --- | --- |")
    for r in results:
        pct = ""
        if r.get("status") == "different":
            mm = r.get("details", {}).get("mismatch_percent", 0.0)
            pct = f"{mm:.1f}%"
        print(f"| {r.get('test', '?')} | {r.get('status', '?')} | {pct} |")


def emit_headline(results: list[dict]) -> None:
    total, matched, _different, _other = counts(results)
    print(f"{matched}/{total} matched against golden images")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("compare_json", help="Path to screenshot-compare.json")
    mode = ap.add_mutually_exclusive_group(required=True)
    mode.add_argument("--markdown", action="store_const", dest="mode", const="markdown")
    mode.add_argument("--headline", action="store_const", dest="mode", const="headline")
    args = ap.parse_args()

    path = Path(args.compare_json)
    if not path.is_file() or path.stat().st_size == 0:
        print(f"[metal-screenshot-summary] Comparison JSON missing or empty: {path}", file=sys.stderr)
        return 2

    results = load_results(path)
    if args.mode == "markdown":
        emit_markdown(results)
    else:
        emit_headline(results)
    return 0


if __name__ == "__main__":
    sys.exit(main())
