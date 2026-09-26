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
#   CN1_BENCH_TRANSLATOR_OPTS  extra -D properties for the translator JVM
#                     (e.g. -Dcn1.checkedCasts=true)
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

# 1. Reuse translator classes only for identical sources. A presence-only cache
# silently runs old code after an edit; Maven incremental compilation also misses
# some source deletions, so changed fingerprints require a clean module build.
TRANSLATOR="$REPO/vm/ByteCodeTranslator/target/classes"
TRANSLATOR_STAMP="$REPO/vm/ByteCodeTranslator/target/bench-translator.sha256"
TRANSLATOR_SHA="$(python3 - "$REPO" <<'PYHASH'
import hashlib, pathlib, sys
root = pathlib.Path(sys.argv[1]) / 'vm/ByteCodeTranslator'
h = hashlib.sha256()
for path in sorted(list((root / 'src').rglob('*')) + [root / 'pom.xml', root.parent / 'pom.xml']):
    if path.is_file():
        h.update(str(path.relative_to(root.parent)).encode())
        h.update(path.read_bytes())
print(h.hexdigest())
PYHASH
)"
if [ ! -f "$TRANSLATOR/com/codename1/tools/translator/ByteCodeTranslator.class" ] ||
   [ ! -f "$TRANSLATOR_STAMP" ] || [ "$(cat "$TRANSLATOR_STAMP")" != "$TRANSLATOR_SHA" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator -am clean package -DskipTests)
    printf '%s\n' "$TRANSLATOR_SHA" > "$TRANSLATOR_STAMP"
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

# 3. JavaAPI classes (cached, but INVALIDATED whenever the source set changes).
# The presence check alone is not enough and fails in a way that looks like a VM bug: when
# Thread.sleep(long) stopped being a native and became Java calling sleepImpl, a cache from
# before that change still declared it native, so the translator emitted a call to
# java_lang_Thread_sleep___long and nothing defined it -- an undefined-symbol link error in
# generated code, with no hint that the cause was a stale directory.
#
# Three things invalidate it, and it takes all three. `-newer` catches an edited or added
# source, but a DELETED one moves no remaining file's timestamp, so the cache would keep
# serving a class whose source no longer exists -- the same stale-cache failure in a
# different disguise. The sorted manifest catches that, and additions with it. Comparing a
# file list rather than hashing timestamps keeps this portable: `stat` takes -f on BSD and
# -c on Linux, and this script runs on both.
JAVAAPI="$REPO/vm/benchmarks/target/javaapi-classes"
JAVAAPI_STAMP="$REPO/vm/benchmarks/target/javaapi-classes.stamp"
JAVAAPI_MANIFEST="$REPO/vm/benchmarks/target/javaapi-classes.manifest"
mkdir -p "$REPO/vm/benchmarks/target"
find "$REPO/vm/JavaAPI/src" -name '*.java' | sort > "$JAVAAPI_MANIFEST.now"
if [ ! -f "$JAVAAPI/java/lang/Object.class" ] || \
   [ ! -f "$JAVAAPI_STAMP" ] || \
   [ ! -f "$JAVAAPI_MANIFEST" ] || \
   ! cmp -s "$JAVAAPI_MANIFEST" "$JAVAAPI_MANIFEST.now" || \
   [ -n "$(find "$REPO/vm/JavaAPI/src" -name '*.java' -newer "$JAVAAPI_STAMP" -print -quit 2>/dev/null)" ]; then
    rm -rf "$JAVAAPI"
    mkdir -p "$JAVAAPI"
    "$J8/bin/javac" -nowarn -source 1.8 -target 1.8 -d "$JAVAAPI" \
        $(cat "$JAVAAPI_MANIFEST.now")
    mv "$JAVAAPI_MANIFEST.now" "$JAVAAPI_MANIFEST"
    touch "$JAVAAPI_STAMP"
else
    rm -f "$JAVAAPI_MANIFEST.now"
fi

# 4. compile the benchmark class against JavaAPI only. Bench is shared with
# the generated port application; torture programs remain in src/com/bench.
mkdir -p "$WORK/classes"
SOURCES=("src/com/bench/$MAIN.java")
if [ "$MAIN" = "Bench" ]; then
    SOURCES+=("common/src/main/java/com/bench/CommonWorkloads.java")
fi
"$J8/bin/javac" -nowarn -encoding UTF-8 -bootclasspath "$JAVAAPI" -source 1.8 -target 1.8 \
    -d "$WORK/classes" "${SOURCES[@]}"

# 5. translate to C
mkdir -p "$WORK/out"
"$J8/bin/java" $CN1_BENCH_TRANSLATOR_OPTS -cp "$TRANSLATOR:$ASM_CP" com.codename1.tools.translator.ByteCodeTranslator \
    clean "$JAVAAPI;$WORK/classes" "$WORK/out" "$MAIN" com.bench "$MAIN" 1.0 clean none \
    > "$WORK/translate.log" 2>&1 || { echo "TRANSLATE FAILED"; tail -30 "$WORK/translate.log"; exit 1; }

# 6. compile. -fwrapv -fno-strict-aliasing -fno-builtin-fmod(f) are MANDATORY
#    for generated C (Java wrapping arithmetic; clang -O3 provably miscompiles
#    without them). ThinLTO (-flto=thin, clang only) is the release shape.
SRCDIR="$WORK/out/dist/$MAIN-src"
# The .S as well as the .c. The translator emits the virtual-thread context switch
# beside the generated sources, and on aarch64/x86_64 the C half references it, so
# a *.c-only invocation links against a missing cn1VirtualThreadSwitch. The CMake
# and Xcode project generators had the identical omission; this is the third place
# that had to learn the same thing. nullglob keeps the argument from expanding to a
# literal "*.S" on a target where no assembly is emitted.
shopt -s nullglob
ASM=("$SRCDIR"/*.S)
shopt -u nullglob
$CC -O3 -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
    $CN1_BENCH_CFLAGS $EXTRA -I"$SRCDIR" "$SRCDIR"/*.c "${ASM[@]}" -lm -lpthread -o "$OUTBIN" \
    2> "$WORK/cc.log" || { echo "COMPILE FAILED"; tail -30 "$WORK/cc.log"; exit 1; }
echo "built $OUTBIN (workdir $WORK)"
