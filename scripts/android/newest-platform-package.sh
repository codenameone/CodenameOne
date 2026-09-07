#!/usr/bin/env bash
#
# Print the newest stable sdkmanager package for an Android API level.
#
#   scripts/android/newest-platform-package.sh 37   ->  platforms;android-37.2
#   scripts/android/newest-platform-package.sh 36   ->  platforms;android-36
#
# Needed because an API level is not one platform. From API 37 there is no
# unsuffixed package at all -- sdkmanager offers android-37.0, android-37.1 and
# android-37.2 -- and a removal can land in any of those revisions, so pinning
# the ".0" of a level tests the oldest one and merges a regression introduced
# in a later revision. Asking sdkmanager which revisions exist keeps that from
# going stale, at the cost of a check that can turn red when Google ships a new
# minor: which is the point, since that is exactly when we want to know.
#
# Previews are excluded. Their version carries a suffix (37.2-beta3), they are
# not what a developer's SDK will settle on, and a beta that removes something
# is not yet a fact about a shipped release.

set -euo pipefail

API="${1:-}"
if [ -z "$API" ]; then
  echo "usage: $0 <api-level>" >&2
  exit 2
fi

# SDK-root copy first; the one on PATH can belong to a different SDK root.
SDKMANAGER=""
if [ -x "${ANDROID_SDK_ROOT:-}/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
elif [ -x "${ANDROID_HOME:-}/cmdline-tools/latest/bin/sdkmanager" ]; then
  SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
elif command -v sdkmanager >/dev/null 2>&1; then
  SDKMANAGER="sdkmanager"
fi

# No fallback, deliberately. The obvious one -- assume the ".0" -- is the
# single answer this script must never give: ".0" is the oldest revision of a
# level, it is usually the one already on a runner, and returning it would let
# the jar assertions and both API checks pass while testing exactly the
# revision this script exists to stop them pinning. A resolver that quietly
# degrades to the thing it was written to prevent is worse than no resolver,
# because the coverage looks present. Anything that stops it answering is
# therefore fatal, and the caller's `set -e` turns that into a failed step.
if [ -z "$SDKMANAGER" ]; then
  echo "$0: no sdkmanager on PATH or under ANDROID_SDK_ROOT/ANDROID_HOME," \
       "so the newest API $API platform cannot be determined" >&2
  exit 1
fi

# JDK 17+, or sdkmanager refuses to run at all; a script that sourced the
# workspace env has JAVA_HOME pointing at the JDK 8 the framework is built with.
if ! LIST=$(JAVA_HOME="${JDK_HOME:-${JAVA17_HOME:-${JAVA_HOME:-}}}" "$SDKMANAGER" --list 2>&1); then
  echo "$0: sdkmanager --list failed, so the newest API $API platform cannot" \
       "be determined:" >&2
  echo "$LIST" | tail -5 >&2
  exit 1
fi
if [ -z "$LIST" ]; then
  echo "$0: sdkmanager --list printed nothing, so the newest API $API" \
       "platform cannot be determined" >&2
  exit 1
fi

# `|| true` because a grep that matches nothing exits 1, and under `set -o
# pipefail` that killed the script here -- before the explicit "offers no
# platform" check below could say so. The failure was silent: exit 1, no
# message, which is the shape of bug this whole script exists to refuse.
#
# The trailing character class is what keeps neighbours out: it rejects the
# "-" of android-36-ext18 and of the 37.2-beta1 previews, while accepting the
# space or end-of-line that follows a real package id.
MATCHES=$(echo "$LIST" \
  | grep -oE "platforms;android-${API}(\.[0-9]+)?([^0-9a-zA-Z.-]|$)" \
  | grep -oE "platforms;android-${API}(\.[0-9]+)?" \
  | sort -u || true)

if [ -z "$MATCHES" ]; then
  echo "$0: sdkmanager offers no platform for API $API" >&2
  exit 1
fi

# Pick the newest revision and print THAT LINE, rather than reconstructing an
# id from a stripped suffix. Stripping lost the unsuffixed package: for
# "platforms;android-36" the suffix is the empty string, which is
# indistinguishable from "nothing matched" -- so a level offered only without a
# minor, which is every level up to 36, was reported as unavailable.
#
# An unsuffixed package is the level's original release and older than any
# explicit minor, so it sorts as -1 and any real minor beats it.
BEST_PACKAGE=""
BEST_MINOR=-2
while IFS= read -r PKG; do
  [ -n "$PKG" ] || continue
  SUFFIX="${PKG#platforms;android-${API}}"
  if [ -z "$SUFFIX" ]; then
    MINOR=-1
  else
    MINOR="${SUFFIX#.}"
  fi
  if [ "$MINOR" -gt "$BEST_MINOR" ]; then
    BEST_MINOR="$MINOR"
    BEST_PACKAGE="$PKG"
  fi
done <<MATCHED_PACKAGES
$MATCHES
MATCHED_PACKAGES

if [ -z "$BEST_PACKAGE" ]; then
  echo "$0: could not pick a platform for API $API from:" >&2
  echo "$MATCHES" >&2
  exit 1
fi

echo "$BEST_PACKAGE"
