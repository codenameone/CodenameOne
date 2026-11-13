#!/bin/bash
# Builds the whisper.cpp / llama.cpp artifacts PlatformFeatureCatalog names,
# from the upstream tags pinned in versions.properties, and stages them as a
# Maven repository layout (with .sha256/.sha1/.md5 beside every file) under
# OUT_DIR, ready to copy to repo.codenameone.com/maven2.
#
#   scripts/ai-natives/build.sh <whisper|llama> <ios|android|all> [OUT_DIR]
#
# iOS needs macOS with Xcode; Android needs ANDROID_HOME with NDK
# 28.2.13676358, JDK 17 and Gradle. Upstream sources are cloned into
# $WORK_DIR (default: build/ under this directory).
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
LIB=${1:?usage: build.sh <whisper|llama> <ios|android|all> [OUT_DIR]}
TARGET=${2:?usage: build.sh <whisper|llama> <ios|android|all> [OUT_DIR]}
OUT_DIR=$(mkdir -p "${3:-$HERE/build/repo}" && cd "${3:-$HERE/build/repo}" && pwd)
WORK_DIR=${WORK_DIR:-$HERE/build}
GROUP_PATH=com/codenameone/natives
IOS_MIN=13.0

prop() { sed -n "s/^$1=//p" "$HERE/versions.properties"; }
case "$LIB" in
    whisper) REPO=https://github.com/ggml-org/whisper.cpp.git; NAME=CN1Whisper ;;
    llama) REPO=https://github.com/ggml-org/llama.cpp.git; NAME=CN1Llama ;;
    *) echo "unknown library $LIB" >&2; exit 2 ;;
esac
TAG=$(prop "$LIB.tag")
VERSION="${TAG#v}-$(prop "$LIB.revision")"
SRC="$WORK_DIR/$LIB-$TAG"
mkdir -p "$WORK_DIR"
if [ ! -d "$SRC" ]; then
    git clone --quiet --depth 1 --branch "$TAG" "$REPO" "$SRC"
fi

# Writes the artifact plus a minimal pom and the checksums Maven clients and
# CatalogNatives verify.
stage() {
    local artifact=$1 file=$2 ext=$3
    local dir="$OUT_DIR/$GROUP_PATH/$artifact/$VERSION"
    mkdir -p "$dir"
    cp "$file" "$dir/$artifact-$VERSION.$ext"
    cat > "$dir/$artifact-$VERSION.pom" <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.codenameone.natives</groupId>
  <artifactId>$artifact</artifactId>
  <version>$VERSION</version>
  <packaging>$ext</packaging>
  <description>$LIB.cpp $TAG for Codename One</description>
  <licenses><license><name>MIT</name></license></licenses>
</project>
POM
    for f in "$dir/$artifact-$VERSION.$ext" "$dir/$artifact-$VERSION.pom"; do
        shasum -a 256 "$f" | cut -d' ' -f1 > "$f.sha256"
        shasum -a 1 "$f" | cut -d' ' -f1 > "$f.sha1"
        md5 -q "$f" > "$f.md5" 2>/dev/null || md5sum "$f" | cut -d' ' -f1 > "$f.md5"
    done
    echo "Staged $dir/$artifact-$VERSION.$ext"
}

build_ios_slice() {
    local sdk=$1 archs=$2 out=$3
    local sysroot
    sysroot=$(xcrun --sdk "$sdk" --show-sdk-path)
    local objs=()
    for arch in $archs; do
        local b="$WORK_DIR/ios-$LIB-$sdk-$arch"
        cmake -S "$SRC" -B "$b" -G "Unix Makefiles" \
            -DCMAKE_SYSTEM_NAME=iOS -DCMAKE_OSX_SYSROOT="$sdk" \
            -DCMAKE_OSX_ARCHITECTURES="$arch" -DCMAKE_OSX_DEPLOYMENT_TARGET=$IOS_MIN \
            -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=OFF \
            -DGGML_METAL=ON -DGGML_METAL_EMBED_LIBRARY=ON -DGGML_OPENMP=OFF \
            -DGGML_NATIVE=OFF -DGGML_BLAS=ON \
            -DCMAKE_C_FLAGS="-fno-common -fvisibility=hidden" \
            -DCMAKE_CXX_FLAGS="-fvisibility=hidden -fvisibility-inlines-hidden" \
            -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_EXAMPLES=OFF -DWHISPER_BUILD_SERVER=OFF \
            -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DLLAMA_BUILD_SERVER=OFF \
            -DLLAMA_BUILD_TOOLS=OFF -DLLAMA_CURL=OFF > "$b.cmake.log"
        cmake --build "$b" --config Release --target "$LIB" -j "$(sysctl -n hw.ncpu)" \
            > "$b.build.log"
        xcrun --sdk "$sdk" clang++ -std=c++17 -O2 -arch "$arch" -isysroot "$sysroot" \
            -m"$( [ "$sdk" = iphoneos ] && echo iphoneos || echo ios-simulator )"-version-min=$IOS_MIN \
            -fvisibility-inlines-hidden -I "$SRC/include" -I "$SRC/ggml/include" \
            -I "$HERE/$LIB/src" -c "$HERE/$LIB/src/cn1_$LIB.cpp" -o "$b/cn1_$LIB.o"
        # One relocatable object holding our API, upstream and ggml, with
        # every global except cn1<lib>_* made private: the app may link both
        # archives, and each carries its own ggml.
        local libs=()
        while IFS= read -r a; do libs+=("-Wl,-force_load,$a"); done \
            < <(find "$b" -name '*.a' | sort)
        printf '_cn1%s_*\n' "$LIB" > "$b/exports.txt"
        xcrun --sdk "$sdk" clang++ -r -nostdlib -arch "$arch" -isysroot "$sysroot" \
            -m"$( [ "$sdk" = iphoneos ] && echo iphoneos || echo ios-simulator )"-version-min=$IOS_MIN \
            -Wl,-exported_symbols_list,"$b/exports.txt" \
            "$b/cn1_$LIB.o" "${libs[@]}" -o "$b/prelinked.o"
        # Fail here rather than ship an archive whose API is invisible or
        # whose ggml still collides with the other library's.
        if ! nm -gU "$b/prelinked.o" | grep -q " T _cn1${LIB}_"; then
            echo "ERROR: $b/prelinked.o exports no cn1${LIB}_* symbol" >&2; exit 1
        fi
        if nm -gU "$b/prelinked.o" | grep -v " _cn1${LIB}_" | grep -q .; then
            echo "ERROR: $b/prelinked.o exports more than cn1${LIB}_*:" >&2
            nm -gU "$b/prelinked.o" | grep -v " _cn1${LIB}_" | head >&2; exit 1
        fi
        objs+=("$b/prelinked.o")
    done
    mkdir -p "$out"
    if [ ${#objs[@]} -gt 1 ]; then
        lipo -create "${objs[@]}" -output "$out/prelinked.o"
    else
        cp "${objs[0]}" "$out/prelinked.o"
    fi
    rm -f "$out/lib$NAME.a"
    xcrun libtool -static -o "$out/lib$NAME.a" "$out/prelinked.o"
    mkdir -p "$out/Headers"
    cp "$HERE/$LIB/src/cn1_$LIB.h" "$out/Headers/"
}

build_ios() {
    local stageDir="$WORK_DIR/ios-$LIB-out"
    rm -rf "$stageDir"
    build_ios_slice iphoneos arm64 "$stageDir/device"
    build_ios_slice iphonesimulator "arm64 x86_64" "$stageDir/simulator"
    xcodebuild -create-xcframework \
        -library "$stageDir/device/lib$NAME.a" -headers "$stageDir/device/Headers" \
        -library "$stageDir/simulator/lib$NAME.a" -headers "$stageDir/simulator/Headers" \
        -output "$stageDir/$NAME.xcframework" > /dev/null
    (cd "$stageDir" && ditto -c -k --keepParent "$NAME.xcframework" "$NAME.xcframework.zip")
    stage "cn1-$LIB-ios" "$stageDir/$NAME.xcframework.zip" zip
}

build_android() {
    local prop
    prop=$( [ "$LIB" = whisper ] && echo whisperCppDir || echo llamaCppDir )
    "${GRADLE:-gradle}" -p "$HERE/android" ":cn1-$LIB-android:assembleRelease" \
        "-P$prop=$SRC" --quiet
    local aar="$HERE/android/cn1-$LIB-android/build/outputs/aar/cn1-$LIB-android-release.aar"
    python3 "$HERE/../check-16k-page-alignment.py" "$aar"
    stage "cn1-$LIB-android" "$aar" aar
}

case "$TARGET" in
    ios) build_ios ;;
    android) build_android ;;
    all) build_ios; build_android ;;
    *) echo "unknown target $TARGET" >&2; exit 2 ;;
esac
