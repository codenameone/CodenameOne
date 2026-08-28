#!/usr/bin/env bash
#
# Copies the frozen Codename One artifacts from Maven Central into R2, once.
#
# Some artifacts stopped being published because their content does not change per
# release (see <excludeArtifacts> in maven/pom.xml). Consumers are pinned to the last
# version that *was* published, and that version lives only on Maven Central -- so
# an R2 consumer cannot resolve it. That matters most in exactly the situation this
# migration exists to survive: Central throttled or unavailable.
#
# codenameone-maven-plugin resolves codenameone-designer at cn1.designer.version for
# the Resource Editor goals, so ordinary plugin resolution reaches it. It is not
# history that can be left behind; it is a live dependency of every future release.
#
# Now that releases no longer go to Central, this is a prerequisite rather than a
# nice-to-have: check-frozen-artifacts.sh fails the release while any of it is absent.
# It is idempotent, so re-running after a pin bump seeds only what is new.
#
# Every file the coordinate has on Central is copied, not a chosen subset: the
# designer is consumed as its jar-with-dependencies attachment, and guessing which
# attachments matter is how the pom gets seeded and the artifact does not.
#
# Usage: seed-frozen-artifacts.sh [<artifactId>:<version> ...]
#        defaults to the coordinates derived by frozen-coordinates.py
#
set -euo pipefail

: "${R2_ACCOUNT_ID:?R2_ACCOUNT_ID is required}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID is required}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY is required}"
: "${R2_BUCKET:?R2_BUCKET is required}"

# Coordinates are derived by frozen-coordinates.py so that this script and the
# release-time check cannot disagree about which version is pinned, and so that a
# newly pinned artifact is picked up without anyone remembering to add it here.
COORDINATES_SCRIPT="$(dirname "${BASH_SOURCE[0]}")/frozen-coordinates.py"

coordinates=("$@")
if [ "${#coordinates[@]}" -eq 0 ]; then
    # Captured before the split, deliberately. `mapfile < <(...)` would report the
    # exit status of mapfile rather than of the generator, so an unreadable pin would
    # seed nothing and still exit 0 -- and seeding the wrong set is worse than not
    # seeding at all, because the immutability guard then refuses to repair it.
    if ! frozen_list=$(python3 "$COORDINATES_SCRIPT"); then
        exit 1
    fi
    while IFS= read -r line; do
        [ -n "$line" ] && coordinates+=("$line")
    done <<< "$frozen_list"
    if [ "${#coordinates[@]}" -eq 0 ]; then
        echo "ERROR: no frozen coordinates resolved." >&2
        exit 1
    fi
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
    # `artifact:version[:classifier,...]`. Cut rather than ${x##*:}, which would
    # read the classifier field as the version. Classifiers matter only to the
    # check script: this one copies every file the coordinate has on Central.
    artifact=$(echo "$coordinate" | cut -d: -f1)
    version=$(echo "$coordinate" | cut -d: -f2)
    if [ -z "$artifact" ] || [ -z "$version" ]; then
        echo "ERROR: malformed coordinate '${coordinate}'." >&2
        exit 1
    fi
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
