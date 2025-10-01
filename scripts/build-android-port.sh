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

load_environment() {
  if [ ! -f "$ENV_FILE" ]; then
    return 1
  fi

  log "Workspace environment file metadata"
  ls -l "$ENV_FILE" | while IFS= read -r line; do log "$line"; done
  log "Workspace environment file contents"
  sed 's/^/[build-android-port] ENV: /' "$ENV_FILE"
  # shellcheck disable=SC1090
  source "$ENV_FILE"
}

check_java_home() {
  local name="$1" home="$2" version_pattern="$3"

  if [ -z "$home" ]; then
    log "$name is not set"
    return 1
  fi

  if [ ! -x "$home/bin/java" ]; then
    log "$name does not contain a java binary: $home/bin/java"
    return 1
  fi

  local version_output
  version_output="$("$home/bin/java" -version 2>&1 || true)"
  if ! grep -q "$version_pattern" <<<"$version_output"; then
    log "$name java -version output did not match $version_pattern"
    log "$version_output"
    return 1
  fi

  log "$name detected at $home (${version_output%%$'\n'*})"
  return 0
}

validate_workspace() {
  local missing=0

  if ! check_java_home "JAVA_HOME" "${JAVA_HOME:-}" '1\.8'; then
    missing=1
  fi

  if ! check_java_home "JAVA17_HOME" "${JAVA17_HOME:-}" '17\.'; then
    missing=1
  fi

  if [ -z "${MAVEN_HOME:-}" ]; then
    log "MAVEN_HOME is not set"
    missing=1
  elif [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
    log "Maven binary missing at $MAVEN_HOME/bin/mvn"
    missing=1
  else
    log "Maven detected at $MAVEN_HOME"
  fi

  return $missing
}

log "Loading workspace environment from $ENV_FILE"
if ! load_environment; then
  log "Workspace tools not found. Running setup-workspace.sh"
  ./scripts/setup-workspace.sh -q -DskipTests "$@"
  if ! load_environment; then
    log "Failed to create workspace environment at $ENV_FILE" >&2
    exit 1
  fi
fi

if validate_workspace; then
  log "Workspace environment validated"
else
  log "Workspace validation failed. Re-provisioning tools via setup-workspace.sh"
  ./scripts/setup-workspace.sh -q -DskipTests "$@"
  if ! load_environment; then
    log "Failed to create workspace environment at $ENV_FILE" >&2
    exit 1
  fi
  if ! validate_workspace; then
    echo "Failed to provision required JDK or Maven binaries" >&2
    exit 1
  fi
  log "Workspace environment validated"
fi

log "Loaded environment: JAVA_HOME=${JAVA_HOME:-<unset>} JAVA17_HOME=${JAVA17_HOME:-<unset>} MAVEN_HOME=${MAVEN_HOME:-<unset>}"

export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
"$JAVA_HOME/bin/java" -version
"$JAVA17_HOME/bin/java" -version
"$MAVEN_HOME/bin/mvn" -version

run_maven() {
  xvfb-run -a "$MAVEN_HOME/bin/mvn" "$@"
}

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
if [ ! -f "$BUILD_CLIENT" ]; then
  if ! run_maven -q -f maven/pom.xml cn1:install-codenameone "$@"; then
    [ -f maven/CodeNameOneBuildClient.jar ] && cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT" || true
  fi
fi

run_maven -q -f maven/pom.xml -pl android -am -Dmaven.javadoc.skip=true -Djava.awt.headless=true clean install "$@"
