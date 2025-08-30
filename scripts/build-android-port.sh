#!/bin/bash
# Build Codename One Android port
set -e

DIR="$(cd "$(dirname "$0")" && pwd)/.."
cd "$DIR"

# Locate JDK 11
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | head -n1 | grep -q "11"; then
  JAVA_HOME=$(ls -d "$DIR"/tools/jdk-11* 2>/dev/null | head -n1)
fi
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | head -n1 | grep -q "11"; then
  echo "JAVA_HOME must point to JDK 11." >&2
  exit 1
fi

# Locate JDK 17
if [ -z "$JAVA_HOME_17" ] || ! "$JAVA_HOME_17/bin/java" -version 2>&1 | head -n1 | grep -q "17"; then
  JAVA_HOME_17=$(ls -d "$DIR"/tools/jdk-17* 2>/dev/null | head -n1)
fi
if [ -z "$JAVA_HOME_17" ] || ! "$JAVA_HOME_17/bin/java" -version 2>&1 | head -n1 | grep -q "17"; then
  echo "JAVA_HOME_17 must point to JDK 17." >&2
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

mvn -f maven/pom.xml -pl android -am clean install "$@"
