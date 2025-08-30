#!/bin/bash
# Build Codename One iOS port
# Requires macOS with Xcode installed
set -e

if [[ "$(uname)" != "Darwin" ]]; then
  echo "The iOS port can only be built on macOS." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null; then
  echo "Xcode command-line tools not found." >&2
  exit 1
fi

DIR="$(cd "$(dirname "$0")" && pwd)/.."
cd "$DIR"

# Locate JDK 11
if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=$(ls -d "$DIR"/tools/jdk-11* 2>/dev/null | head -n1)
fi
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | head -n1 | grep -q "11"; then
  echo "JAVA_HOME must point to JDK 11." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
if [ ! -f "$BUILD_CLIENT" ]; then
  if ! mvn -f maven/pom.xml cn1:install-codenameone "$@"; then
    if [ -f maven/CodeNameOneBuildClient.jar ]; then
      mkdir -p "$(dirname "$BUILD_CLIENT")"
      cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT"
    else
      echo "maven/CodeNameOneBuildClient.jar not found." >&2
    fi
  fi
fi

mvn -f maven/pom.xml -pl ios -am clean install "$@"
