#!/usr/bin/env bash
#
# Prints, one absolute path per line, the Codename One core sources the backend
# runtime compiles alongside its own: every file under CodenameOne/src whose type
# declaration carries @com.codename1.impl.SharedWithBackend (or @SharedWithBackend
# with an import) on a line of its own.
#
# This is how a class both halves of an application need -- the ORM, the entity
# annotations -- has exactly ONE copy. It lives in CodenameOne/src and every
# backend build reads it from there; nothing is copied into vm/backend. Server-only
# behaviour gets its own class, with its own name, under vm/backend -- see
# src/com/codename1/impl/orm/BackendSqlAccess.java, the server's implementation
# of the shared SqlAccess interface -- never a second copy of a shared one.
#
# Consumers: build.sh, run-javase.sh, ws-conformance.sh, scripts/lib/cn1ss.sh and
# scripts/check-native-signatures.sh. maven/backend/pom.xml applies the SAME line
# match with an Ant selector, so change both together.
#
# It also refuses a backend source that shadows a core source (same package and
# class name), in src/ or either impl/ tree: that is the copy-and-paste this
# replaced, and javac would silently compile whichever copy it met first.
set -euo pipefail

BACKEND="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE="$(cd "$BACKEND/../../CodenameOne/src" && pwd)"

shadowed=0
for tree in src impl/javase impl/parparvm; do
    [ -d "$BACKEND/$tree" ] || continue
    while IFS= read -r rel; do
        if [ -f "$CORE/$rel" ]; then
            echo "shared-sources: vm/backend/$tree/$rel duplicates CodenameOne/src/$rel." >&2
            echo "  Delete the backend copy and mark the core class @com.codename1.impl.SharedWithBackend;" >&2
            echo "  if the server really needs different behaviour, give it a different class name." >&2
            shadowed=1
        fi
    done < <(cd "$BACKEND/$tree" && find . -name '*.java' | sed 's|^\./||')
done
[ "$shadowed" = 0 ] || exit 1

# [[:space:]]* tolerates a CR, so a CRLF source is found as well. The Ant selector
# in maven/backend/pom.xml uses the equivalent \s*.
grep -rlE --include='*.java' '^@(com\.codename1\.impl\.)?SharedWithBackend[[:space:]]*$' "$CORE" | LC_ALL=C sort
