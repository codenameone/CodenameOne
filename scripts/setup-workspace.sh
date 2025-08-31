#!/usr/bin/env bash
###
# Prepare Codename One workspace by installing Maven, provisioning JDK 11 and JDK 17,
# building core modules, and installing Maven archetypes.
###
set -euo pipefail

log() {
  echo "[setup-workspace] $1"
}

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/tools"
mkdir -p "$TOOLS"

JAVA_HOME="${JAVA_HOME:-}"
JAVA_HOME_17="${JAVA_HOME_17:-}"
MAVEN_HOME="${MAVEN_HOME:-}"

log "Detecting host platform"
os_name=$(uname -s)
arch_name=$(uname -m)
case "$os_name" in
  Linux) os="linux" ;;
  Darwin) os="mac" ;;
  *) echo "Unsupported OS: $os_name" >&2; exit 1 ;;
esac
case "$arch_name" in
  x86_64|amd64) arch="x64" ;;
  arm64|aarch64) arch="aarch64" ;;
  *) echo "Unsupported architecture: $arch_name" >&2; exit 1 ;;
esac

# Determine platform-specific JDK download URLs
JDK11_URL="https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.28%2B6/OpenJDK11U-jdk_${arch}_${os}_hotspot_11.0.28_6.tar.gz"
JDK17_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_${arch}_${os}_hotspot_17.0.16_8.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"

install_jdk() {
  local url="$1" dest_var="$2"
  local tmp="$TOOLS/jdk.tgz"
  log "Downloading JDK from $url"
  curl -fL "$url" -o "$tmp"
  local top
  top=$(tar -tzf "$tmp" 2>/dev/null | head -1 | cut -d/ -f1 || true)
  tar -xzf "$tmp" -C "$TOOLS"
  rm "$tmp"
  local home="$TOOLS/$top"
  if [ -d "$home/Contents/Home" ]; then
    home="$home/Contents/Home"
  fi
  eval "$dest_var=\"$home\""
}

log "Ensuring JDK 11 is available"
if [ ! -x "${JAVA_HOME:-}/bin/java" ] || ! "${JAVA_HOME:-}/bin/java" -version 2>&1 | grep -q '11\.0'; then
  log "Provisioning JDK 11 (this may take a while)..."
  install_jdk "$JDK11_URL" JAVA_HOME
else
  log "Using existing JDK 11 at $JAVA_HOME"
fi

log "Ensuring JDK 17 is available"
if [ ! -x "${JAVA_HOME_17:-}/bin/java" ] || ! "${JAVA_HOME_17:-}/bin/java" -version 2>&1 | grep -q '17\.0'; then
  log "Provisioning JDK 17 (this may take a while)..."
  install_jdk "$JDK17_URL" JAVA_HOME_17
else
  log "Using existing JDK 17 at $JAVA_HOME_17"
fi

log "Ensuring Maven is available"
if ! [ -x "${MAVEN_HOME:-}/bin/mvn" ]; then
  tmp="$TOOLS/maven.tgz"
  log "Downloading Maven from $MAVEN_URL"
  curl -fL "$MAVEN_URL" -o "$tmp"
  tar -xzf "$tmp" -C "$TOOLS"
  rm "$tmp"
  MAVEN_HOME="$TOOLS/apache-maven-3.9.6"
else
  log "Using existing Maven at $MAVEN_HOME"
fi

log "Writing environment to $TOOLS/env.sh"
cat > "$TOOLS/env.sh" <<ENV
export JAVA_HOME="$JAVA_HOME"
export JAVA_HOME_17="$JAVA_HOME_17"
export MAVEN_HOME="$MAVEN_HOME"
export PATH="\$JAVA_HOME/bin:\$MAVEN_HOME/bin:\$PATH"
ENV

source "$TOOLS/env.sh"

log "JDK 11 version:"; "$JAVA_HOME/bin/java" -version
log "JDK 17 version:"; "$JAVA_HOME_17/bin/java" -version
log "Maven version:"; "$MAVEN_HOME/bin/mvn" -version

PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

log "Building Codename One core modules"
"$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml -DskipTests -Djava.awt.headless=true install "$@"

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
log "Ensuring CodeNameOneBuildClient.jar is installed"
if [ ! -f "$BUILD_CLIENT" ]; then
  if ! "$MAVEN_HOME/bin/mvn" -f maven/pom.xml cn1:install-codenameone "$@"; then
    log "Falling back to copying CodeNameOneBuildClient.jar"
    mkdir -p "$(dirname "$BUILD_CLIENT")"
    cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT" || true
  fi
fi

log "Installing cn1-maven-archetypes"
if [ ! -d cn1-maven-archetypes ]; then
  git clone https://github.com/shannah/cn1-maven-archetypes
fi
(cd cn1-maven-archetypes && "$MAVEN_HOME/bin/mvn" -q -DskipTests -DskipITs=true -Dinvoker.skip=true install)
