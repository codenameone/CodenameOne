#!/usr/bin/env bash
#
# Uploads a central-publishing staging tree to the Codename One R2 Maven
# repository (https://repo.codenameone.com/maven2).
#
# central-publishing-maven-plugin stages every module's artifacts into
# <topLevelBuildDir>/central-staging in standard Maven layout, with MD5, SHA-1,
# SHA-256 and SHA-512 checksums and .asc signatures already generated, *before*
# it uploads anything to Central. That directory is exactly what R2 needs, and
# it exists even when the Central upload fails -- so this runs independently of
# whether Central accepted the release.
#
# Usage: publish-staging-to-r2.sh <staging-dir> [<staging-dir> ...]
#
# Required environment:
#   R2_ACCOUNT_ID          Cloudflare account id (forms the S3 endpoint)
#   R2_ACCESS_KEY_ID       R2 API token access key
#   R2_SECRET_ACCESS_KEY   R2 API token secret
#   R2_BUCKET              bucket name, e.g. cn1-maven
# Optional:
#   R2_ALLOW_OVERWRITE=1   permit replacing an existing object whose bytes differ
#
set -euo pipefail

: "${R2_ACCOUNT_ID:?R2_ACCOUNT_ID is required}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID is required}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY is required}"
: "${R2_BUCKET:?R2_BUCKET is required}"

if [ "$#" -lt 1 ]; then
    echo "usage: $0 <staging-dir> [<staging-dir> ...]" >&2
    exit 2
fi

export AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION=auto
# R2 rejects the CRC checksums recent aws-cli v2 releases send by default.
export AWS_REQUEST_CHECKSUM_CALCULATION=when_required
export AWS_RESPONSE_CHECKSUM_VALIDATION=when_required

ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
IMMUTABLE='public, max-age=31536000, immutable'

uploaded=0

for dir in "$@"; do
    if [ ! -d "$dir" ]; then
        echo "==> skipping $dir (not a directory)"
        continue
    fi

    # Bookkeeping that must never reach the repository. Releases stage clean,
    # but the snapshot path leaves maven-metadata-central-staging.xml behind,
    # and a local-repo copy would carry _remote.repositories. maven-metadata.xml
    # itself is owned by regen-maven-metadata.py, which derives it from the
    # bucket -- a per-build copy would list only the version just built and
    # would clobber the accumulated history.
    find "$dir" \( -name 'maven-metadata*.xml' \
                -o -name 'maven-metadata*.xml.*' \
                -o -name '_remote.repositories' \
                -o -name 'resolver-status.properties' \
                -o -name '.index' \) -delete

    if [ -z "$(find "$dir" -type f -print -quit)" ]; then
        echo "==> skipping $dir (empty after cleanup)"
        continue
    fi

    echo "==> checking $dir for conflicting objects"
    conflicts=0
    while IFS= read -r file; do
        key="maven2/${file#"$dir"/}"
        remote_etag=$(aws s3api head-object --bucket "$R2_BUCKET" --key "$key" \
            --endpoint-url "$ENDPOINT" --query ETag --output text 2>/dev/null || true)
        [ -z "$remote_etag" ] && continue
        local_md5=$(md5 -q "$file" 2>/dev/null || md5sum "$file" | cut -d' ' -f1)
        if [ "${remote_etag//\"/}" != "$local_md5" ]; then
            echo "    CONFLICT: $key already exists with different content"
            conflicts=$((conflicts + 1))
        fi
    done < <(find "$dir" -type f)

    if [ "$conflicts" -gt 0 ] && [ "${R2_ALLOW_OVERWRITE:-0}" != "1" ]; then
        echo "ERROR: $conflicts object(s) would be overwritten with different bytes." >&2
        echo "Released versions are immutable. Re-cut the release under a new version," >&2
        echo "or set R2_ALLOW_OVERWRITE=1 if you are deliberately replacing them." >&2
        exit 1
    fi

    # `cp --recursive`, never `sync`: sync --delete would erase every previous
    # version, because a staging tree only ever holds the release being built.
    # cp cannot delete, which is why it is used here.
    echo "==> uploading $dir"
    aws s3 cp "$dir" "s3://${R2_BUCKET}/maven2" --recursive \
        --endpoint-url "$ENDPOINT" \
        --cache-control "$IMMUTABLE" \
        --only-show-errors
    n=$(find "$dir" -type f | wc -l | tr -d ' ')
    uploaded=$((uploaded + n))
    echo "    uploaded $n files"
done

echo "==> done, $uploaded file(s) uploaded to s3://${R2_BUCKET}/maven2"
