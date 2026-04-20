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
  metadata="$(curl -fsSL https://repo.maven.apache.org/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml || true)"
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
if [ -n "$RAW_VERSION" ]; then
  # An explicit tag/argument is authoritative. Maven Central metadata
  # can lag behind a just-pushed release, which used to leave the
  # playground pinned one version behind whenever the workflow fired
  # before Central had indexed the new artifact.
  VERSION="$(extract_version "$RAW_VERSION")"
  if [ -z "$VERSION" ]; then
    echo "Invalid Codename One version/tag: $RAW_VERSION" >&2
    exit 1
  fi
else
  VERSION="$(latest_plugin_version_from_maven || true)"
fi

if [ -z "$VERSION" ]; then
  echo "Usage: $0 [<cn1-version-or-tag>]" >&2
  echo "Unable to determine Codename One plugin version from argument or Maven Central metadata." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
PLAYGROUND_POM="$ROOT_DIR/scripts/cn1playground/pom.xml"

perl -0pi -e "s|(<properties>.*?<cn1\\.version>)[^<]+(</cn1\\.version>)|\${1}$VERSION\${2}|s;" "$PLAYGROUND_POM"
perl -0pi -e 's|(<properties>.*?<cn1\.plugin\.version>)[^<]+(</cn1\.plugin\.version>)|${1}\${cn1.version}${2}|s;' "$PLAYGROUND_POM"

echo "Updated Playground Codename One versions to $VERSION"
