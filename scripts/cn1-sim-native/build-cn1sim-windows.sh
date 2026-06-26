#!/bin/bash
# Cross-compiles cn1sim.dll - the Windows native simulator backend - from
# macOS using clang + lld-link against an `xwin splat` Windows SDK layout
# (the same toolchain the WindowsNativeBuilder uses for cloud builds).
#
# Requirements: brew install llvm xwin; xwin --accept-license splat --output ~/.codenameone/xwin
# Windows JNI headers: copied from a Windows JDK include/ dir (jni.h + win32/jni_md.h),
# default location ~/Downloads/d3dshare/jdk-include.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

MODULE="$REPO_ROOT/maven/cn1-sim-native"
GEN="$MODULE/generator/target/generated-sources/cn1sim-win"
NATIVE_SOURCES="$REPO_ROOT/Ports/WindowsPort/nativeSources"
OUT_DIR="$MODULE/target/windows"
mkdir -p "$OUT_DIR/obj"

XWIN="${CN1_XWIN_SYSROOT:-$HOME/.codenameone/xwin}"
JNI_INC="${CN1_WIN_JNI_INCLUDE:-$HOME/Downloads/d3dshare/jdk-include}"
CLANG="${CLANG:-/opt/homebrew/opt/llvm/bin/clang}"
TRIPLE="x86_64-pc-windows-msvc"

if [ ! -d "$XWIN/crt/include" ]; then
    echo "xwin splat output not found at $XWIN" >&2
    exit 1
fi
if [ ! -f "$JNI_INC/jni.h" ]; then
    echo "Windows JNI headers not found at $JNI_INC" >&2
    exit 1
fi

if [ ! -f "$GEN/cn1sim_jni_windows.m" ]; then
    bash "$SCRIPT_DIR/generate-windows-shims.sh"
fi

INCLUDES=(
    -I"$JNI_INC" -I"$JNI_INC/win32"
    -I"$MODULE/runtime/sim-include"
    -I"$MODULE/runtime"
    -I"$GEN"
    -I"$REPO_ROOT/vm/ByteCodeTranslator/src"
    -I"$NATIVE_SOURCES"
    -isystem "$XWIN/crt/include"
    -isystem "$XWIN/sdk/include/ucrt"
    -isystem "$XWIN/sdk/include/um"
    -isystem "$XWIN/sdk/include/shared"
    -isystem "$XWIN/sdk/include/winrt"
)
CFLAGS=(
    --target=$TRIPLE
    -fms-extensions -fms-compatibility
    -DCN1_SIM_DESKTOP -DNEW_CODENAME_ONE_VM=1 -DCOBJMACROS -DNOMINMAX -DWIN32_LEAN_AND_MEAN
    -DUNICODE -D_UNICODE
    -O1 -g -gcodeview
    -Wno-deprecated-declarations -Wno-nullability-completeness -Wno-ignored-attributes
)
# Unlike the iOS sources, the Windows port sources include cn1_windows.h
# themselves with correct winsock ordering - no prefix include.

# Whole port except the browser (needs the external WebView2 SDK header) -
# its natives fall back to the generated weak stubs.
PORT_SOURCES=()
for f in "$NATIVE_SOURCES"/cn1_windows_*.c "$NATIVE_SOURCES"/cn1_windows_*.cpp; do
    case "$(basename "$f")" in
        cn1_windows_browser.cpp) continue ;;
    esac
    PORT_SOURCES+=("$f")
done

SIM_SOURCES=(
    "$MODULE/runtime/cn1jni_runtime.c"
    "$MODULE/runtime/cn1jni_extras.c"
    "$MODULE/runtime/cn1jni_win_stubs.c"
    "$GEN/cn1sim_jni_windows.m"
    "$GEN/cn1sim_stubs_windows.c"
)

OBJS=()
for src in "${SIM_SOURCES[@]}" "${PORT_SOURCES[@]}"; do
    obj="$OUT_DIR/obj/$(basename "${src%.*}").obj"
    echo "[build-cn1sim-win] CC $(basename "$src")"
    case "$src" in
        *.cpp) "$CLANG" -x c++ -std=c++17 "${CFLAGS[@]}" "${INCLUDES[@]}" -c "$src" -o "$obj" ;;
        *)     "$CLANG" -x c "${CFLAGS[@]}" "${INCLUDES[@]}" -c "$src" -o "$obj" ;;
    esac
    OBJS+=("$obj")
done

echo "[build-cn1sim-win] Linking cn1sim.dll"
"$CLANG" --target=$TRIPLE -shared -fuse-ld=lld -o "$OUT_DIR/cn1sim.dll" "${OBJS[@]}" \
    -L"$XWIN/crt/lib/x86_64" -L"$XWIN/sdk/lib/um/x86_64" -L"$XWIN/sdk/lib/ucrt/x86_64" \
    -ld2d1 -ldwrite -lwindowscodecs -lole32 -luser32 -lgdi32 -lshell32 \
    -lws2_32 -lwinhttp -loleaut32 -ld3d11 -ldxgi -ladvapi32 -lshlwapi -lmfplat -lmf -lmfuuid -lmfreadwrite

echo "[build-cn1sim-win] Built $OUT_DIR/cn1sim.dll"