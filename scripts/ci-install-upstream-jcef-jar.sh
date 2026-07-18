#!/usr/bin/env bash
# Install the JCEF Maven Java dependencies used by the legacy Ant JavaSE build.
# The Maven build resolves these transitively from me.friwi:jcefmaven, while the
# Ant project needs explicit jar files in cn1-binaries/javase. Native JCEF
# distributions are intentionally not installed here; JCEF Maven downloads or
# streams the current platform distribution when BrowserComponent is first used.
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
binaries_dir="$repo_root/../cn1-binaries/javase"

if [ ! -d "$binaries_dir" ]; then
    echo "[install-upstream-jcef] $binaries_dir not present; nothing to do"
    exit 0
fi

jcef_ver="$(grep -oE '<jcef\.version>[^<]+' "$repo_root/maven/pom.xml" | head -1 | sed 's/<jcef\.version>//')"
jcefmaven_ver="$(grep -oE '<jcefmaven\.version>[^<]+' "$repo_root/maven/pom.xml" | head -1 | sed 's/<jcefmaven\.version>//')"
if [ -z "$jcef_ver" ] || [ -z "$jcefmaven_ver" ]; then
    echo "[install-upstream-jcef] could not read jcef.version from maven/pom.xml" >&2
    exit 1
fi

install_artifact() {
    local group_path="$1"
    local artifact="$2"
    local version="$3"
    local dest_name="$4"
    local dest="$binaries_dir/$dest_name"
    local m2_jar="$HOME/.m2/repository/$group_path/$artifact/$version/$artifact-$version.jar"
    local tmp="$(mktemp)"
    local enc
    local url

    if [ -f "$m2_jar" ]; then
        echo "[install-upstream-jcef] using cached $m2_jar"
        cp "$m2_jar" "$tmp"
    else
        enc="$(python3 -c 'import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))' "$version")"
        url="https://repo1.maven.org/maven2/$group_path/$artifact/$enc/$artifact-$enc.jar"
        echo "[install-upstream-jcef] downloading $url"
        curl -fsSL -o "$tmp" "$url"
    fi

    # dest may be a symlink into a shared tree (e.g. /opt/cn1-binaries); remove
    # it first so we replace it instead of writing through the symlink.
    rm -f "$dest"
    mv "$tmp" "$dest"
    echo "[install-upstream-jcef] installed $artifact $version -> $dest"
}

install_artifact me/friwi jcef-api "$jcef_ver" jcef.jar
install_artifact me/friwi jcefmaven "$jcefmaven_ver" jcefmaven.jar
install_artifact org/apache/commons commons-compress 1.27.1 commons-compress.jar
install_artifact com/google/code/gson gson 2.11.0 gson.jar
install_artifact commons-codec commons-codec 1.17.1 commons-codec.jar
install_artifact commons-io commons-io 2.16.1 commons-io.jar
install_artifact org/apache/commons commons-lang3 3.16.0 commons-lang3.jar
