#!/usr/bin/env bash
# Local real-child conformance. Native builds must never run in CI.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MODE="${1:-jvm}"
case "$MODE" in jvm|native) ;; *) echo "usage: $0 [jvm|native]" >&2; exit 2 ;; esac
if [[ "$MODE" == native && -n "${CI:-}" ]]; then
    echo "Native process conformance is local-only." >&2; exit 2
fi
J8="${JDK_8_HOME:-${JAVA_HOME:-}}"
if [[ -z "$J8" && -x /usr/libexec/java_home ]]; then J8="$(/usr/libexec/java_home -v 1.8)"; fi
: "${J8:?Set JDK_8_HOME to Java 8}"
export JAVA_HOME="$J8" PATH="$J8/bin:$PATH" JDK_8_HOME="$J8"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-process.XXXXXX")"
WORK="$(cd "$WORK" && pwd -P)"
trap 'rm -rf "$WORK"' EXIT
# A space in the executable path is part of the contract.
"${CC:-cc}" -Wall -Wextra -Werror "$ROOT/scripts/process/process_child.c" -o "$WORK/process child"
"${CC:-cc}" -Wall -Wextra -Werror -I "$ROOT/vm/ByteCodeTranslator/src" \
    "$ROOT/scripts/process/process_os_test.c" -o "$WORK/process-os"
"$WORK/process-os" "$WORK/process child" "$WORK"
if [[ "$MODE" == jvm ]]; then
    mkdir -p "$WORK/classes"
    "$J8/bin/javac" -encoding UTF-8 -d "$WORK/classes" \
        "$ROOT/CodenameOne/src/com/codename1/impl/SharedWithBackend.java" \
        "$ROOT/CodenameOne/src/com/codename1/io/Process.java" \
        "$ROOT/CodenameOne/src/com/codename1/io/ProcessBuilder.java" \
        "$ROOT/CodenameOne/src/com/codename1/impl/process/ProcessSupport.java" \
        "$ROOT/Ports/JavaSE/src/com/codename1/impl/process/JavaSEProcessSupport.java" \
        "$ROOT/vm/backend/impl/javase/com/codename1/impl/ImplementationFactory.java" \
        "$ROOT/vm/backend/demo/process/com/demo/ProcessConformance.java"
    "$J8/bin/java" -cp "$WORK/classes" com.demo.ProcessConformance "$WORK/process child" "$WORK"
else
    CN1_BACKEND_DEMO=demo/process CN1_BACKEND_STANDALONE_DEMO=1 \
        "$ROOT/vm/backend/build.sh" ProcessConformance com.demo "$WORK/process-native"
    "$WORK/process-native" "$WORK/process child" "$WORK"
    CN1_BACKEND_DEMO=demo/process-virtual CN1_BACKEND_STANDALONE_DEMO=1 \
        "$ROOT/vm/backend/build.sh" ProcessVirtualConformance com.demo "$WORK/process-virtual"
    CN1_HTTP_POLL_MODE=3 "$WORK/process-virtual" "$WORK/process child"
fi
