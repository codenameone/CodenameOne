#!/usr/bin/env bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <cn1-version-or-tag>" >&2
  exit 1
fi

VERSION="$1"
VERSION="${VERSION#refs/tags/}"
VERSION="${VERSION#v}"

if [[ ! "$VERSION" =~ ^[0-9]+(\.[0-9A-Za-z-]+)+$ ]]; then
  echo "Invalid Codename One version: $VERSION" >&2
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

while IFS= read -r pom; do
  replace_file "$pom" "s|<cn1\\.plugin\\.version>[^<]+</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"
done < <(find "$ROOT_INITIALIZR_DIR" -name pom.xml -type f)

replace_file "$GENERATOR_MODEL" "s|private static final String CN1_PLUGIN_VERSION = \\\"[^\\\"]+\\\";|private static final String CN1_PLUGIN_VERSION = \\\"$VERSION\\\";|g;"
replace_file "$MATRIX_TEST" "s|<cn1\\.plugin\\.version>[^<]+</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"

echo "Updated Initializr Codename One versions to $VERSION"
