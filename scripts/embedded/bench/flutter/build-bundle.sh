#!/usr/bin/env bash
# Builds the gauge as a release (AOT) flutter-pi bundle in flutter/bundle.
#
#   flutter/build-bundle.sh
#
# Needs Flutter 3.35.4 on the host: the version the engine in flutter/Containerfile
# was built for. A snapshot from a different SDK is refused by that engine.
# flutterpi_tool does not activate with this SDK, so this does what it does by
# hand: compile the app to a product kernel, then turn the kernel into an arm64
# ELF snapshot with the gen_snapshot published beside that engine.
set -euo pipefail
cd "$(dirname "$0")"
FLUTTER="$(dirname "$(dirname "$(readlink -f "$(command -v flutter)")")")"
ENGINE_BINARIES=72209d4   # ardera/flutter-engine-binaries-for-arm, "flutter 3.35.4"
case "$(uname -s)" in
    Darwin) GEN=gen_snapshot_macos_x64_release ;;   # x86_64, runs under Rosetta on Apple silicon
    *)      GEN=gen_snapshot_linux_x64_release ;;
esac
OUT=build/cn1bench
mkdir -p "$OUT" bundle
flutter pub get >/dev/null
flutter build bundle --release --target-platform=linux-arm64 >/dev/null
if [ ! -x "$OUT/$GEN" ]; then
    curl -sSfL -o "$OUT/$GEN" \
        "https://raw.githubusercontent.com/ardera/flutter-engine-binaries-for-arm/$ENGINE_BINARIES/arm64/$GEN"
    chmod +x "$OUT/$GEN"
fi
"$FLUTTER/bin/cache/dart-sdk/bin/dartaotruntime" \
    "$FLUTTER/bin/cache/dart-sdk/bin/snapshots/frontend_server_aot.dart.snapshot" \
    --sdk-root "$FLUTTER/bin/cache/artifacts/engine/common/flutter_patched_sdk_product/" \
    --target=flutter --aot --tfa -Ddart.vm.product=true \
    --packages .dart_tool/package_config.json \
    --output-dill "$OUT/app.dill" package:gauge/main.dart >/dev/null
"$OUT/$GEN" --deterministic --snapshot_kind=app-aot-elf \
    --elf=build/flutter_assets/app.so --strip "$OUT/app.dill"
cp -R build/flutter_assets/. bundle/
echo "flutter/bundle is ready"
