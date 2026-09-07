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

fallback() {
  # What the level would have been called before minor revisions existed, and
  # the first revision after. Used only when sdkmanager cannot be consulted.
  if [ "$API" -ge 37 ]; then echo "platforms;android-$API.0"; else echo "platforms;android-$API"; fi
}

if [ -z "$SDKMANAGER" ]; then
  fallback
  exit 0
fi

# JDK 17+, or sdkmanager refuses to run at all; a script that sourced the
# workspace env has JAVA_HOME pointing at the JDK 8 the framework is built with.
LIST=$(JAVA_HOME="${JDK_HOME:-${JAVA17_HOME:-${JAVA_HOME:-}}}" "$SDKMANAGER" --list 2>/dev/null || true)
if [ -z "$LIST" ]; then
  fallback
  exit 0
fi

BEST=$(echo "$LIST" \
  | grep -oE "platforms;android-${API}(\.[0-9]+)?([^0-9a-zA-Z.-]|$)" \
  | grep -oE "platforms;android-${API}(\.[0-9]+)?" \
  | sort -u \
  | sed "s/^platforms;android-${API}//" \
  | sed 's/^\.//' \
  | sort -n \
  | tail -1)

if [ -z "$BEST" ]; then
  fallback
elif [ "$BEST" = "$API" ] || [ -z "$BEST" ]; then
  echo "platforms;android-$API"
else
  echo "platforms;android-$API.$BEST"
fi
