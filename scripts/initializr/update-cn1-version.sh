#!/usr/bin/env bash
set -euo pipefail

extract_version() {
  local raw="$1"
  local parsed=""
  raw="${raw#refs/tags/}"
  if [[ "$raw" =~ ^v?([0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*)$ ]]; then
    parsed="${BASH_REMATCH[1]}"
  elif [[ "$raw" =~ ([0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*) ]]; then
    parsed="${BASH_REMATCH[1]}"
  fi
  echo "$parsed"
}

latest_plugin_version_from_maven() {
  local metadata
  metadata="$(curl -fsSL https://repo1.maven.org/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml || true)"
  if [ -z "$metadata" ]; then
    return 1
  fi
  local version
  version="$(printf '%s' "$metadata" | perl -ne 'if (m|<release>([^<]+)</release>|) { print $1; exit 0 }')"
  if [ -z "$version" ]; then
    version="$(printf '%s' "$metadata" | perl -ne 'if (m|<latest>([^<]+)</latest>|) { print $1; exit 0 }')"
  fi
  if [ -n "$version" ]; then
    echo "$version"
    return 0
  fi
  return 1
}

RAW_VERSION="${1:-}"
VERSION=""
TAG_VERSION=""
if [ -n "$RAW_VERSION" ]; then
  TAG_VERSION="$(extract_version "$RAW_VERSION")"
  if [ -z "$TAG_VERSION" ]; then
    echo "Invalid Codename One version/tag: $RAW_VERSION" >&2
    exit 1
  fi
  VERSION="$TAG_VERSION"
fi

MAVEN_VERSION="$(latest_plugin_version_from_maven || true)"
if [ -n "$MAVEN_VERSION" ]; then
  if [ -n "$TAG_VERSION" ] && [ "$TAG_VERSION" != "$MAVEN_VERSION" ]; then
    echo "Tag version ($TAG_VERSION) differs from Maven Central release ($MAVEN_VERSION). Using Maven Central release." >&2
  fi
  VERSION="$MAVEN_VERSION"
fi

if [ -z "$VERSION" ]; then
  echo "Usage: $0 [<cn1-version-or-tag>]" >&2
  echo "Unable to determine Codename One plugin version from argument or Maven Central metadata." >&2
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
  replace_file "$pom" "s|<cn1\\.version>[^<]+</cn1\\.version>|<cn1.version>$VERSION</cn1.version>|g;"
done < <(find "$ROOT_INITIALIZR_DIR" -name pom.xml -type f)

replace_file "$GENERATOR_MODEL" "s|private static final String CN1_PLUGIN_VERSION = \\\"[^\\\"]+\\\";|private static final String CN1_PLUGIN_VERSION = \\\"$VERSION\\\";|g;"
replace_file "$MATRIX_TEST" "s|<cn1\\.plugin\\.version>[^<]+</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"
replace_file "$MATRIX_TEST" "s|<cn1\\.version>[^<]+</cn1\\.version>|<cn1.version>$VERSION</cn1.version>|g;"


echo "Updated Initializr Codename One versions to $VERSION"
