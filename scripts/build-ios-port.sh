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

if [ -f "$DIR/tools/env.sh" ]; then
  source "$DIR/tools/env.sh"
fi

if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | head -n1 | grep -q "11"; then
  echo "Provisioning workspace..."
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$DIR/tools/env.sh"
fi

if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | head -n1 | grep -q "11"; then
  echo "Failed to set up JDK 11." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

echo "JAVA_HOME is set to $JAVA_HOME"
if [ -x "$JAVA_HOME/bin/java" ]; then
  "$JAVA_HOME/bin/java" -version
else
  echo "java executable not found under $JAVA_HOME" >&2
fi
echo "PATH is $PATH"
mvn -version

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
