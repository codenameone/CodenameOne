#!/usr/bin/env bash
# Build Codename One Android port using JDK 8 for Maven and JDK 17 for compilation
set -euo pipefail

log() {
  echo "[build-android-port] $1"
}

# Normalize TMPDIR and compose paths without duplicate slashes
TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"

# Place downloaded tools outside the repository so it isn't filled with binaries
# Strip any trailing slash again at the join to be extra safe.
DOWNLOAD_DIR="${TMPDIR%/}/codenameone-tools"
log "The DOWNLOAD_DIR is ${DOWNLOAD_DIR}"

ENV_DIR="$DOWNLOAD_DIR/tools"
ENV_FILE="$ENV_DIR/env.sh"

log "Loading workspace environment from $ENV_FILE"
if [ -f "$ENV_FILE" ]; then
  log "Workspace environment file metadata"
  ls -l "$ENV_FILE" | while IFS= read -r line; do log "$line"; done
  log "Workspace environment file contents"
  sed 's/^/[build-android-port] ENV: /' "$ENV_FILE"
  # shellcheck disable=SC1090
  source "$ENV_FILE"
else
  log "Workspace tools not found. Running setup-workspace.sh"
  ./scripts/setup-workspace.sh -q -DskipTests
  if [ -f "$ENV_FILE" ]; then
    log "Workspace environment file metadata after setup"
    ls -l "$ENV_FILE" | while IFS= read -r line; do log "$line"; done
    log "Workspace environment file contents after setup"
    sed 's/^/[build-android-port] ENV: /' "$ENV_FILE"
    # shellcheck disable=SC1090
    source "$ENV_FILE"
  else
    log "Failed to create workspace environment at $ENV_FILE" >&2
    exit 1
  fi
fi

log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA_HOME_17=${JAVA_HOME_17:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"

if [ -z "${JAVA_HOME_17:-}" ] || ! "${JAVA_HOME_17:-}/bin/java" -version 2>&1 | grep -q '17\\.0'; then
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
