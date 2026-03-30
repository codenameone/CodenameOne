#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
BUILD_DIR="${TMPDIR:-/tmp}/cn1-snippet-cli"
CLASSES_DIR="$BUILD_DIR/classes"
SOURCES_FILE="$BUILD_DIR/sources.txt"
STAMP_FILE="$BUILD_DIR/.compiled.ok"

usage() {
  cat <<'USAGE'
Usage:
  scripts/java-snippet-to-playground-uri.sh --file <path>
  cat snippet.java | scripts/java-snippet-to-playground-uri.sh

Converts a Java snippet into a Codename One playground URI.

Output:
  - Success: /playground/?code=<base64url>
  - Failure: {"ok":false,"errorType":"...","message":"...","line":n,"column":n}
USAGE
}

INPUT_MODE="stdin"
INPUT_FILE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --file)
      shift
      if [[ $# -eq 0 ]]; then
        echo "Missing value for --file" >&2
        usage >&2
        exit 2
      fi
      INPUT_MODE="file"
      INPUT_FILE="$1"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

TMP_INPUT="$(mktemp)"
trap 'rm -f "$TMP_INPUT"' EXIT

if [[ "$INPUT_MODE" == "file" ]]; then
  if [[ ! -f "$INPUT_FILE" ]]; then
    echo "File not found: $INPUT_FILE" >&2
    exit 2
  fi
  cat "$INPUT_FILE" > "$TMP_INPUT"
else
  if [[ -t 0 ]]; then
    echo "No input provided. Use --file or pipe snippet via stdin." >&2
    usage >&2
    exit 2
  fi
  cat > "$TMP_INPUT"
fi

mkdir -p "$BUILD_DIR" "$CLASSES_DIR"

# Build a javac source list from repository sources, excluding BeanShell desktop/classpath files
# that are not needed by the CN1 playground runtime.
: > "$SOURCES_FILE"
while IFS= read -r src; do
  case "$src" in
    */bsh/classpath/*) continue ;;
    */bsh/commands/dir.java) continue ;;
    */bsh/util/ClassBrowser.java) continue ;;
  esac
  printf '%s\n' "$src" >> "$SOURCES_FILE"
done < <(find \
  "$ROOT_DIR/CodenameOne/src" \
  "$ROOT_DIR/Factory/src" \
  "$ROOT_DIR/scripts/cn1playground/common/src/main/java/com/codenameone/playground" \
  "$ROOT_DIR/scripts/cn1playground/common/src/main/java/bsh" \
  "$ROOT_DIR/scripts/cn1playground/common/src/main/java/bsh/cn1" \
  "$ROOT_DIR/scripts/cn1playground/common/src/main/java/bsh/cn1/gen" \
  -name '*.java' -print)

printf '%s\n' "$ROOT_DIR/scripts/cn1playground/common/src/test/java/com/codenameone/playground/JavaSnippetToPlaygroundUriHarness.java" >> "$SOURCES_FILE"

rebuild=true
if [[ -f "$STAMP_FILE" ]]; then
  rebuild=false
  while IFS= read -r src; do
    if [[ "$src" -nt "$STAMP_FILE" ]]; then
      rebuild=true
      break
    fi
  done < "$SOURCES_FILE"
fi

if [[ "$rebuild" == "true" ]]; then
  javac -encoding UTF-8 -d "$CLASSES_DIR" @"$SOURCES_FILE" >/dev/null 2>&1
  touch "$STAMP_FILE"
fi

java -cp "$CLASSES_DIR" com.codenameone.playground.JavaSnippetToPlaygroundUriHarness --file "$TMP_INPUT"
