#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/extract-javadoc-java-snippets.sh [--package-prefix <prefix>] [path ...]

Extract Java fenced code blocks from JavaDoc comments and emit JSONL records:
  {"sourceFile":"...","symbol":"...","snippetIndex":n,"startLine":n,"endLine":n,"code":"..."}

Arguments:
  --package-prefix <prefix>  Only process Java sources with package names that start with prefix.
  path ...                   Files/directories to scan. Defaults to CodenameOne/src.
USAGE
}

PACKAGE_PREFIX=""
TARGETS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package-prefix)
      shift
      if [[ $# -eq 0 ]]; then
        echo "Missing value for --package-prefix" >&2
        exit 2
      fi
      PACKAGE_PREFIX="$1"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      while [[ $# -gt 0 ]]; do
        TARGETS+=("$1")
        shift
      done
      ;;
    *)
      TARGETS+=("$1")
      shift
      ;;
  esac
done

if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=("CodenameOne/src")
fi

python3 - "$PACKAGE_PREFIX" "${TARGETS[@]}" <<'PY'
import json
import re
import sys
from pathlib import Path

package_prefix = sys.argv[1]
paths = sys.argv[2:]

package_re = re.compile(r"^\s*package\s+([A-Za-z_][\w\.]*)\s*;")
fence_start_re = re.compile(r"^\s*```\s*java\s*$", re.IGNORECASE)
fence_end_re = re.compile(r"^\s*```\s*$")


def collect_files(raw_paths):
    out = []
    seen = set()
    for raw in raw_paths:
        p = Path(raw)
        if not p.exists():
            continue
        if p.is_file() and p.suffix == ".java":
            rp = str(p.resolve())
            if rp not in seen:
                out.append(p)
                seen.add(rp)
            continue
        if p.is_dir():
            for f in sorted(p.rglob("*.java")):
                rp = str(f.resolve())
                if rp not in seen:
                    out.append(f)
                    seen.add(rp)
    return out


def find_package(lines):
    for line in lines:
        m = package_re.match(line)
        if m:
            return m.group(1)
    return ""


def normalize_decl(line):
    return " ".join(line.strip().split())


def symbol_after_javadoc(lines, javadoc_end_line):
    i = javadoc_end_line
    max_i = min(len(lines), javadoc_end_line + 40)
    while i < max_i:
        raw = lines[i - 1]
        stripped = raw.strip()
        i += 1
        if not stripped:
            continue
        if stripped.startswith("@"):
            continue
        if stripped.startswith("//"):
            continue
        if stripped.startswith("/*"):
            while i <= len(lines) and "*/" not in lines[i - 1]:
                i += 1
            i += 1
            continue
        return normalize_decl(stripped)
    return f"line:{javadoc_end_line}"


def relative_source(path):
    try:
        return str(path.relative_to(Path.cwd()))
    except ValueError:
        return str(path)


def extract_fences(block_lines, line_start, symbol, source_rel, idx):
    in_fence = False
    code_lines = []
    fence_start_line = None
    for offset, raw_line in enumerate(block_lines):
        abs_line = line_start + offset
        stripped = raw_line.strip()
        if not in_fence and fence_start_re.match(stripped):
            in_fence = True
            code_lines = []
            fence_start_line = abs_line
            continue
        if in_fence and fence_end_re.match(stripped):
            idx += 1
            yield idx, {
                "sourceFile": source_rel,
                "symbol": symbol,
                "snippetIndex": idx,
                "startLine": (fence_start_line or abs_line) + 1,
                "endLine": abs_line - 1,
                "code": "\n".join(code_lines),
            }
            in_fence = False
            code_lines = []
            fence_start_line = None
            continue
        if in_fence:
            code_lines.append(raw_line.rstrip())


for source in collect_files(paths):
    text = source.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    package_name = find_package(lines)
    if package_prefix and not package_name.startswith(package_prefix):
        continue

    idx = 0
    line_no = 1
    while line_no <= len(lines):
        line = lines[line_no - 1]
        if "/**" in line:
            javadoc_start = line_no
            while line_no <= len(lines) and "*/" not in lines[line_no - 1]:
                line_no += 1
            if line_no > len(lines):
                break
            javadoc_end = line_no
            symbol = symbol_after_javadoc(lines, javadoc_end + 1)
            block_lines = [re.sub(r"^\s*\* ?", "", l) for l in lines[javadoc_start - 1:javadoc_end]]
            for idx, record in extract_fences(block_lines, javadoc_start, symbol, relative_source(source), idx):
                print(json.dumps(record, ensure_ascii=False))
            line_no = javadoc_end + 1
            continue

        if re.match(r"^\s*///", line):
            doc_start = line_no
            block_lines = []
            while line_no <= len(lines) and re.match(r"^\s*///", lines[line_no - 1]):
                block_lines.append(re.sub(r"^\s*///\s?", "", lines[line_no - 1]))
                line_no += 1
            symbol = symbol_after_javadoc(lines, line_no)
            for idx, record in extract_fences(block_lines, doc_start, symbol, relative_source(source), idx):
                print(json.dumps(record, ensure_ascii=False))
            continue

        line_no += 1
PY
