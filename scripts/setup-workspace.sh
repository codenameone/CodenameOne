#!/usr/bin/env bash
###
# Prepare Codename One workspace by installing Maven, provisioning JDK 8, 11 and 17,
# building core modules, and installing Maven archetypes.
# IMPORTANT: Run this script from the project root!
###
set -euo pipefail
[ "${DEBUG:-0}" = "1" ] && set -x

log() {
  echo "[setup-workspace] $1"
}

# Normalize TMPDIR so it has no trailing slash
TMPDIR="${TMPDIR:-/tmp}"
TMPDIR="${TMPDIR%/}"

# Place downloaded tools outside the repository so it isn't filled with binaries
DOWNLOAD_DIR="$TMPDIR/codenameone-tools"
mkdir -p "$DOWNLOAD_DIR"
ENV_DIR="$DOWNLOAD_DIR/tools"
mkdir -p "$ENV_DIR"
mkdir -p ../cn1-binaries
CN1_BINARIES="$(cd ../cn1-binaries && pwd -P)"
rm -Rf ../cn1-binaries

mkdir -p ~/.codenameone
cp maven/CodeNameOneBuildClient.jar ~/.codenameone

# Reuse previously saved environment if present (so we can skip downloads)
if [ -f "$ENV_DIR/env.sh" ]; then
  # shellcheck disable=SC1090
  source "$ENV_DIR/env.sh"
fi

JAVA_HOME="${JAVA_HOME:-}"
JAVA_HOME_11="${JAVA_HOME_11:-}"
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
arch_jdk8="$arch"
if [ "$os" = "mac" ] && [ "$arch" = "aarch64" ]; then
  arch_jdk8="x64"
fi

JDK8_URL="https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jdk_${arch_jdk8}_${os}_hotspot_8u462b08.tar.gz"
JDK11_URL="https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.28%2B6/OpenJDK11U-jdk_${arch}_${os}_hotspot_11.0.28_6.tar.gz"
JDK17_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_${arch}_${os}_hotspot_17.0.16_8.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"

install_jdk() {
  local url="$1" dest_var="$2"
  local archive="$DOWNLOAD_DIR/$(basename "$url")"

  if [ -f "$archive" ]; then
    log "Using cached JDK archive $(basename "$archive")"
  else
    log "Downloading JDK from $url"
    curl -fL "$url" -o "$archive"
  fi

  # Find top directory name inside the tarball
  local top
  top=$(tar -tzf "$archive" 2>/dev/null | head -1 | cut -d/ -f1 || true)

  # Extract only if target directory doesn't already exist
  if [ -n "$top" ] && [ -d "$DOWNLOAD_DIR/$top" ]; then
    log "JDK already extracted at $DOWNLOAD_DIR/$top"
  else
    log "Extracting JDK to $DOWNLOAD_DIR"
    tar -xzf "$archive" -C "$DOWNLOAD_DIR"
  fi

  local home="$DOWNLOAD_DIR/$top"
  if [ -d "$home/Contents/Home" ]; then
    home="$home/Contents/Home"
  fi
  eval "$dest_var=\"$home\""
}

log "Ensuring JDK 8 is available"
if [ ! -x "${JAVA_HOME:-}/bin/java" ] || ! "${JAVA_HOME:-}/bin/java" -version 2>&1 | grep -q '8\.0'; then
  log "Provisioning JDK 8..."
  install_jdk "$JDK8_URL" JAVA_HOME
else
  log "Using existing JDK 8 at $JAVA_HOME"
fi

log "Ensuring JDK 11 is available"
if [ ! -x "${JAVA_HOME_11:-}/bin/java" ] || ! "${JAVA_HOME_11:-}/bin/java" -version 2>&1 | grep -q '11\.0'; then
  log "Provisioning JDK 11..."
  install_jdk "$JDK11_URL" JAVA_HOME_11
else
  log "Using existing JDK 11 at $JAVA_HOME_11"
fi

log "Ensuring JDK 17 is available"
if [ ! -x "${JAVA_HOME_17:-}/bin/java" ] || ! "${JAVA_HOME_17:-}/bin/java" -version 2>&1 | grep -q '17\.0'; then
  log "Provisioning JDK 17..."
  install_jdk "$JDK17_URL" JAVA_HOME_17
else
  log "Using existing JDK 17 at $JAVA_HOME_17"
fi

log "Ensuring Maven is available"
if ! [ -x "${MAVEN_HOME:-}/bin/mvn" ]; then
  mvn_archive="$DOWNLOAD_DIR/$(basename "$MAVEN_URL")"
  if [ -f "$mvn_archive" ]; then
    log "Using cached Maven archive $(basename "$mvn_archive")"
  else
    log "Downloading Maven from $MAVEN_URL"
    curl -fL "$MAVEN_URL" -o "$mvn_archive"
  fi
  mvn_top=$(tar -tzf "$mvn_archive" 2>/dev/null | head -1 | cut -d/ -f1 || true)
  if [ -n "$mvn_top" ] && [ -d "$DOWNLOAD_DIR/$mvn_top" ]; then
    log "Maven already extracted at $DOWNLOAD_DIR/$mvn_top"
  else
    log "Extracting Maven to $DOWNLOAD_DIR"
    tar -xzf "$mvn_archive" -C "$DOWNLOAD_DIR"
  fi
  MAVEN_HOME="$DOWNLOAD_DIR/$mvn_top"
else
  log "Using existing Maven at $MAVEN_HOME"
fi

log "Writing environment to $ENV_DIR/env.sh"
cat > "$ENV_DIR/env.sh" <<ENV
export JAVA_HOME="$JAVA_HOME"
export JAVA_HOME_11="$JAVA_HOME_11"
export JAVA_HOME_17="$JAVA_HOME_17"
export MAVEN_HOME="$MAVEN_HOME"
export PATH="\$JAVA_HOME/bin:\$MAVEN_HOME/bin:\$PATH"
ENV

# shellcheck disable=SC1090
source "$ENV_DIR/env.sh"

log "JDK 8 version:"; "$JAVA_HOME/bin/java" -version
log "JDK 11 version:"; "$JAVA_HOME_11/bin/java" -version
log "JDK 17 version:"; "$JAVA_HOME_17/bin/java" -version
log "Maven version:"; "$MAVEN_HOME/bin/mvn" -version

PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

log "Cloning cn1-binaries"
rm -Rf "$CN1_BINARIES"
git clone https://github.com/codenameone/cn1-binaries "$CN1_BINARIES"

log "Building Codename One core modules"
"$MAVEN_HOME/bin/mvn" -f maven/pom.xml -DskipTests -Djava.awt.headless=true -Dcn1.binaries="$CN1_BINARIES" -Dcodename1.platform=javase -P local-dev-javase,compile-android install "$@"

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
set +e  # don't let a transient git failure abort the whole build
if [ -d cn1-maven-archetypes/.git ]; then
  log "Updating existing cn1-maven-archetypes checkout"
  if ! git -C cn1-maven-archetypes fetch --all --tags; then
    log "git fetch failed (exit 128?). Leaving existing copy as-is."
  else
    git -C cn1-maven-archetypes reset --hard origin/master || \
      log "git reset failed; keeping local state."
  fi
else
  if ! git clone https://github.com/shannah/cn1-maven-archetypes cn1-maven-archetypes; then
    log "git clone failed (likely exit 128). Skipping archetype install."
    skip_archetypes=1
  fi
fi
set -e

if [ "${skip_archetypes:-0}" -eq 0 ]; then
  (cd cn1-maven-archetypes && "$MAVEN_HOME/bin/mvn" -DskipTests -DskipITs=true -Dinvoker.skip=true install) || \
    log "Archetype mvn install failed; continuing."
fi