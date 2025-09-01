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
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
if [ -f "$ROOT/tools/env.sh" ]; then
  source "$ROOT/tools/env.sh"
else
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$ROOT/tools/env.sh"
fi
if ! "${JAVA_HOME:-}/bin/java" -version 2>&1 | grep -q '11\.0'; then
  ./scripts/setup-workspace.sh -q -DskipTests
  source "$ROOT/tools/env.sh"
fi
if ! "${JAVA_HOME:-}/bin/java" -version 2>&1 | grep -q '11\.0'; then
  echo "Failed to provision JDK 11" >&2
  exit 1
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
