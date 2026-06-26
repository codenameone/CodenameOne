#!/bin/bash
# Generates the JNI shim layer for the Windows port's WindowsNative class and
# cross-checks the generated symbol names against the symbols defined by
# Ports/WindowsPort/nativeSources.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CORE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
if [ -d "$REPO_ROOT/maven/core/target/classes" ]; then
    CORE_JAR="$REPO_ROOT/maven/core/target/classes"
fi

OUT_DIR="${1:-$REPO_ROOT/maven/cn1-sim-native/generator/target/generated-sources/cn1sim-win}"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

echo "[generate-windows-shims] Compiling WindowsNative + generator"
"$JAVAC" -nowarn -cp "$CORE_JAR" -sourcepath "$REPO_ROOT/Ports/WindowsPort/src" -d "$BUILD_DIR" \
    "$REPO_ROOT/Ports/WindowsPort/src/com/codename1/impl/windows/WindowsNative.java" \
    "$REPO_ROOT"/maven/cn1-sim-native/generator/src/main/java/com/codename1/simnative/gen/*.java

echo "[generate-windows-shims] Extracting defined symbols from nativeSources"
DEFINED="$BUILD_DIR/defined-symbols.txt"
grep -rohE "com_codename1_impl_windows_WindowsNative_[A-Za-z0-9_]+" \
    "$REPO_ROOT"/Ports/WindowsPort/nativeSources/ 2>/dev/null | sort -u > "$DEFINED"

echo "[generate-windows-shims] Generating shims into $OUT_DIR"
"$JAVA" -cp "$BUILD_DIR:$CORE_JAR" com.codename1.simnative.gen.ShimGenerator \
    com.codename1.impl.windows.WindowsNative "$OUT_DIR" windows "$DEFINED"
"$JAVA" -cp "$BUILD_DIR:$CORE_JAR" com.codename1.simnative.gen.StubEmitter \
    com.codename1.impl.windows.WindowsNative "$OUT_DIR" windows "$DEFINED"

TOTAL=$(wc -l < "$OUT_DIR/cn1sim_symbols_windows.txt" | tr -d ' ')
UNRESOLVED=$(wc -l < "$OUT_DIR/cn1sim_unresolved_windows.txt" | tr -d ' ')
echo "[generate-windows-shims] $TOTAL methods, $UNRESOLVED unresolved (weak-stubbed)"
if [ "$UNRESOLVED" -gt 0 ]; then
    cat "$OUT_DIR/cn1sim_unresolved_windows.txt"
fi
if [ "$UNRESOLVED" -gt $((TOTAL / 20)) ]; then
    echo "[generate-windows-shims] More than 5% unresolved - mangling regression suspected" >&2
    exit 1
fi
echo "[generate-windows-shims] Symbol resolution OK"