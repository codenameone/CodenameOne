#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
# Under this checkout's own build output: several clones share /tmp.
BUILD_DIR="$ROOT_DIR/scripts/cn1playground/common/target/snippet-cli"
CP_FILE="$BUILD_DIR/classpath.txt"
STAMP_FILE="$BUILD_DIR/.built.ok"

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

mkdir -p "$BUILD_DIR"

# The snippet is compiled exactly as the Playground compiles it: by the in-tree
# Java compiler, against the API stub library the Playground build generates from
# the framework and ParparVM's class library. Both come from the Maven build of
# scripts/cn1playground/common, which needs the framework SNAPSHOT installed in the
# local repository (scripts/setup-workspace.sh). The build and its classpath are
# cached and redone only when a source under the playground or the compiler changes.
# Extra Maven arguments (a -Dmaven.repo.local, say) go in SNIPPET_MVN_ARGS.
PLAYGROUND_DIR="$ROOT_DIR/scripts/cn1playground"
rebuild=true
if [[ -f "$STAMP_FILE" && -f "$CP_FILE" ]]; then
  if [[ -z "$(find "$PLAYGROUND_DIR/common/src" "$PLAYGROUND_DIR/common/pom.xml" \
      "$ROOT_DIR/vm/JavaCompiler/src" -newer "$STAMP_FILE" -print -quit)" ]]; then
    rebuild=false
  fi
fi
if [[ "$rebuild" == "true" ]]; then
  # shellcheck disable=SC2086
  mvn -q -B -nsu ${SNIPPET_MVN_ARGS:-} -f "$PLAYGROUND_DIR/common/pom.xml" -DskipTests test-compile >&2
  # shellcheck disable=SC2086
  mvn -q -B -nsu ${SNIPPET_MVN_ARGS:-} -f "$PLAYGROUND_DIR/common/pom.xml" dependency:build-classpath \
    -Dmdep.includeScope=test -Dmdep.outputFile="$CP_FILE" >&2
  touch "$STAMP_FILE"
fi

CP="$PLAYGROUND_DIR/common/target/test-classes:$PLAYGROUND_DIR/common/target/classes:$(cat "$CP_FILE")"
java -Djava.awt.headless=true -cp "$CP" com.codenameone.playground.JavaSnippetToPlaygroundUriHarness --file "$TMP_INPUT"
