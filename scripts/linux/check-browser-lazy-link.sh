#!/usr/bin/env bash
# Compile the real browser bridge with real WebKit/GTK headers and inspect every
# unresolved symbol. No dead stripping may hide an accidentally linked API.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SOURCE="${1:-$ROOT/Ports/LinuxPort/nativeSources/cn1_linux_browser.c}"
TASK_DIR="$(mktemp -d)"
trap 'rm -rf "$TASK_DIR"' EXIT
# Only the generated VM primitive declarations are replaced. Native port and
# GTK/WebKit headers (including their type-checking macros) remain unmodified.
cat > "$TASK_DIR/cn1_globals.h" <<'HEADER'
#include <stdint.h>
typedef void* JAVA_OBJECT;
typedef int JAVA_INT;
typedef int64_t JAVA_LONG;
typedef int JAVA_BOOLEAN;
#define JAVA_VOID void
#define JAVA_NULL ((JAVA_OBJECT)0)
#define JAVA_TRUE 1
#define JAVA_FALSE 0
#define CODENAME_ONE_THREAD_STATE void* threadStateData
HEADER
for mode in debug release; do
  flags=(-O0)
  [ "$mode" != release ] || flags=(-O3 -DNDEBUG)
  # pkg-config emits compiler flags as separate words.
  read -r -a includes <<< "$(pkg-config --cflags gtk+-3.0 webkit2gtk-4.1)"
  "${CC:-cc}" -c -fPIC "${flags[@]}" -Werror=implicit-function-declaration \
    -I"$TASK_DIR" -I"$ROOT/Ports/LinuxPort/nativeSources" "${includes[@]}" \
    "$SOURCE" -o "$TASK_DIR/browser-$mode.o"
  nm -u "$TASK_DIR/browser-$mode.o" > "$TASK_DIR/undefined"
  if grep -E '[[:space:]]_?(webkit_|jsc_)' "$TASK_DIR/undefined"; then
    echo "ERROR: $mode browser bridge links optional WebKit/JavaScriptCore APIs directly" >&2
    exit 1
  fi
  echo "PASS: $mode browser bridge has no direct WebKit/JavaScriptCore references"
done
