#!/bin/bash
# Builds the ParparVM translator with ParparVM: its own bytecode, plus ASM's, is
# translated to C and compiled into a native binary.
#
#   build-selfhost.sh [-O1|-O3]        default -O1
#
# Requirements:
#   JDK_8_HOME  a working JDK 8 (JavaAPI and the translator compile with it)
#   clang, and maven on PATH the first time (to resolve ASM)
#
# The mandatory clang flags below are not negotiable for generated C: Java
# arithmetic wraps, and clang -O3 provably miscompiles without -fwrapv
# -fno-strict-aliasing -fno-builtin-fmod(f). See vm/benchmarks/README.md.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
OPT="${1:--O1}"
CC="${CN1_SELFHOST_CC:-clang}"
J8="${JDK_8_HOME:?set JDK_8_HOME to a working JDK 8}"
OUT="$REPO/vm/selfhost/target"
mkdir -p "$OUT"

# 1. translator classes + ASM classpath, built once by maven and then cached.
TRANSLATOR="$REPO/vm/ByteCodeTranslator/target/classes"
if [ ! -f "$TRANSLATOR/com/codename1/tools/translator/ByteCodeTranslator.class" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator -am package -DskipTests)
fi
ASM_CP_FILE="$REPO/vm/ByteCodeTranslator/target/selfhost-asm-classpath.txt"
if [ ! -f "$ASM_CP_FILE" ]; then
    (cd "$REPO/vm" && mvn -q -B -pl ByteCodeTranslator dependency:build-classpath \
        -Dmdep.outputFile=target/selfhost-asm-classpath.txt)
fi
ASM_CP="$(cat "$ASM_CP_FILE")"

# 2. the C runtime the translator emits from its own classpath resources.
for f in cn1_globals.h cn1_globals.m nativeMethods.m cn1_intrinsics.h; do
    cp "$REPO/vm/ByteCodeTranslator/src/$f" "$TRANSLATOR/$f"
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
    "$J8/bin/javac" -nowarn -Xmaxerrs 10000 -source 1.8 -target 1.8 -d "$JAVAAPI" $(cat "$MANIFEST.now")
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

# 5. compile it against JavaAPI ALONE. -Xmaxerrs because javac's default cap of 100
#    silently truncates and makes a large gap look small.
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes"
"$J8/bin/javac" -nowarn -Xmaxerrs 100000 -source 1.8 -target 1.8 \
    -bootclasspath "$JAVAAPI" -cp "$ASM_CP" -d "$OUT/classes" "@$SRCLIST"

# 6. ASM as class files: the translator walks directories, never archives.
rm -rf "$OUT/asm-classes"; mkdir -p "$OUT/asm-classes"
for jar in $(echo "$ASM_CP" | tr ':' '\n' | grep -E 'asm.*\.jar$'); do
    (cd "$OUT/asm-classes" && unzip -oq "$jar" -x 'module-info.class' 'META-INF/*')
done

# 7. translate. The app name has to be the mangled main class: three classes in the
#    set declare main, and ByteCodeClass.addMethod refuses to pick one otherwise.
APP=com_codename1_tools_translator_ByteCodeTranslator
rm -rf "$OUT/out"; mkdir -p "$OUT/out"
"$J8/bin/java" -Xmx4g -cp "$TRANSLATOR:$ASM_CP" com.codename1.tools.translator.ByteCodeTranslator \
    clean "$JAVAAPI;$OUT/asm-classes;$OUT/classes" "$OUT/out" \
    "$APP" com.codename1.tools.translator "$APP" 1.0 clean none \
    > "$OUT/translate.log" 2>&1 \
    || { echo "TRANSLATE FAILED"; tail -40 "$OUT/translate.log"; exit 1; }

# 8. compile. The .S as well as the .c: the virtual-thread context switch is emitted
#    beside the generated sources and the C half references it, so a *.c-only
#    invocation links against a missing cn1VirtualThreadSwitch.
SRCDIR="$OUT/out/dist/$APP-src"
ASMS=$(ls "$SRCDIR"/*.S 2>/dev/null || true)
BIN="$OUT/parpar$( [ "$OPT" = "-O3" ] && echo "-O3" || echo "" )"
$CC $OPT -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf \
    $CN1_SELFHOST_CFLAGS -I"$SRCDIR" "$SRCDIR"/*.c $ASMS -lm -lpthread -o "$BIN" \
    2> "$OUT/cc.log" || { echo "COMPILE FAILED"; tail -40 "$OUT/cc.log"; exit 1; }
echo "built $BIN"
