#!/usr/bin/env bash
#
# Marks a release version fully published in the Codename One R2 Maven repository.
#
# Everything a tag publishes -- the core reactor and the three editors -- shares one
# version, and the plugin resolves its editors at its own version. So the unit of
# completeness is the release, not a directory: advertising core 7.0.x while
# codenameone-gamebuilder 7.0.x is missing gives a consumer a plugin whose
# cn1:gamebuilder goal cannot resolve.
#
# regen-maven-metadata.py refuses to advertise any version without this marker, so
# call this only once every upload for the tag has succeeded. A tag without it leaves
# its artifacts in the bucket but invisible, which a re-run repairs.
#
# Usage: mark-release-complete.sh <version>
#
set -euo pipefail

: "${R2_ACCOUNT_ID:?R2_ACCOUNT_ID is required}"
: "${R2_ACCESS_KEY_ID:?R2_ACCESS_KEY_ID is required}"
: "${R2_SECRET_ACCESS_KEY:?R2_SECRET_ACCESS_KEY is required}"
: "${R2_BUCKET:?R2_BUCKET is required}"

version="${1:?usage: $0 <version>}"

export AWS_ACCESS_KEY_ID="$R2_ACCESS_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$R2_SECRET_ACCESS_KEY"
export AWS_DEFAULT_REGION=auto
export AWS_REQUEST_CHECKSUM_CALCULATION=when_required
export AWS_RESPONSE_CHECKSUM_VALIDATION=when_required

ENDPOINT="https://${R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
# Kept in sync with RELEASES_PSEUDO_ARTIFACT / RELEASE_MARKER in
# regen-maven-metadata.py. It sits under com/codenameone/ so a single listing finds
# it, and its name is not a valid Maven artifactId, so nothing will request it.
KEY="maven2/com/codenameone/_cn1-releases/${version}/complete"

marker=$(mktemp)
printf 'version=%s\nmarked-by=mark-release-complete.sh\n' "$version" > "$marker"
aws s3 cp "$marker" "s3://${R2_BUCKET}/${KEY}" \
    --endpoint-url "$ENDPOINT" \
    --cache-control 'public, max-age=31536000, immutable' \
    --content-type 'text/plain' \
    --only-show-errors
rm -f "$marker"

echo "==> release ${version} marked complete; it may now be advertised"
