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

# Validate Xcode version is at least 16.0
XCODE_VERSION=$(xcodebuild -version 2>/dev/null | head -n 1 | awk '{print $2}')
XCODE_MAJOR=$(echo "$XCODE_VERSION" | cut -d. -f1)
if [ "$XCODE_MAJOR" -lt 16 ]; then
  echo "Error: Xcode version $XCODE_VERSION is too old. Minimum required version is 16.0" >&2
  exit 1
fi
echo "Using Xcode version $XCODE_VERSION"

# Normalize TMPDIR and compose paths without duplicate slashes
TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"

# Place downloaded tools outside the repository so it isn't filled with binaries
# Strip any trailing slash again at the join to be extra safe.
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
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
