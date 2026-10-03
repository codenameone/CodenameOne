#!/usr/bin/env bash
# Prints the archetype javaVersion that suits the JDK running this build.
#
# The archetype defaults javaVersion to 17 (Flutter support needs it, and it
# matches start.codenameone.com), while several CI legs deliberately run the
# test suites on JDK 8 so the framework is built the way it has to be. A suite
# that generates a project and then builds it has to ask for a version that the
# JDK in its hands can actually produce, or it fails with "invalid target
# release: 17" -- a failure about the generated project that reads like a
# broken archetype.
#
# Shared by tests/env.sh and maven/integration-tests/inc/env.sh so the two
# suites cannot drift apart on it. Override with CN1_ARCHETYPE_JAVA_VERSION.
set -u

if [ -n "${CN1_ARCHETYPE_JAVA_VERSION:-}" ]; then
  echo "$CN1_ARCHETYPE_JAVA_VERSION"
  exit 0
fi

java_bin="java"
[ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && java_bin="$JAVA_HOME/bin/java"

# "1.8.0_452" -> 8, "17.0.20" -> 17. The 1.x form is the only reason this is
# not a single field split.
version_line="$("$java_bin" -version 2>&1 | head -1)"
major="$(echo "$version_line" | sed -E 's/[^"]*"([0-9]+)\.?([0-9]+)?.*/\1 \2/')"
set -- $major
if [ "${1:-}" = "1" ]; then
  major="${2:-8}"
else
  major="${1:-}"
fi

if [ -n "$major" ] && [ "$major" -ge 17 ] 2>/dev/null; then
  echo 17
else
  echo 8
fi
