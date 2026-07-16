#!/bin/bash
# Translates one benchmark/torture class through ParparVM's clean target and
# compiles it into a native binary.
#
#   translate-and-build.sh <MainSimpleClassName> <outBinary> [extra clang flags...]
#
# Requirements:
#   - JDK_8_HOME pointing at a JDK 8 (JavaAPI + bench sources compile with it)
#   - Maven on PATH (used once to build the translator + resolve ASM)
#   - clang on PATH
#
# Environment knobs:
#   CN1_BENCH_CFLAGS  extra clang flags (e.g. -flto=thin for the release shape)
#   CN1_BENCH_CC      compiler (default clang)
set -e
cd "$(dirname "$0")"
MAIN="$1"; shift
OUTBIN="$1"; shift
EXTRA="$@"

REPO="$(cd ../.. && pwd)"
CC="${CN1_BENCH_CC:-clang}"
J8="${JDK_8_HOME:?set JDK_8_HOME to a JDK 8 home}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1bench.XXXXXX")"

# 1. translator classes + ASM classpath (built once, then cached)
TRANSLATOR="$REPO/vm/ByteCodeTranslator/target/classes"
if [ ! -f "$TRANSLATOR/com/codename1/tools/translator/ByteCodeTranslator.class" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator -am package -DskipTests)
fi
ASM_CP_FILE="$REPO/vm/ByteCodeTranslator/target/bench-asm-classpath.txt"
if [ ! -f "$ASM_CP_FILE" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator dependency:build-classpath -Dmdep.outputFile=target/bench-asm-classpath.txt)
fi
ASM_CP="$(cat "$ASM_CP_FILE")"

# 2. sync the C runtime resources the translator emits from its classpath
for f in cn1_globals.h cn1_globals.m nativeMethods.m cn1_intrinsics.h; do
    cp "$REPO/vm/ByteCodeTranslator/src/$f" "$TRANSLATOR/$f"
done

# 3. JavaAPI classes (built once, then cached)
JAVAAPI="$REPO/vm/benchmarks/target/javaapi-classes"
if [ ! -f "$JAVAAPI/java/lang/Object.class" ]; then
    mkdir -p "$JAVAAPI"
    "$J8/bin/javac" -nowarn -source 1.8 -target 1.8 -d "$JAVAAPI" \
        $(find "$REPO/vm/JavaAPI/src" -name '*.java')
fi

# 4. compile the benchmark class against JavaAPI only. Bench is shared with
# the generated port application; torture programs remain in src/com/bench.
mkdir -p "$WORK/classes"
SOURCE="src/com/bench/$MAIN.java"
[ "$MAIN" = "Bench" ] && SOURCE="common/src/main/java/com/bench/Bench.java"
"$J8/bin/javac" -nowarn -encoding UTF-8 -bootclasspath "$JAVAAPI" -source 1.8 -target 1.8 \
    -d "$WORK/classes" "$SOURCE"

# 5. translate to C
mkdir -p "$WORK/out"
"$J8/bin/java" -cp "$TRANSLATOR:$ASM_CP" com.codename1.tools.translator.ByteCodeTranslator \
    clean "$JAVAAPI;$WORK/classes" "$WORK/out" "$MAIN" com.bench "$MAIN" 1.0 clean none \
    > "$WORK/translate.log" 2>&1 || { echo "TRANSLATE FAILED"; tail -30 "$WORK/translate.log"; exit 1; }

# 6. compile. -fwrapv -fno-strict-aliasing -fno-builtin-fmod(f) are MANDATORY
#    for generated C (Java wrapping arithmetic; clang -O3 provably miscompiles
#    without them). ThinLTO (-flto=thin, clang only) is the release shape.
SRCDIR="$WORK/out/dist/$MAIN-src"
$CC -O3 -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
    $CN1_BENCH_CFLAGS $EXTRA -I"$SRCDIR" "$SRCDIR"/*.c -lm -lpthread -o "$OUTBIN" \
    2> "$WORK/cc.log" || { echo "COMPILE FAILED"; tail -30 "$WORK/cc.log"; exit 1; }
echo "built $OUTBIN (workdir $WORK)"
