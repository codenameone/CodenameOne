#!/usr/bin/env bash
# Build the Codename One native macOS (AppKit) port.
#
# macOS-only for the obvious reason, and it needs Xcode for the same reason the
# iOS port does: the port ships Objective-C that has to compile.
set -euo pipefail
if [[ "$(uname)" != "Darwin" ]]; then
  echo "The macOS port can only be built on macOS with Xcode installed." >&2
  exit 1
fi
if ! command -v xcodebuild >/dev/null; then
  echo "Xcode command-line tools not found." >&2
  exit 1
fi

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

# maven/mac/pom.xml stages the shared Apple sources it uses -- filtered by
# Ports/MacPort/shared-natives.exclude and shared-java.exclude -- and pulls
# Themes/iOSModernTheme.res into nativemac.jar, so nothing needs pre-staging
# under Ports/MacPort/. For local iteration on native-themes/, run
# scripts/build-native-themes.sh.

# Reinstall the CSS compiler first so changes under maven/css-compiler/ and
# maven/css-cli/ reach the app build that follows this script. The maven
# plugin's css goal forks com.codename1.designer.css.CN1CSSCLI on a classpath
# it resolves from the local repository (codenameone-css-cli, which brings
# codenameone-css-compiler and codenameone-project-model), so a cached
# ~/.m2/repository would otherwise hand it the previous build's jars even when
# CSSTheme.java has changed, and the new parsing would silently miss the
# app's theme.res. The designer module has no part in this -- it is the
# resource editor, and no build runs it -- so it is not built here. Nothing
# else in this slice is needed later: `-pl mac -am` below rebuilds core
# itself, and the plugin and the javase port come from setup-workspace.sh.
# Skip javadoc/source jars: they aren't needed to install the macOS port, and the
# framework is compiled with JDK 8 whose javadoc rejects the JDK 9+ options
# (--add-stylesheet / --add-script) configured in maven/pom.xml. Mirrors the flags
# setup-workspace.sh already passes for the main framework install.
"$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml -pl css-cli -am -DskipTests -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Djava.awt.headless=true install
"$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml -pl mac -am -Dmaven.javadoc.skip=true -Dmaven.source.skip=true -Djava.awt.headless=true clean install "$@"
