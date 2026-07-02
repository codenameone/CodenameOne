#!/usr/bin/env bash
# Install the upstream me.friwi jcef-api jar (matching maven/pom.xml's
# jcef.version) as cn1-binaries/javase/jcef.jar, which is what the legacy Ant
# build of the JavaSE port compiles against. This keeps the Ant build in sync
# with the Maven build, which consumes the me.friwi artifacts directly. The Ant
# build only needs the API jar for compilation (CEF natives are not used there).
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
binaries_dir="$repo_root/../cn1-binaries/javase"

if [ ! -d "$binaries_dir" ]; then
    echo "[install-upstream-jcef] $binaries_dir not present; nothing to do"
    exit 0
fi

ver="$(grep -oE '<jcef\.version>[^<]+' "$repo_root/maven/pom.xml" | head -1 | sed 's/<jcef\.version>//')"
if [ -z "$ver" ]; then
    echo "[install-upstream-jcef] could not read jcef.version from maven/pom.xml" >&2
    exit 1
fi

dest="$binaries_dir/jcef.jar"
m2_jar="$HOME/.m2/repository/me/friwi/jcef-api/$ver/jcef-api-$ver.jar"
tmp="$(mktemp)"

if [ -f "$m2_jar" ]; then
    echo "[install-upstream-jcef] using cached $m2_jar"
    cp "$m2_jar" "$tmp"
else
    enc="$(python3 -c 'import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))' "$ver")"
    url="https://repo1.maven.org/maven2/me/friwi/jcef-api/$enc/jcef-api-$enc.jar"
    echo "[install-upstream-jcef] downloading $url"
    curl -fsSL -o "$tmp" "$url"
fi

# dest may be a symlink into a shared tree (e.g. /opt/cn1-binaries); remove it
# first so we replace it with a standalone file rather than writing in place.
rm -f "$dest"
mv "$tmp" "$dest"
echo "[install-upstream-jcef] installed jcef-api $ver -> $dest"
