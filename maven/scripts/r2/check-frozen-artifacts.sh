#!/usr/bin/env bash
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.
#
# Fails while any artifact a release resolves but does not publish is missing from R2.
#
# This exists because the release stopped going to Maven Central. Up to the cutover a
# missing frozen artifact was invisible: every consumer still had Central in its
# resolution path, so codenameone-designer resolved from there no matter what R2 held.
# With R2 as the only repository a generated project declares, the same absence means
# `cn1:design` cannot resolve at all -- and the release that shipped it is already
# immutable by the time anyone runs the goal.
#
# So it is checked before the release builds, not after: the cheapest moment to find
# out is the one where nothing has been published yet.
#
# Usage: check-frozen-artifacts.sh [<artifactId>:<version> ...]
#        defaults to the coordinates in frozen-coordinates.sh
#
# Reads only the public repository, so it needs no R2 credentials.
#
set -euo pipefail

BASE_URL="${R2_BASE_URL:-https://repo.codenameone.com/maven2}"

# shellcheck source=maven/scripts/r2/frozen-coordinates.sh
source "$(dirname "${BASH_SOURCE[0]}")/frozen-coordinates.sh"

coordinates=("$@")
if [ "${#coordinates[@]}" -eq 0 ]; then
    if ! frozen_list=$(cn1_frozen_coordinates); then
        exit 1
    fi
    while IFS= read -r line; do
        [ -n "$line" ] && coordinates+=("$line")
    done <<< "$frozen_list"
fi

if [ "${#coordinates[@]}" -eq 0 ]; then
    echo "ERROR: no frozen coordinates resolved." >&2
    exit 1
fi

# Cache buster for the same reason the release polls carry one: Cloudflare caches 404s
# on the custom domain, so a probe run just after a seed could otherwise keep seeing a
# stale negative and block a release whose prerequisite is actually in place.
cb="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-0}-$$"

status=0
for coordinate in "${coordinates[@]}"; do
    artifact="${coordinate%%:*}"
    version="${coordinate##*:}"
    # The jar as well as the pom: a pom-only copy resolves during dependency
    # collection and then fails at the point of actually using the artifact, which is
    # the harder failure to read.
    for extension in pom jar; do
        url="${BASE_URL}/com/codenameone/${artifact}/${version}/${artifact}-${version}.${extension}?cb=${cb}"
        code=$(curl -s -o /dev/null -w "%{http_code}" "$url")
        if [ "$code" = "200" ]; then
            echo "ok: ${artifact}:${version} (${extension})"
        else
            echo "MISSING on R2: ${artifact}:${version} (${extension}, HTTP ${code})" >&2
            status=1
        fi
    done
done

if [ "$status" != "0" ]; then
    cat >&2 <<'MSG'

A release resolves these artifacts but no longer publishes them, and they are not in
the repository generated projects use. Seed them, then re-run this release:

    Actions -> "Seed frozen artifacts to R2" -> Run workflow

or locally, with the R2 credentials:

    bash maven/scripts/r2/seed-frozen-artifacts.sh
MSG
fi
exit $status
