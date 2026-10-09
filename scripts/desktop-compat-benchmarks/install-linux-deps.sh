#!/usr/bin/env bash
#
# Installs everything the Linux leg of the desktop-compat benchmarks needs on a
# Debian/Ubuntu machine: the GTK build stack of the native Linux port (the list
# is the one .github/workflows/linux-build-run.yml installs), JDK 17 for the
# Codename One build, JDK 21 for the baseline, OpenJFX (as jmods) and
# the zig toolchain the native port links an old-glibc binary with.
#
# Usage: install-linux-deps.sh <tools-dir>
#   <tools-dir> receives zig/ and javafx-jmods/. Nothing else is written outside
#   the package manager's own directories.
#
# Used both by the Containerfile beside it and by the CI workflow, so the local
# container and the runner measure the same stack.
set -euo pipefail

TOOLS_DIR="${1:?usage: install-linux-deps.sh <tools-dir>}"
ZIG_VERSION="${BENCH_ZIG_VERSION:-0.14.1}"

SUDO=""
if [ "$(id -u)" -ne 0 ]; then
  SUDO="sudo"
fi

export DEBIAN_FRONTEND=noninteractive
$SUDO apt-get update
$SUDO apt-get install -y --no-install-recommends \
  ca-certificates curl unzip zip xz-utils file time binutils procps git \
  cmake ninja-build pkg-config clang \
  xvfb x11-utils openbox fonts-dejavu-core python3 python3-xlib \
  libgtk-3-dev libcairo2-dev libpango1.0-dev libgdk-pixbuf-2.0-dev libglib2.0-dev \
  libfontconfig1-dev libfreetype-dev \
  libcurl4-openssl-dev libssl-dev \
  libgstreamer1.0-dev libgstreamer-plugins-base1.0-dev gstreamer1.0-plugins-base gstreamer1.0-plugins-good \
  libwebkit2gtk-4.1-dev libsecret-1-dev libnotify-dev libgeoclue-2-dev \
  libepoxy-dev libegl1-mesa-dev libgles2-mesa-dev libgl1-mesa-dri \
  libxtst6 libxxf86vm1 \
  openjdk-17-jdk openjdk-21-jdk maven

mkdir -p "$TOOLS_DIR"
ARCH="$(uname -m)"

if [ ! -x "$TOOLS_DIR/zig/zig" ]; then
  curl -fsSL -o "$TOOLS_DIR/zig.tar.xz" \
    "https://ziglang.org/download/${ZIG_VERSION}/zig-${ARCH}-linux-${ZIG_VERSION}.tar.xz"
  mkdir -p "$TOOLS_DIR/zig"
  tar -xf "$TOOLS_DIR/zig.tar.xz" -C "$TOOLS_DIR/zig" --strip-components=1
  rm -f "$TOOLS_DIR/zig.tar.xz"
fi

# The compiler wrapper linux-build-run.yml uses: zig cc against glibc 2.28, so
# the binary measured here is the portable one a developer would ship.
cat > "$TOOLS_DIR/cn1-zig-cc" <<WRAP
#!/bin/sh
ARCH=\$(uname -m)
exec "$TOOLS_DIR/zig/zig" cc -target \${ARCH}-linux-gnu.2.28 -D_DEFAULT_SOURCE -Wno-macro-redefined \\
  -L/usr/lib/\${ARCH}-linux-gnu -L/lib/\${ARCH}-linux-gnu "\$@"
WRAP
chmod +x "$TOOLS_DIR/cn1-zig-cc"

# OpenJFX as jmods (see javafx-jmods.sh for why jmods and not the jars).
HERE="$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)"
JDK21="/usr/lib/jvm/java-21-openjdk-$(dpkg --print-architecture)"
bash "$HERE/javafx-jmods.sh" "$JDK21" "$TOOLS_DIR/javafx-jmods"

"$TOOLS_DIR/zig/zig" version
ls "$TOOLS_DIR/javafx-jmods"
