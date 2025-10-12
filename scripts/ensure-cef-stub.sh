#!/usr/bin/env bash
# Ensure the codenameone-cef artifact is available in the local Maven repository on
# platforms where the published binaries are unavailable (e.g., Linux ARM).
set -euo pipefail
[ "${DEBUG:-0}" = "1" ] && set -x

log() {
  echo "[ensure-cef-stub] $1"
}

os_name="$(uname -s)"
arch_name="$(uname -m)"
if [ "$os_name" != "Linux" ] || { [ "$arch_name" != "arm64" ] && [ "$arch_name" != "aarch64" ]; }; then
  log "Host ${os_name}-${arch_name} does not require a CEF stub; skipping"
  exit 0
fi

MAVEN_BIN="${MAVEN_BIN:-}"
if [ -z "$MAVEN_BIN" ] && [ -n "${MAVEN_HOME:-}" ] && [ -x "$MAVEN_HOME/bin/mvn" ]; then
  MAVEN_BIN="$MAVEN_HOME/bin/mvn"
fi
if [ -z "$MAVEN_BIN" ] && command -v mvn >/dev/null 2>&1; then
  MAVEN_BIN="$(command -v mvn)"
fi
if [ -z "$MAVEN_BIN" ] || [ ! -x "$MAVEN_BIN" ]; then
  log "Maven executable not found. Set MAVEN_HOME, MAVEN_BIN, or ensure mvn is on PATH." >&2
  exit 1
fi

LOCAL_MAVEN_REPO="${LOCAL_MAVEN_REPO:-$HOME/.m2/repository}"
LOCAL_MAVEN_REPO="${LOCAL_MAVEN_REPO%/}"
if [ -z "$LOCAL_MAVEN_REPO" ]; then
  log "LOCAL_MAVEN_REPO is empty" >&2
  exit 1
fi
mkdir -p "$LOCAL_MAVEN_REPO"

CEF_VERSION="84.4.1-M3"
CEF_COORD="com.codenameone:codenameone-cef:$CEF_VERSION"
CEF_REPO_PATH="$LOCAL_MAVEN_REPO/com/codenameone/codenameone-cef/$CEF_VERSION"
CEF_POM="$CEF_REPO_PATH/codenameone-cef-$CEF_VERSION.pom"

if [ -s "$CEF_POM" ]; then
  log "codenameone-cef $CEF_VERSION already present in $LOCAL_MAVEN_REPO"
  exit 0
fi

log "Installing stub codenameone-cef $CEF_VERSION artifact into $LOCAL_MAVEN_REPO"
mkdir -p "$CEF_REPO_PATH"
stub_pom="$(mktemp "${TMPDIR:-/tmp}/codenameone-cef-stub.XXXXXX.pom")"
trap 'rm -f "$stub_pom"' EXIT

cat > "$stub_pom" <<'POM'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.codenameone</groupId>
  <artifactId>codenameone-cef</artifactId>
  <version>84.4.1-M3</version>
  <packaging>pom</packaging>
  <name>codenameone-cef stub</name>
  <description>Stub artifact to satisfy codenameone-cef dependency on platforms where CEF binaries are unavailable.</description>
</project>
POM

"$MAVEN_BIN" -B -ntp org.apache.maven.plugins:maven-install-plugin:3.1.2:install-file \
  -Dfile="$stub_pom" \
  -DgroupId=com.codenameone \
  -DartifactId=codenameone-cef \
  -Dversion="$CEF_VERSION" \
  -Dpackaging=pom \
  -DgeneratePom=false \
  -DcreateChecksum=true \
  -DlocalRepositoryPath="$LOCAL_MAVEN_REPO"

log "Stub $CEF_COORD installed"
