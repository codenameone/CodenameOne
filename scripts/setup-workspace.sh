#!/usr/bin/env bash
# Prepare Codename One workspace by installing Maven, provisioning JDK 8 and JDK 17,
# building core modules, and installing Maven archetypes.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/tools"
mkdir -p "$TOOLS"

JAVA_HOME="${JAVA_HOME:-}"
JAVA_HOME_17="${JAVA_HOME_17:-}"
MAVEN_HOME="${MAVEN_HOME:-}"

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

# JDK 8 is not available for Apple Silicon. Use the Intel build when running on macOS ARM.
jdk8_arch="$arch"
jdk17_arch="$arch"
if [ "$os" = "mac" ] && [ "$arch" = "aarch64" ]; then
  jdk8_arch="x64"
fi

JDK8_URL="https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jdk_${jdk8_arch}_${os}_hotspot_8u462b08.tar.gz"
JDK17_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_${jdk17_arch}_${os}_hotspot_17.0.16_8.tar.gz"
MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz"

install_jdk() {
  local url="$1" dest_var="$2"
  local tmp="$TOOLS/jdk.tgz"
  echo "Downloading JDK from $url"
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

if [ ! -x "${JAVA_HOME:-}/bin/java" ] || ! "${JAVA_HOME:-}/bin/java" -version 2>&1 | grep -q '1\.8'; then
  echo "Provisioning JDK 8 (this may take a while)..."
  install_jdk "$JDK8_URL" JAVA_HOME
fi

if [ ! -x "${JAVA_HOME_17:-}/bin/java" ] || ! "${JAVA_HOME_17:-}/bin/java" -version 2>&1 | grep -q '17\.0'; then
  echo "Provisioning JDK 17 (this may take a while)..."
  install_jdk "$JDK17_URL" JAVA_HOME_17
fi

if ! [ -x "${MAVEN_HOME:-}/bin/mvn" ]; then
  echo "Downloading Maven from $MAVEN_URL"
  tmp="$TOOLS/maven.tgz"
  curl -fL "$MAVEN_URL" -o "$tmp"
  tar -xzf "$tmp" -C "$TOOLS"
  rm "$tmp"
  MAVEN_HOME="$TOOLS/apache-maven-3.9.6"
fi

cat > "$TOOLS/env.sh" <<ENV
export JAVA_HOME="$JAVA_HOME"
export JAVA_HOME_17="$JAVA_HOME_17"
export MAVEN_HOME="$MAVEN_HOME"
export PATH="\$JAVA_HOME/bin:\$MAVEN_HOME/bin:\$PATH"
ENV

source "$TOOLS/env.sh"

"$JAVA_HOME/bin/java" -version
"$JAVA_HOME_17/bin/java" -version
"$MAVEN_HOME/bin/mvn" -version

PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
("$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml install "$@")

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
if [ ! -f "$BUILD_CLIENT" ]; then
  if ! "$MAVEN_HOME/bin/mvn" -q -f maven/pom.xml cn1:install-codenameone "$@"; then
    mkdir -p "$(dirname "$BUILD_CLIENT")"
    cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT" || true
  fi
fi

if [ ! -d cn1-maven-archetypes ]; then
  git clone https://github.com/shannah/cn1-maven-archetypes
fi
(cd cn1-maven-archetypes && "$MAVEN_HOME/bin/mvn" -q -DskipTests -DskipITs install)
