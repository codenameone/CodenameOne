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
# The artifacts a release resolves but no longer publishes, as `<artifactId>:<version>`.
#
# Sourced by seed-frozen-artifacts.sh (which copies them into R2) and by
# check-frozen-artifacts.sh (which refuses to release while one of them is absent).
# Both need the same answer, and a copy in each would let a pin bump be seeded
# correctly and checked against the old version, or the reverse.
#
# The version is read from the single place a user project resolves it from -- the
# cn1.designer.version @Parameter default -- rather than repeated here, so bumping the
# pin cannot leave the seeding or the check behind on the previous version. Reading it
# is a hard failure, never a default: a silent fallback would seed one version and
# release against another.
#
# sqlite-jdbc is deliberately NOT here. It reads like a pin in
# codenameone-javase's pom, but its version comes from the parent's
# dependencyManagement, which tracks ${project.version} -- it is an ordinary reactor
# module published on every tag, and the release's own R2 upload covers it.

cn1_frozen_repo_root() {
    cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd
}

# read_pinned <description> <file> <sed-pattern-with-one-capture-group>
cn1_read_pinned() {
    local description="$1" file="$2" pattern="$3"
    local value
    # `|` as the delimiter, not `/`: the patterns contain paths and closing tags.
    value=$(sed -nE "s|.*${pattern}.*|\1|p" "$file" | head -n1)
    if [ -z "$value" ]; then
        echo "ERROR: could not read the ${description} from ${file}." >&2
        echo "It has moved or been renamed; update frozen-coordinates.sh rather than" >&2
        echo "hardcoding a version, or the seeding and the release check will disagree." >&2
        return 1
    fi
    printf '%s' "$value"
}

# Prints one `<artifactId>:<version>` per line.
cn1_frozen_coordinates() {
    local repo_root designer_version
    repo_root="$(cn1_frozen_repo_root)"
    designer_version=$(cn1_read_pinned "pinned designer version" \
        "${repo_root}/maven/codenameone-maven-plugin/src/main/java/com/codename1/maven/AbstractCN1Mojo.java" \
        "cn1\\.designer\\.version\", defaultValue = \"([^\"]+)\"") || return 1

    # Deprecated Resource Editor, and the Batik SVG support that exists only to serve
    # it. Both are <excludeArtifact> in maven/pom.xml, so no tag publishes them.
    echo "codenameone-designer:${designer_version}"
    echo "codenameone-javase-svg:${designer_version}"
}
