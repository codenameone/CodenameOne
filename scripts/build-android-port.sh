#!/bin/bash
# Build Codename One Android port
set -e

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

if [ -z "$JAVA_HOME_17" ] || ! "$JAVA_HOME_17/bin/java" -version 2>&1 | head -n1 | grep -q "17"; then
  echo "Provisioning JDK 17..."
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$DIR/tools/env.sh"
fi

if [ -z "$JAVA_HOME_17" ] || ! "$JAVA_HOME_17/bin/java" -version 2>&1 | head -n1 | grep -q "17"; then
  echo "Failed to set up JDK 17." >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

echo "JAVA_HOME is set to $JAVA_HOME"
"$JAVA_HOME/bin/java" -version
echo "JAVA_HOME_17 is set to $JAVA_HOME_17"
"$JAVA_HOME_17/bin/java" -version
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

mvn -f maven/pom.xml -pl android -am clean install "$@"
