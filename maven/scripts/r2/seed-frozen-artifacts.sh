#!/usr/bin/env bash
#
# Copies the frozen Codename One artifacts from Maven Central into R2, once.
#
# Some artifacts stopped being published because their content does not change per
# release (see <excludeArtifacts> in maven/pom.xml). Consumers are pinned to the last
# version that *was* published, and that version lives only on Maven Central -- so
# after the cutover an R2 consumer could not resolve it. That matters most in exactly
# the situation this migration exists to survive: Central throttled or unavailable.
#
# codenameone-javase declares com.codenameone:sqlite-jdbc:${cn1.sqlite.jdbc.version},
# and codenameone-javase is itself a runtime dependency of codenameone-maven-plugin,
# so ordinary plugin resolution reaches it. It is not history that can be left behind;
# it is a live dependency of every future release.
#
# Run this once before flipping CN1_DUAL_PUBLISH to false. It is idempotent.
#
# Usage: seed-frozen-artifacts.sh [<artifactId>:<version> ...]
#        defaults to the set pinned in maven/pom.xml and AbstractCN1Mojo
#
set -euo pipefail

: "${R2_ACCOUNT_ID:?R2_ACCOUNT_ID is required}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID is required}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY is required}"
: "${R2_BUCKET:?R2_BUCKET is required}"

# Versions are read from where they are actually declared rather than repeated here.
# Duplicating them would mean a bump could seed the wrong artifact -- the pin would move
# and the seeding would not, silently, with no build failure to catch it.
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

read_pinned() {
    local description="$1" file="$2" pattern="$3"
    local value
    # `|` as the delimiter, not `/`: the XML pattern contains a closing tag.
    value=$(sed -nE "s|.*${pattern}.*|\1|p" "$file" | head -n1)
    if [ -z "$value" ]; then
        echo "ERROR: could not read the ${description} from ${file}." >&2
        echo "It has moved or been renamed; update this script rather than hardcoding it." >&2
        exit 1
    fi
    printf '%s' "$value"
}

SQLITE_VERSION=$(read_pinned "pinned sqlite-jdbc version" \
    "${REPO_ROOT}/maven/pom.xml" \
    "<cn1\\.sqlite\\.jdbc\\.version>([^<]+)</cn1\\.sqlite\\.jdbc\\.version>")

# The designer pin lives only on the mojo parameter, deliberately: see the comment in
# maven/pom.xml explaining why it is not also a Maven property.
DESIGNER_VERSION=$(read_pinned "pinned designer version" \
    "${REPO_ROOT}/maven/codenameone-maven-plugin/src/main/java/com/codename1/maven/AbstractCN1Mojo.java" \
    "cn1\\.designer\\.version\", defaultValue = \"([^\"]+)\"")

DEFAULT_COORDINATES=(
    "sqlite-jdbc:${SQLITE_VERSION}"
    "codenameone-designer:${DESIGNER_VERSION}"
    "codenameone-javase-svg:${DESIGNER_VERSION}"
)

coordinates=("$@")
if [ "${#coordinates[@]}" -eq 0 ]; then
    coordinates=("${DEFAULT_COORDINATES[@]}")
fi

export AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION=auto
export AWS_REQUEST_CHECKSUM_CALCULATION=when_required
export AWS_RESPONSE_CHECKSUM_VALIDATION=when_required

ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
CENTRAL="https://repo1.maven.org/maven2/com/codenameone"
IMMUTABLE='public, max-age=31536000, immutable'

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
seeded=0

for coordinate in "${coordinates[@]}"; do
    artifact="${coordinate%%:*}"
    version="${coordinate##*:}"
    echo "==> ${artifact}:${version}"

    listing=$(curl -fsS "${CENTRAL}/${artifact}/${version}/" 2>/dev/null) || {
        echo "ERROR: ${artifact}:${version} is not on Maven Central, so it cannot be seeded." >&2
        exit 1
    }
    files=$(echo "$listing" | grep -oE 'href="[^"]+"' | sed 's/href="//;s/"//' \
            | grep -vE '^\.\.|/$|^\?')
    if [ -z "$files" ]; then
        echo "ERROR: no files listed for ${artifact}:${version}." >&2
        exit 1
    fi

    dir="${work}/${artifact}/${version}"
    mkdir -p "$dir"
    for file in $files; do
        curl -fsS -o "${dir}/${file}" "${CENTRAL}/${artifact}/${version}/${file}"
    done

    # Verify what was downloaded before publishing it: a truncated copy would be worse
    # than none, because the immutability guard would then refuse to repair it.
    while IFS= read -r sha1file; do
        subject="${sha1file%.sha1}"
        [ -f "$subject" ] || continue
        expected=$(tr -d '[:space:]' < "$sha1file")
        actual=$(shasum "$subject" | cut -d' ' -f1)
        if [ "$expected" != "$actual" ]; then
            echo "ERROR: checksum mismatch for $(basename "$subject") from Central." >&2
            exit 1
        fi
    done < <(find "$dir" -name '*.sha1')

    aws s3 cp "$dir" "s3://${R2_BUCKET}/maven2/com/codenameone/${artifact}/${version}" \
        --recursive --endpoint-url "$ENDPOINT" \
        --cache-control "$IMMUTABLE" --only-show-errors
    n=$(find "$dir" -type f | wc -l | tr -d ' ')
    seeded=$((seeded + n))
    echo "    seeded $n files"
done

# Deliberately no release-completion marker: these are pinned dependencies resolved by
# exact path, not a release. regen-maven-metadata.py knows them as frozen and generates
# their metadata from what is present rather than requiring a marker.
echo "==> done, $seeded file(s) seeded into s3://${R2_BUCKET}/maven2"
