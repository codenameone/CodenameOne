#!/bin/bash
# Builds the Bench microbenchmarks (vm/benchmarks) for perf-gate.py: the native binary
# ParparVM runs, and the class files JDK 25 runs.
#
#   build-bench.sh [-O1|-O3]        default -O3, the shape that ships
#
# Run AFTER build-selfhost.sh: it reuses that build's JavaAPI classes, translator classes
# and ASM classpath, and compiles through the same compile-dist.sh, so the benchmark
# binary gets exactly the flags the self-hosted translator got on this platform.
#
# Outputs, under vm/selfhost/target/:
#   bench-classes/        Bench + CommonWorkloads, compiled against JavaAPI only
#   bench[-O3][.exe]      the native binary
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
OPT="${1:--O3}"
case "$OPT" in -O3) CN1_SELFHOST_CFLAGS="-flto=thin $CN1_SELFHOST_CFLAGS";; esac
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) WINDOWS=1; CPSEP=';'; EXE=.exe ;;
    *) WINDOWS=0; CPSEP=':'; EXE= ;;
esac
PYTHON="$(command -v python3 || command -v python)"
native_path() { if [ "$WINDOWS" = 1 ]; then cygpath -m "$1"; else echo "$1"; fi; }
CC="${CN1_SELFHOST_CC:-clang}"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
[ "$WINDOWS" = 1 ] && J8="$(cygpath -u "$J8")"
OUT="$REPO/vm/selfhost/target"
TRANSLATOR="$REPO/vm/ByteCodeTranslator/target/classes"
JAVAAPI="$OUT/javaapi-classes"
ASM_CP_FILE="$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt"
if [ ! -f "$JAVAAPI/java/lang/Object.class" ] || [ ! -f "$ASM_CP_FILE" ] || \
   [ ! -f "$TRANSLATOR/com/codename1/tools/translator/ByteCodeTranslator.class" ]; then
    echo "build-bench.sh: run build-selfhost.sh first (JavaAPI, translator or ASM classpath missing)"
    exit 1
fi
ASM_CP="$(cat "$ASM_CP_FILE")"

# 1. The benchmark classes, against JavaAPI alone -- the same bootclasspath the
#    translator's own sources are held to, so nothing outside the VM's library slips in.
rm -rf "$OUT/bench-classes"; mkdir -p "$OUT/bench-classes"
"$J8/bin/javac" -nowarn -encoding UTF-8 -source 1.8 -target 1.8 \
    -bootclasspath "$(native_path "$JAVAAPI")" -d "$(native_path "$OUT/bench-classes")" \
    "$(native_path "$REPO/vm/benchmarks/src/com/bench/Bench.java")" \
    "$(native_path "$REPO/vm/benchmarks/common/src/main/java/com/bench/CommonWorkloads.java")"

# 2. translate with the JVM translator, as build-selfhost.sh does.
APP=Bench
rm -rf "$OUT/bench-out"; mkdir -p "$OUT/bench-out"
"$J8/bin/java" -Xmx2g -cp "$(native_path "$TRANSLATOR")$CPSEP$ASM_CP" \
    com.codename1.tools.translator.ByteCodeTranslator \
    clean "$(native_path "$JAVAAPI");$(native_path "$OUT/bench-classes")" \
    "$(native_path "$OUT/bench-out")" "$APP" com.bench "$APP" 1.0 clean none \
    > "$OUT/bench-translate.log" 2>&1 \
    || { echo "TRANSLATE FAILED"; tail -40 "$OUT/bench-translate.log"; exit 1; }

# 3. compile.
BIN="$OUT/bench$( [ "$OPT" = "-O3" ] && echo "-O3" || echo "" )$EXE"
. "$REPO/vm/selfhost/compile-dist.sh"
cn1_compile_dist "$OUT/bench-out/dist" "$APP" "$BIN" "$OPT" "$OUT/bench-cc.log"
