#!/usr/bin/env bash
#
# Copies the files Maven itself is bootstrapped from into R2, at the same path they
# have on Maven Central, so CI never needs Central to start Maven.
#
# What it copies is derived from the tree, never listed by hand:
#   - every distributionUrl= and wrapperUrl= in a tracked maven-wrapper.properties
#     (the Maven distributions the wrappers unpack, and the takari wrapper jar the
#     older 0.5.6 scripts download first), and
#   - every apache-maven archive named in scripts/setup-workspace.sh.
# Only the path below /maven2/ matters; the host may already be repo.codenameone.com.
#
# Each file is downloaded from Google's copy of Central (Central throttles), checked
# against its .sha1 from the same place, and uploaded with that .sha1 beside it. A
# file already served from repo.codenameone.com is skipped, so re-running after a
# wrapper version bump seeds only the new one. scripts/check-maven-central-mirror.py
# holds the CI wrappers to repo.codenameone.com, so: bump a wrapper, run the
# "Seed Maven distributions to R2" workflow, then merge.
#
# Usage: seed-maven-distributions.sh           # upload what is missing
#        seed-maven-distributions.sh --list    # print the derived paths, no credentials needed
#        seed-maven-distributions.sh --verify  # fail unless every path is served from R2
#
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd -P)"
SOURCE="https://maven-central.storage-download.googleapis.com/maven2"
R2_PUBLIC="https://repo.codenameone.com/maven2"
IMMUTABLE='public, max-age=31536000, immutable'

derive_paths() {
    {
        git -C "$ROOT" ls-files '*maven-wrapper.properties' | while read -r f; do
            grep -E '^(distributionUrl|wrapperUrl)=' "$ROOT/$f" | cut -d= -f2-
        done
        grep -oE 'https://[^ "]*/maven2/org/apache/maven/apache-maven/[^ "]*' "$ROOT/scripts/setup-workspace.sh"
    } | sed -E 's#^.*/maven2/##' | tr -d '\r' | sort -u
}

paths=$(derive_paths)
if [ -z "$paths" ]; then
    echo "ERROR: no Maven bootstrap files derived from the tree." >&2
    exit 1
fi
# Paths become bucket keys and curl arguments; refuse anything that is not a plain
# Maven repository path.
while read -r p; do
    case "$p" in
        *[!A-Za-z0-9._/-]*|*..*|/*) echo "ERROR: refusing path '$p'." >&2; exit 1 ;;
    esac
done <<< "$paths"

# Retried: Check workflows runs --verify on every PR, and one network blip must not
# turn it red.
served() { curl -fsSIL --retry 3 --retry-all-errors -o /dev/null "$R2_PUBLIC/$1" 2>/dev/null; }

case "${1:-}" in
    --list) echo "$paths"; exit 0 ;;
    --verify)
        status=0
        while read -r p; do
            if served "$p"; then echo "ok: $p"; else echo "MISSING from R2: $p" >&2; status=1; fi
        done <<< "$paths"
        exit "$status" ;;
    "") ;;
    *) echo "usage: $0 [--list|--verify]" >&2; exit 2 ;;
esac

: "${R2_ACCOUNT_ID:?R2_ACCOUNT_ID is required}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID is required}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY is required}"
: "${R2_BUCKET:?R2_BUCKET is required}"
export AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION=auto
export AWS_REQUEST_CHECKSUM_CALCULATION=when_required
export AWS_RESPONSE_CHECKSUM_VALIDATION=when_required
ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"

work=$(mktemp -d)
trap 'rm -rf "${work:?}"' EXIT
seeded=0
while read -r p; do
    if served "$p"; then
        echo "already in R2: $p"
        continue
    fi
    # One directory per file, so nothing needs deleting between iterations.
    dir="$work/$seeded"
    mkdir -p "$dir"
    name=$(basename "$p")
    curl -fsS --retry 3 -o "$dir/$name" "$SOURCE/$p"
    curl -fsS --retry 3 -o "$dir/$name.sha1" "$SOURCE/$p.sha1"
    expected=$(awk '{print $1}' "$dir/$name.sha1" | tr -d '[:space:]')
    actual=$(shasum "$dir/$name" | cut -d' ' -f1)
    if [ "$expected" != "$actual" ]; then
        echo "ERROR: SHA-1 mismatch for $p ($actual, expected $expected)." >&2
        exit 1
    fi
    for f in "$name" "$name.sha1"; do
        aws s3 cp "$dir/$f" "s3://${R2_BUCKET}/maven2/$(dirname "$p")/$f" \
            --endpoint-url "$ENDPOINT" --cache-control "$IMMUTABLE" --only-show-errors
    done
    seeded=$((seeded + 1))
    echo "seeded: $p"
done <<< "$paths"
echo "==> done, $seeded file(s) seeded"
