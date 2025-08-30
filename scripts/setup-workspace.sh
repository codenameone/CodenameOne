#!/bin/bash
# Prepare Codename One workspace by installing Maven, provisioning JDK 11 and JDK 17,
# building core modules, and installing Maven archetypes
set -e
DIR="$(cd "$(dirname "$0")" && pwd)/.."
cd "$DIR"

TOOLS_DIR="$DIR/tools"
mkdir -p "$TOOLS_DIR"

ensure_jdk() {
  local envvar="$1" version="$2" url="$3"
  local current="${!envvar}"
  if [ -z "$current" ] || ! "$current/bin/java" -version 2>&1 | head -n1 | grep -q "$version"; then
    local existing=$(ls -d "$TOOLS_DIR"/jdk-$version* 2>/dev/null | head -n1)
    if [ -z "$existing" ]; then
      echo "Downloading JDK $version (this may take a while)..."
      local archive="$TOOLS_DIR/jdk$version.tar.gz"
      curl -L -o "$archive" "$url"
      tar -xzf "$archive" -C "$TOOLS_DIR"
      rm "$archive"
      existing=$(ls -d "$TOOLS_DIR"/jdk-$version* | head -n1)
    fi
    export "$envvar"="$existing"
  fi
}

ensure_maven() {
  local version="3.9.6"
  MAVEN_HOME="$TOOLS_DIR/apache-maven-$version"
  if [ ! -d "$MAVEN_HOME" ]; then
    local archive="$TOOLS_DIR/apache-maven-$version-bin.tar.gz"
    echo "Downloading Maven $version..."
    curl -L -o "$archive" "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.tar.gz"
    tar -xzf "$archive" -C "$TOOLS_DIR"
    rm "$archive"
  fi
  export MAVEN_HOME
  export PATH="$MAVEN_HOME/bin:$PATH"
}

detect_platform() {
  case "$(uname -s)" in
    Linux) platform="linux" ;;
    Darwin) platform="mac" ;;
    *) echo "Unsupported OS: $(uname -s)" >&2; exit 1 ;;
  esac

  case "$(uname -m)" in
    x86_64|amd64) arch="x64" ;;
    arm64|aarch64) arch="aarch64" ;;
    *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
  esac
}

detect_platform

JDK11_URL="https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.28%2B6/OpenJDK11U-jdk_${arch}_${platform}_hotspot_11.0.28_6.tar.gz"
JDK17_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_${arch}_${platform}_hotspot_17.0.16_8.tar.gz"

ensure_jdk JAVA_HOME 11 "$JDK11_URL"
ensure_jdk JAVA_HOME_17 17 "$JDK17_URL"
ensure_maven

cat > "$TOOLS_DIR/env.sh" <<EOF
export JAVA_HOME="$JAVA_HOME"
export JAVA_HOME_17="$JAVA_HOME_17"
export MAVEN_HOME="$MAVEN_HOME"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:\$PATH"
EOF

source "$TOOLS_DIR/env.sh"

mvn -f maven/pom.xml install "$@"

BUILD_CLIENT="$HOME/.codenameone/CodeNameOneBuildClient.jar"
if [ ! -f "$BUILD_CLIENT" ]; then
  echo "Installing Codename One build client..."
  if ! mvn -f maven/pom.xml cn1:install-codenameone "$@"; then
    echo "Falling back to manual copy of build client jar." >&2
    if [ -f maven/CodeNameOneBuildClient.jar ]; then
      mkdir -p "$(dirname "$BUILD_CLIENT")"
      cp maven/CodeNameOneBuildClient.jar "$BUILD_CLIENT"
    else
      echo "maven/CodeNameOneBuildClient.jar not found." >&2
    fi
  fi
fi

if [ ! -d cn1-maven-archetypes ]; then
  git clone https://github.com/shannah/cn1-maven-archetypes
fi
(
  cd cn1-maven-archetypes
  mvn install "$@"
)
