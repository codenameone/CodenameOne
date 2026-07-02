#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_BIN=${GRADLE:-gradle}

if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Library/Android/sdk" ]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

if [ -z "${WHISPER_CPP_DIR:-}" ] && [ -z "${WHISPER_PREBUILT_DIR:-}" ]; then
    echo "Set WHISPER_CPP_DIR or WHISPER_PREBUILT_DIR before building the AAR." >&2
    exit 2
fi

"$GRADLE_BIN" -p "$SCRIPT_DIR" :cn1-ai-whisper-android:assembleRelease "$@"

mkdir -p "$SCRIPT_DIR/../android/src/main/resources"
cp "$SCRIPT_DIR/cn1-ai-whisper-android/build/outputs/aar/cn1-ai-whisper-android-release.aar" \
   "$SCRIPT_DIR/../android/src/main/resources/cn1-ai-whisper-android.aar"

echo "Wrote $SCRIPT_DIR/../android/src/main/resources/cn1-ai-whisper-android.aar"
