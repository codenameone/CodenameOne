#!/usr/bin/env bash
# Build Codename One Android port using JDK 11 for Maven and JDK 17 for compilation
set -euo pipefail

# Normalize TMPDIR so it has no trailing slash
TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="${TMPDIR:-/tmp}/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"

if [ -f "$ENV_DIR/env.sh" ]; then
  source "$ENV_DIR/env.sh"
else
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$ENV_DIR/env.sh"
fi

if ! "${JAVA_HOME_11:-}/bin/java" -version 2>&1 | grep -q '11\.0'; then
  echo "Failed to provision JDK 11" >&2
  exit 1
fi
if ! "${JAVA_HOME_17:-}/bin/java" -version 2>&1 | grep -q '17\.0'; then
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$ROOT/tools/env.sh"
fi
if ! "${JAVA_HOME_17:-}/bin/java" -version 2>&1 | grep -q '17\.0'; then
  echo "Failed to provision JDK 17" >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
"$JAVA_HOME/bin/java" -version
"$JAVA_HOME_17/bin/java" -version
"$MAVEN_HOME/bin/mvn" -version

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
if [ ! -f "$BUILD_CLIENT" ]; then
  if ! "$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml cn1:install-codenameone "$@"; then
    [ -f maven/CodeNameOneBuildClient.jar ] && cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT" || true
  fi
fi

"$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml -pl android -am -Dmaven.javadoc.skip=true -Djava.awt.headless=true clean install "$@"
