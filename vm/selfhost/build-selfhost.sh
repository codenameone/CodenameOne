#!/bin/bash
# Builds the ParparVM translator with ParparVM: its own bytecode, plus ASM's, is
# translated to C and compiled into a native binary.
#
#   build-selfhost.sh [-O1|-O3]        default -O1
#
# Requirements:
#   JDK_8_HOME  a working JDK 8 (JavaAPI and the translator compile with it)
#   clang, and maven on PATH the first time (to resolve ASM)
#   Windows (Git Bash): cmake, ninja and clang-cl on PATH, i.e. an MSVC developer
#   environment -- the same toolchain CleanTargetIntegrationTest builds with there
#
# The mandatory clang flags below are not negotiable for generated C: Java
# arithmetic wraps, and clang -O3 provably miscompiles without -fwrapv
# -fno-strict-aliasing -fno-builtin-fmod(f). See vm/benchmarks/README.md.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
OPT="${1:--O1}"
# -O3 implies ThinLTO: that IS the documented release shape (vm/benchmarks/README.md),
# and measured here it is the only rung that beats -O1 -- 1.45s against 1.61s for -O1,
# 1.70s for -O2 and 1.73s for plain -O3. Benchmarking a bare -O3 binary and calling it
# the release build understates it, so the flag is not left to the caller to remember.
case "$OPT" in -O3) CN1_SELFHOST_CFLAGS="-flto=thin $CN1_SELFHOST_CFLAGS";; esac
# WINDOWS (Git Bash) differs in three places, and each fails silently rather than loudly
# if missed: a Java classpath separator is ';', and a colon-joined list of drive-letter
# paths splits at every "C:"; the JDK wants Windows paths; and the native build is the
# translator's own CMake project under clang-cl rather than a direct clang line.
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) WINDOWS=1; CPSEP=';'; EXE=.exe ;;
    *) WINDOWS=0; CPSEP=':'; EXE= ;;
esac
# Windows runners ship `python`, and not always `python3`.
PYTHON="$(command -v python3 || command -v python)"
# `cygpath -m` (C:/x/y), not -w: the JDK and CMake both take forward slashes, and a
# backslash inside a javac @argfile is an escape rather than a separator.
native_path() { if [ "$WINDOWS" = 1 ]; then cygpath -m "$1"; else echo "$1"; fi; }
# A list file the JDK reads itself gets no MSYS path conversion, so convert its lines.
native_list() { if [ "$WINDOWS" = 1 ]; then cygpath -m -f "$1"; else cat "$1"; fi; }
CC="${CN1_SELFHOST_CC:-clang}"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
# setup-java hands Git Bash a backslashed Windows path, which it cannot reliably exec.
[ "$WINDOWS" = 1 ] && J8="$(cygpath -u "$J8")"
OUT="$REPO/vm/selfhost/target"
mkdir -p "$OUT"
CN1_BUILD_SOURCE_SNAPSHOT="$(native_path "$(mktemp -t cn1sources)")"
export CN1_BUILD_SOURCE_SNAPSHOT
"$PYTHON" "$REPO/vm/selfhost/bench-selfhost.py" --snapshot-sources "$CN1_BUILD_SOURCE_SNAPSHOT"

# 1. translator classes + ASM classpath, built once by maven and then cached.
TRANSLATOR="$REPO/vm/ByteCodeTranslator/target/classes"
# Rebuild when the classes are MISSING or STALE. Testing only for existence meant
# that re-running this after editing a translator source silently self-hosted the
# previous build, and the resulting binary was then compared against a JVM side
# built from the new sources -- which reports the intended change as a VM
# divergence. verify-selfhost.sh carries the same guard for the same reason, and
# maven's own incremental check is not enough on its own: it answered "Nothing to
# compile - all classes are up to date" for a source three hours newer than its
# class.
TR_MANIFEST="$REPO/vm/ByteCodeTranslator/target/selfhost-src.manifest"
# The staging copy lives OUTSIDE target/, because `mvn clean` below deletes that whole
# directory -- staging it inside meant the file was gone by the time it was compared and
# moved into place, so the guard rebuilt on every single run and then failed on the
# missing file. Caught by asking the gate to stay SILENT when nothing changed, which is
# the half of a negative control that is easy to skip.
TR_MANIFEST_NOW="$(mktemp -t cn1selfhostmanifest)"
trap 'rm -f "$TR_MANIFEST_NOW" "$CN1_BUILD_SOURCE_SNAPSHOT"' EXIT
find "$REPO/vm/ByteCodeTranslator/src" -type f | sort > "$TR_MANIFEST_NOW"
needs_build=0
if [ ! -f "$TRANSLATOR/com/codename1/tools/translator/ByteCodeTranslator.class" ]; then
    needs_build=1
elif [ ! -f "$TR_MANIFEST" ] || ! cmp -s "$TR_MANIFEST" "$TR_MANIFEST_NOW"; then
    # A MANIFEST DIFF, because -newer cannot see a DELETION. Removing or renaming a
    # source or a runtime resource makes no remaining file newer, so the timestamp test
    # below is satisfied, maven is never re-run, and the deleted file survives in
    # target/classes. The JVM translator then keeps embedding a runtime resource that no
    # longer exists in the tree -- and because BOTH sides of the self-host comparison
    # consume that same stale copy, Gate A still passes. A gate that cannot fail on a
    # deleted file is not covering deletions.
    #
    # Same mechanism the JavaAPI block below already uses, and for the same reason;
    # -type f rather than -name '*.java' because the C runtime ships as classpath
    # resources (cn1_globals.m, nativeMethods.m, java_io_File.m, cn1_win_compat.c ...).
    echo "translator source set changed (file added or removed) -- rebuilding"
    needs_build=1
elif [ -n "$(find "$REPO/vm/ByteCodeTranslator/src" -type f -newer "$TRANSLATOR" -print -quit 2>/dev/null)" ]; then
    # -type f, not -name '*.java'. The translator carries its C runtime as CLASSPATH
    # RESOURCES -- cn1_globals.m, nativeMethods.m, java_io_File.m, cn1_win_compat.c,
    # xmlvm.h and the rest -- and maven copies them into target/classes. Watching
    # only Java sources meant editing any of those left the old copy in place, so
    # the self-hosted binary embedded an obsolete runtime while the JVM side used
    # the new one. That surfaces as a Gate A divergence pointing at the VM, which is
    # exactly the misdiagnosis this guard exists to prevent.
    echo "translator sources or resources are newer than $TRANSLATOR -- rebuilding"
    needs_build=1
fi
if [ "$needs_build" = 1 ]; then
    # `clean` because the incremental check cannot be trusted here; it also removes
    # selfhost-asm-classpath.txt, which the next block regenerates. The clean is what
    # actually evicts a deleted resource from target/classes, so the manifest test above
    # is only useful paired with it.
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator -am clean package -DskipTests)
fi
# Record the manifest only after a build that succeeded -- `set -e` aborts above on
# failure, so reaching here means target/classes matches this file list. Writing it
# earlier would let one failed build convince every later run it was up to date. It has
# to be written AFTER the maven run for the same reason the staging copy is kept out of
# target/: the clean would otherwise remove it.
mkdir -p "$(dirname "$TR_MANIFEST")"
cp -f "$TR_MANIFEST_NOW" "$TR_MANIFEST"
ASM_CP_FILE="$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt"
if [ ! -f "$ASM_CP_FILE" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator dependency:build-classpath \
        -Dmdep.outputFile=target/selfhost-asm-classpath.txt)
fi
ASM_CP="$(cat "$ASM_CP_FILE")"

# 2. the C runtime the translator emits from its own classpath resources.
#
# Copy EVERY non-Java file maven would have staged, not a hand-listed four. The
# list drifts: java_io_File.m, cn1_win_compat.c and xmlvm.h are all read through
# the same classpath lookup, and a hand-written subset silently ships whichever
# ones nobody remembered.
( cd "$REPO/vm/ByteCodeTranslator/src" && find . -type f ! -name '*.java' -print ) \
  | while read -r rel; do
        mkdir -p "$TRANSLATOR/$(dirname "$rel")"
        cp "$REPO/vm/ByteCodeTranslator/src/$rel" "$TRANSLATOR/$rel"
    done

# 3. JavaAPI, rebuilt from source whenever the source set changed.
#
# The presence check alone is not enough, and it fails in a way that looks like a VM
# bug rather than a stale cache: a class compiled before a method stopped being
# native still declares it native, so the translator emits a call to a symbol nothing
# defines. Three things invalidate it and it takes all three -- `-newer` catches an
# edited or added source, but a DELETED one moves no remaining file's timestamp, so
# the sorted manifest is what catches removals. Comparing a file list rather than
# hashing timestamps keeps this portable; `stat` takes -f on BSD and -c on Linux.
JAVAAPI="$OUT/javaapi-classes"
STAMP="$OUT/javaapi-classes.stamp"
MANIFEST="$OUT/javaapi-classes.manifest"
find "$REPO/vm/JavaAPI/src" -name '*.java' | sort > "$MANIFEST.now"
if [ ! -f "$JAVAAPI/java/lang/Object.class" ] || [ ! -f "$STAMP" ] || [ ! -f "$MANIFEST" ] || \
   ! cmp -s "$MANIFEST" "$MANIFEST.now" || \
   [ -n "$(find "$REPO/vm/JavaAPI/src" -name '*.java' -newer "$STAMP" -print -quit 2>/dev/null)" ]; then
    rm -rf "$JAVAAPI"; mkdir -p "$JAVAAPI"
    native_list "$MANIFEST.now" > "$MANIFEST.args"
    "$J8/bin/javac" -nowarn -Xmaxerrs 10000 -source 1.8 -target 1.8 -d "$(native_path "$JAVAAPI")" \
        "@$(native_path "$MANIFEST.args")"
    rm -f "$MANIFEST.args"
    mv "$MANIFEST.now" "$MANIFEST"
    touch "$STAMP"
else
    rm -f "$MANIFEST.now"
fi

# 4. The self-host source set: every translator source except the ones a stub
#    replaces, plus the two classes that carry their own main().
#
#    Only the sources that CANNOT compile against JavaAPI are stubbed, and the list
#    is driven by what is in stubs/ rather than by a name pattern. A blanket
#    "Javascript*" exclusion is what stubbed JavascriptNativeRegistry, which
#    compiles fine and -- as the comment at its call site in Parser warns -- is
#    consulted on EVERY target, not just JavaScript. Answering false there culled
#    java.util.HashMap's getImpl/putImpl/removeImpl/containsKeyImpl/clearImpl and
#    the two helpers only they call, and the native translator emitted seven
#    methods as empty stubs that the JVM one emitted in full.
SRC="$REPO/vm/ByteCodeTranslator/src"
STUBS="$REPO/vm/selfhost/stubs"
STUBBED=$(cd "$STUBS" && find . -name '*.java' | sed 's|.*/||;s|\.java$||' | tr '\n' '|' | sed 's/|$//')
SRCLIST="$OUT/sources.txt"
find "$SRC" -name '*.java' \
  | grep -vE "/($STUBBED)\.java$" \
  | grep -v '/CastSemanticsVerifier\.java$' \
  | grep -v '/NativeSignatureVerifierCli\.java$' > "$SRCLIST"
find "$STUBS" -name '*.java' >> "$SRCLIST"
native_list "$SRCLIST" > "$SRCLIST.args"

# 5. compile it against JavaAPI ALONE. -Xmaxerrs because javac's default cap of 100
#    silently truncates and makes a large gap look small.
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes"
"$J8/bin/javac" -nowarn -Xmaxerrs 100000 -source 1.8 -target 1.8 \
    -bootclasspath "$(native_path "$JAVAAPI")" -cp "$ASM_CP" -d "$(native_path "$OUT/classes")" \
    "@$(native_path "$SRCLIST.args")"

# 6. ASM as class files: the translator walks directories, never archives.
rm -rf "$OUT/asm-classes"; mkdir -p "$OUT/asm-classes"
# `jar xf` rather than unzip, which Git Bash does not reliably ship. The same entries
# are then dropped that the unzip excluded.
echo "$ASM_CP" | tr "$CPSEP" '\n' | grep -E 'asm.*\.jar$' | while read -r jar; do
    (cd "$OUT/asm-classes" && "$J8/bin/jar" xf "$jar")
done
rm -rf "$OUT/asm-classes/META-INF" "$OUT/asm-classes/module-info.class"

# 7. translate. The app name has to be the mangled main class: three classes in the
#    set declare main, and ByteCodeClass.addMethod refuses to pick one otherwise.
APP=com_codename1_tools_translator_ByteCodeTranslator
rm -rf "$OUT/out"; mkdir -p "$OUT/out"
# CN1_SELFHOST_JAVA_OPTS reaches the TRANSLATOR that emits the C, not the C compiler --
# it is how a codegen-level ablation is run. The one that matters for GC work is
# -Dcn1.frameless.objects=false -Dcn1.frameless.instance=false, which reverts to pushing
# every object reference onto threadObjectStack; with frameless codegen on (the default)
# a live reference can exist ONLY in a C local, so any collector that scans the precise
# stack alone will miss it. Word-split on purpose: this is a list of options.
"$J8/bin/java" -Xmx4g $CN1_SELFHOST_JAVA_OPTS -cp "$(native_path "$TRANSLATOR")$CPSEP$ASM_CP" \
    com.codename1.tools.translator.ByteCodeTranslator \
    clean "$(native_path "$JAVAAPI");$(native_path "$OUT/asm-classes");$(native_path "$OUT/classes")" \
    "$(native_path "$OUT/out")" \
    "$APP" com.codename1.tools.translator "$APP" 1.0 clean none \
    > "$OUT/translate.log" 2>&1 \
    || { echo "TRANSLATE FAILED"; tail -40 "$OUT/translate.log"; exit 1; }

# 8. compile. The .S as well as the .c: the virtual-thread context switch is emitted
#    beside the generated sources and the C half references it, so a *.c-only
#    invocation links against a missing cn1VirtualThreadSwitch.
SRCDIR="$OUT/out/dist/$APP-src"
ASMS=$(ls "$SRCDIR"/*.S 2>/dev/null || true)
BIN="$OUT/parpar$( [ "$OPT" = "-O3" ] && echo "-O3" || echo "" )$EXE"
if [ "$WINDOWS" = 1 ]; then
    # The translator's own CMake project, turned into an executable the way
    # CleanTargetIntegrationTest does it, so this compiles exactly what that test proves
    # compiles on Windows. Release plus /clang:$OPT; no ThinLTO, which would need lld-link
    # and is not what the Windows builder ships.
    CMAKE_LISTS="$OUT/out/dist/CMakeLists.txt"
    "$PYTHON" - "$CMAKE_LISTS" <<'PYEOF'
import sys
path = sys.argv[1]
text = open(path, encoding='utf-8').read()
at = text.index('add_library(${PROJECT_NAME}')
end = text.index(')', at)
text = text[:at] + 'add_executable(' + text[at + len('add_library('):end + 1] + text[end + 1:]
open(path, 'w', encoding='utf-8').write(text)
PYEOF
    rm -rf "$OUT/cmake-build"
    cmake -S "$(native_path "$OUT/out/dist")" -B "$(native_path "$OUT/cmake-build")" -G Ninja \
        -DCMAKE_C_COMPILER=clang-cl -DCMAKE_BUILD_TYPE=Release \
        "-DCMAKE_C_FLAGS_RELEASE=/O2 /Ob2 /DNDEBUG /clang:$OPT $CN1_SELFHOST_CFLAGS_WINDOWS" \
        > "$OUT/cc.log" 2>&1 \
        && cmake --build "$(native_path "$OUT/cmake-build")" >> "$OUT/cc.log" 2>&1 \
        || { echo "COMPILE FAILED"; tail -40 "$OUT/cc.log"; exit 1; }
    cp "$OUT/cmake-build/$APP.exe" "$BIN"
    CN1_BUILD_FLAGS="Release /clang:$OPT $CN1_SELFHOST_CFLAGS_WINDOWS" CN1_SELFHOST_CC=clang-cl \
        "$PYTHON" "$REPO/vm/selfhost/bench-selfhost.py" --record-build "$BIN"
    echo "built $BIN"
    exit 0
fi
$CC $OPT -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
    $CN1_SELFHOST_CFLAGS -I"$SRCDIR" "$SRCDIR"/*.c $ASMS -lm -lpthread -o "$BIN" \
    2> "$OUT/cc.log" || { echo "COMPILE FAILED"; tail -40 "$OUT/cc.log"; exit 1; }
CN1_BUILD_FLAGS="$OPT -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf $CN1_SELFHOST_CFLAGS" \
    "$PYTHON" "$REPO/vm/selfhost/bench-selfhost.py" --record-build "$BIN"
echo "built $BIN"
