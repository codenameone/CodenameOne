#!/usr/bin/env bash
# Build Codename One iOS port (macOS only)
set -euo pipefail
if [[ "$(uname)" != "Darwin" ]]; then
  echo "The iOS port can only be built on macOS with Xcode installed." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null; then
  echo "Xcode command-line tools not found." >&2
  exit 1
fi

# Normalize TMPDIR so it has no trailing slash
TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"
DOWNLOAD_DIR="$TMPDIR/codenameone-tools"
ENV_DIR="$DOWNLOAD_DIR/tools"

if [ -f "$ENV_DIR/env.sh" ]; then
  source "$ENV_DIR/env.sh"
else
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$ENV_DIR/env.sh"
fi

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
"$JAVA_HOME/bin/java" -version
"$MAVEN_HOME/bin/mvn" -version

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
if [ ! -f "$BUILD_CLIENT" ]; then
  if ! "$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml cn1:install-codenameone "$@"; then
    [ -f maven/CodeNameOneBuildClient.jar ] && cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT" || true
  fi
fi

"$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml -pl ios -am -Djava.awt.headless=true clean install "$@"
