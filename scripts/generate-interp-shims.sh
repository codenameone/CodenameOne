#!/bin/bash
# Regenerates the device runtime's shim classes.
#
# The shims are checked in rather than generated during the build: they are the
# app's compiled contract with pushed code, and a build-time generator would let
# that contract drift silently between the machine that pushes and the app that
# was installed from a store. Regenerate deliberately and commit the result.
#
# The set is not curated. It is every public, non-final, constructible class and
# every public interface under com.codename1, because an application may
# subclass anything the API exposes and a hand-maintained list is wrong the
# first time somebody subclasses something unusual.
#
# Every shim that can exist is generated, and every one of them compiles. The
# only classes skipped are those Java itself forbids subclassing from another
# package -- a package-private abstract method leaves no legal subclass outside
# its own package -- and the generator names the method when it skips one.
#
# There is deliberately no drop-what-fails fallback. A shim that will not
# compile is a generator bug; hiding it behind a prune loop is how Interp_ui_Form
# went missing once already.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP="$ROOT/scripts/cn1-device-runtime"
OUT="$APP/common/src/main/java"
GEN="$OUT/com/codenameone/devruntime/gen"
CORE_JAR="$ROOT/maven/core/target/codenameone-core-8.0-SNAPSHOT.jar"
# The device's java.* API, as the application tool chain sees it. Not the JDK
# (thousands of types the device lacks) and not vm/JavaAPI (which has types the
# app classpath does not expose, ReentrantLock among them).
JAVA_RUNTIME="$(ls "$ROOT"/.m2-local/com/codenameone/java-runtime/*/java-runtime-*.jar 2>/dev/null \
    | grep -vE 'sources|javadoc' | head -1)"
if [ -z "$JAVA_RUNTIME" ]; then
    JAVA_RUNTIME="$(ls "$HOME"/.m2/repository/com/codenameone/java-runtime/*/java-runtime-*.jar 2>/dev/null \
        | grep -vE 'sources|javadoc' | head -1)"
fi
if [ -z "$JAVA_RUNTIME" ]; then
    echo "codenameone-java-runtime not found; build it first" >&2
    exit 1
fi

if [ ! -f "$CORE_JAR" ]; then
    echo "core jar not built: $CORE_JAR" >&2
    echo "build it first:  mvn -f maven/pom.xml -pl core install -DskipTests" >&2
    exit 1
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Generate with the JDK the app is compiled with, not whichever one happens to
# be on PATH. The java.* shims are derived from reflection over the running
# JDK's classes, and the JDKs disagree: LambdaMetafactory is non-final on 8 and
# final on 17, Thread's methods differ. Generating on one and compiling on
# another produces shims that cannot exist.
if [ -n "${JAVA17_HOME:-}" ]; then
    export JAVA_HOME="$JAVA17_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# ASM reads the device runtime's method tables; reflection over the running JDK
# reports methods the phone does not have.
ASM="$(find "$ROOT/.m2-local/org/ow2/asm" "$HOME/.m2/repository/org/ow2/asm" \
    -name 'asm-9.8.jar' 2>/dev/null | head -1)"
if [ -z "$ASM" ]; then
    echo "asm not found in the local repositories" >&2
    exit 1
fi
javac -nowarn -d "$WORK" -cp "$CORE_JAR:$ASM" \
    "$ROOT/scripts/hellocodenameone/tools/src/main/java/com/codenameone/devruntime/tools/GenerateInterpShims.java"

rm -f "$GEN"/Interp_*.java "$GEN"/InterpShimRegistry.java
SEED="$ROOT/scripts/hellocodenameone/tools/unshimmable-by-contract.txt"
SEED_ARG=()
[ -f "$SEED" ] && SEED_ARG=(--exclude "$SEED")
java -cp "$WORK:$CORE_JAR:$ASM" com.codenameone.devruntime.tools.GenerateInterpShims \
    "$OUT" "$CORE_JAR" --java-runtime "$JAVA_RUNTIME" "${SEED_ARG[@]}"

# Verify. Every generated shim must compile: a shim that does not is a bug in
# the generator, not a property of the framework. That is not a theoretical
# distinction -- an earlier compile-and-drop loop silently ate Interp_ui_Form
# because Form re-declares getComponentForm() final, and a device runtime that
# cannot subclass Form is useless.
#
# Classes Java genuinely forbids subclassing across packages (a package-private
# abstract method) are excluded by the generator, up front, naming the method.
# Anything else reaching javac is a defect and fails the build here.
CLASSES="$WORK/classes"
mkdir -p "$CLASSES"
if ! javac -nowarn -proc:none -d "$CLASSES" -cp "$CORE_JAR" "$GEN"/*.java 2> "$WORK/errors.txt"; then
    echo "generated shims do not compile -- this is a generator bug, not a limitation" >&2
    grep -E "error:" "$WORK/errors.txt" | head -20 >&2
    echo >&2
    echo "offending shims:" >&2
    grep -oE "Interp_[A-Za-z0-9_]+\.java" "$WORK/errors.txt" | sort -u | head -20 >&2
    exit 1
fi

# Pruning is gone, so the core-type guard is belt and braces rather than the
# thing standing between us and a useless build. Keep it: it costs nothing and
# it is the assertion that would have caught the Form regression immediately.
#
# Each java.* entry below is a gap that actually shipped, not a hypothetical.
# Runnable went missing when the curated list was replaced by a scan of
# com.codename1 alone, and every exception type went missing while a difference
# between the JDK's Throwable and the device's was treated as a reason to skip
# the class. Both took a device run to notice. This is the check that makes the
# next one cost a second instead.
for required in Interp_ui_Component Interp_ui_Container Interp_ui_Form \
                Interp_ui_Label Interp_ui_Button Interp_ui_Dialog \
                Interp_I_ui_events_ActionListener \
                Interp_I_java_lang_Runnable \
                Interp_java_lang_Exception Interp_java_lang_RuntimeException \
                Interp_java_lang_Thread Interp_java_util_ArrayList; do
    if [ ! -f "$GEN/$required.java" ]; then
        echo "$required was not generated -- that is a generator bug" >&2
        exit 1
    fi
done

# Regenerating has to be a no-op, or the checked-in shims and the generator
# quietly disagree and every future diff is noise. Reflection does not promise
# an order for declared methods and really does vary run to run, which is why
# the generator sorts them.
SECOND="$WORK/second"
mkdir -p "$SECOND"
java -cp "$WORK:$CORE_JAR:$ASM" com.codenameone.devruntime.tools.GenerateInterpShims \
    "$SECOND" "$CORE_JAR" --java-runtime "$JAVA_RUNTIME" "${SEED_ARG[@]}" > /dev/null
if ! diff -rq "$SECOND/com/codenameone/devruntime/gen" "$GEN" > "$WORK/unstable.txt"; then
    echo "generation is not reproducible -- the same inputs produced different shims:" >&2
    head -5 "$WORK/unstable.txt" >&2
    exit 1
fi

ls "$GEN"/Interp_*.java | wc -l | xargs echo "shims:"
