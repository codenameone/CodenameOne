#!/bin/bash
# Builds libcn1sim.dylib: the macOS native simulator backend reusing the iOS
# port's Metal rendering pipeline, driven from the JVM via the generated JNI
# shims. Run scripts/cn1-sim-native/generate-ios-shims.sh first (this script
# runs it automatically when the generated sources are missing).
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

MODULE="$REPO_ROOT/maven/cn1-sim-native"
GEN="$MODULE/generator/target/generated-sources/cn1sim"
NATIVE_SOURCES="$REPO_ROOT/Ports/iOSPort/nativeSources"
OUT_DIR="$MODULE/target"
mkdir -p "$OUT_DIR/obj"

if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi
JNI_INC="$JAVA_HOME/include"

if [ ! -f "$GEN/cn1sim_jni_ios.m" ]; then
    bash "$SCRIPT_DIR/generate-ios-shims.sh"
fi

echo "[build-libcn1sim] Generating weak stubs"
BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT
CORE_JAR="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
if [ -d "$REPO_ROOT/maven/core/target/classes" ]; then
    CORE_JAR="$REPO_ROOT/maven/core/target/classes"
fi
JAVAC="${JAVA_HOME}/bin/javac"
JAVA="${JAVA_HOME}/bin/java"
"$JAVAC" -nowarn -cp "$CORE_JAR" -sourcepath "$REPO_ROOT/Ports/iOSPort/src" -d "$BUILD_DIR" \
    "$REPO_ROOT/Ports/iOSPort/src/com/codename1/impl/ios/IOSNative.java" \
    "$MODULE"/generator/src/main/java/com/codename1/simnative/gen/*.java
DEFINED="$BUILD_DIR/defined-symbols.txt"
grep -ohE "com_codename1_impl_ios_IOSNative_[A-Za-z0-9_]+" \
    "$NATIVE_SOURCES"/*.m "$NATIVE_SOURCES"/*.h 2>/dev/null | sort -u > "$DEFINED"
"$JAVA" -cp "$BUILD_DIR:$CORE_JAR" com.codename1.simnative.gen.StubEmitter \
    com.codename1.impl.ios.IOSNative "$GEN" ios "$DEFINED"

echo "[build-libcn1sim] Compiling Metal shaders"
xcrun -sdk macosx metal -c "$NATIVE_SOURCES/CN1MetalShaders.metal" -o "$OUT_DIR/CN1MetalShaders.air"
xcrun -sdk macosx metallib "$OUT_DIR/CN1MetalShaders.air" -o "$OUT_DIR/cn1sim.metallib"

INCLUDES=(
    -I"$JNI_INC" -I"$JNI_INC/darwin"
    -I"$MODULE/runtime/sim-include"
    -I"$MODULE/runtime"
    -I"$GEN"
    -I"$REPO_ROOT/vm/ByteCodeTranslator/src"
    -I"$NATIVE_SOURCES"
)
CFLAGS=(
    -fno-objc-arc -fobjc-exceptions -fmodules
    -DCN1_USE_METAL -DCN1_SIM_DESKTOP
    -O1 -g -fPIC
    -Wno-deprecated-declarations -Wno-nullability-completeness
    # device Xcode projects force-include the VM types via a prefix header;
    # several port headers use JAVA_INT etc. without including anything
    -include "$REPO_ROOT/vm/ByteCodeTranslator/src/cn1_globals.h"
)

# Port sources compiled directly from nativeSources (their includes are clean
# under CN1_SIM_DESKTOP).
PORT_SOURCES=(
    "$NATIVE_SOURCES/CN1Metalcompat.m"
    "$NATIVE_SOURCES/CN1MetalPipelineCache.m"
    "$NATIVE_SOURCES/CN1MetalGlyphAtlas.m"
)

# ExecutableOp sources quote-include CodenameOne_GLViewController.h whose real
# version cannot parse without UIKit. Copy them to a scratch dir so the
# include search falls through to -I, where sim-include's shim header wins.
PORTSRC="$OUT_DIR/portsrc"
mkdir -p "$PORTSRC"
for f in ExecutableOp.m FillRect.m DrawLine.m DrawString.m ClipRect.m GLUIImage.m DrawImage.m TileImage.m PaintOp.m; do
    cp "$NATIVE_SOURCES/$f" "$PORTSRC/$f"
    PORT_SOURCES+=("$PORTSRC/$f")
done

SIM_SOURCES=(
    "$MODULE/runtime/cn1jni_runtime.c"
    "$MODULE/runtime/cn1jni_objc.m"
    "$MODULE/macos/cn1_sim_uikit_compat.m"
    "$MODULE/macos/CN1SimViewController.m"
    "$MODULE/macos/CN1SimImages.m"
    "$MODULE/macos/CN1SimWindow.m"
    "$GEN/cn1sim_jni_ios.m"
    "$GEN/cn1sim_stubs_ios.c"
)

OBJS=()
for src in "${SIM_SOURCES[@]}" "${PORT_SOURCES[@]}"; do
    obj="$OUT_DIR/obj/$(basename "${src%.*}").o"
    echo "[build-libcn1sim] CC $(basename "$src")"
    case "$src" in
        *.c) clang -x c "${CFLAGS[@]}" "${INCLUDES[@]}" -c "$src" -o "$obj" ;;
        *)   clang -x objective-c "${CFLAGS[@]}" "${INCLUDES[@]}" -c "$src" -o "$obj" ;;
    esac
    OBJS+=("$obj")
done

echo "[build-libcn1sim] Linking libcn1sim.dylib"
clang -dynamiclib -o "$OUT_DIR/libcn1sim.dylib" "${OBJS[@]}" \
    -L"$JAVA_HOME/lib" -ljawt -Wl,-rpath,"$JAVA_HOME/lib" \
    -framework Foundation -framework AppKit -framework Metal \
    -framework QuartzCore -framework CoreGraphics -framework CoreText -framework IOSurface \
    -framework WebKit -framework AVFoundation -framework AVKit -framework CoreMedia \
    -framework simd 2>/dev/null || \
clang -dynamiclib -o "$OUT_DIR/libcn1sim.dylib" "${OBJS[@]}" \
    -L"$JAVA_HOME/lib" -ljawt -Wl,-rpath,"$JAVA_HOME/lib" \
    -framework Foundation -framework AppKit -framework Metal \
    -framework QuartzCore -framework CoreGraphics -framework CoreText -framework IOSurface \
    -framework WebKit -framework AVFoundation -framework AVKit -framework CoreMedia

echo "[build-libcn1sim] Built $OUT_DIR/libcn1sim.dylib and $OUT_DIR/cn1sim.metallib"