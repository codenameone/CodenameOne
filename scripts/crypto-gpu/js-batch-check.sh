#!/usr/bin/env bash
#
# Translates Ports/JavaScriptPort/tests/fixtures/JavaScriptCryptoBatchApp with the JavaScript
# backend and runs it in headless Chromium with WebGPU, through js-batch-check.mjs: every
# asynchronous CryptoBatch answer the GPU gives must be the portable engine's, and the line per
# kernel carries both rates.
#
#   scripts/crypto-gpu/js-batch-check.sh [build-dir] [entries]
#
# Needs JDK 8 (tools/env.sh), node and Playwright (PLAYWRIGHT_MODULE, CHROMIUM_PATH as for
# webgpu-check.mjs). Maven resolves from MAVEN_REPO_LOCAL when it is set.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BUILD_DIR="${1:-$(mktemp -d "${TMPDIR:-/tmp}/cn1-crypto-js-XXXXXX")}"
ENTRIES="${2:-4096}"
REPO_ARG=()
if [ -n "${MAVEN_REPO_LOCAL:-}" ]; then
  REPO_ARG=("-Dmaven.repo.local=$MAVEN_REPO_LOCAL")
fi
mvn -B -q ${REPO_ARG[@]+"${REPO_ARG[@]}"} -f "$ROOT/maven/pom.xml" -pl parparvm -am -DskipTests \
  -Dmaven.javadoc.skip=true package
mkdir -p "$BUILD_DIR/classes"
BUILD_DIR="$(cd "$BUILD_DIR" && pwd)"
BUNDLE="$ROOT/maven/parparvm/target/bundle"
CORE="$ROOT/maven/core/target/codenameone-core-8.0-SNAPSHOT.jar"
(
  cd "$BUILD_DIR/classes"
  jar xf "$CORE"
  jar xf "$BUNDLE/parparvm-java-api.jar"
)
cp -R "$ROOT/maven/parparvm/target/javascript-port-classes"/. "$BUILD_DIR/classes/"
javac -encoding UTF-8 -source 8 -target 8 -cp "$CORE:$BUNDLE/JavaScriptPort.jar" \
  -d "$BUILD_DIR/classes" "$ROOT/Ports/JavaScriptPort/tests/fixtures/JavaScriptCryptoBatchApp.java"
java -Dcodename1.javascriptport.webapp="$ROOT/Ports/JavaScriptPort/src/main/webapp" \
  -cp "$BUNDLE/parparvm-compiler.jar" com.codename1.tools.translator.ByteCodeTranslator \
  javascript "$BUILD_DIR/classes" "$BUILD_DIR/output" JavaScriptCryptoBatchApp \
  com.codename1.jsfixture CryptoBatch 1.0 ios none
DIST="$BUILD_DIR/output/dist/JavaScriptCryptoBatchApp-js"
mkdir -p "$DIST/assets"
cp "$BUILD_DIR/classes/material-design-font.ttf" "$DIST/assets/"
if [ ! -f "$DIST/js/cn1_crypto_gpu.js" ]; then
  echo "the build dropped js/cn1_crypto_gpu.js from an application that runs CryptoBatch" >&2
  exit 1
fi
node "$ROOT/scripts/crypto-gpu/js-batch-check.mjs" "$DIST" "$ENTRIES"
