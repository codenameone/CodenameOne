#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <cn1-version-or-tag>" >&2
  exit 1
fi

RAW_VERSION="$1"
RAW_VERSION="${RAW_VERSION#refs/tags/}"
VERSION=""
if [[ "$RAW_VERSION" =~ ^v?([0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*)$ ]]; then
  VERSION="${BASH_REMATCH[1]}"
elif [[ "$RAW_VERSION" =~ ([0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*) ]]; then
  VERSION="${BASH_REMATCH[1]}"
fi
if [ -z "$VERSION" ]; then
  echo "Invalid Codename One version/tag: $RAW_VERSION" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

replace_file() {
  local file="$1"
  local expr="$2"
  perl -0pi -e "$expr" "$file"
}

ROOT_INITIALIZR_DIR="$ROOT_DIR/scripts/initializr"
GENERATOR_MODEL="$ROOT_DIR/scripts/initializr/common/src/main/java/com/codename1/initializr/model/GeneratorModel.java"
MATRIX_TEST="$ROOT_DIR/scripts/initializr/common/src/test/java/com/codename1/initializr/model/GeneratorModelMatrixTest.java"
COMMON_ZIP="$ROOT_DIR/scripts/initializr/common/src/main/resources/common.zip"

while IFS= read -r pom; do
  replace_file "$pom" "s|<cn1\\.plugin\\.version>[^<]+</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"
done < <(find "$ROOT_INITIALIZR_DIR" -name pom.xml -type f)

replace_file "$GENERATOR_MODEL" "s|private static final String CN1_PLUGIN_VERSION = \\\"[^\\\"]+\\\";|private static final String CN1_PLUGIN_VERSION = \\\"$VERSION\\\";|g;"
replace_file "$MATRIX_TEST" "s|<cn1\\.plugin\\.version>[^<]+</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"

if unzip -p "$COMMON_ZIP" pom.xml | grep -q "<cn1.plugin.version>$VERSION</cn1.plugin.version>"; then
  :
else
  TMP_DIR="$(mktemp -d)"
  unzip -q "$COMMON_ZIP" -d "$TMP_DIR"
  replace_file "$TMP_DIR/pom.xml" "s|<cn1\\.plugin\\.version>[^<]+</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"
  (
    cd "$TMP_DIR"
    zip -q -X -r "$COMMON_ZIP" .
  )
  rm -rf "$TMP_DIR"
fi

echo "Updated Initializr Codename One versions to $VERSION"
