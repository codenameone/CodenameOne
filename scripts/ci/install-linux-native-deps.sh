#!/usr/bin/env bash
#
# Installs the native Linux port's build dependencies.
#
# The port's CMakeLists resolves all of these through pkg_check_modules, so a
# missing one stops configuration before a single source file is compiled --
# "Package 'libcurl', required by 'virtual:world', not found" is what an
# incomplete list looks like, and it names only the first miss, so trimming by
# trial and error costs one CI round trip per package.
#
# One definition because there is more than one consumer: the native desktop
# workflow and the Flutter benchmark build the same local-linux-device target
# and need the same stack. A caller that needs extra packages passes them as
# arguments rather than keeping a second copy of this list.
#
# GTK3 + Cairo + Pango + GdkPixbuf + GLib/GIO (render/widgets),
# FontConfig/FreeType (bundled-font registration), libcurl (HTTP), GStreamer
# (media/camera/audio), WebKitGTK (browser), libsecret (secure storage),
# libnotify (notifications), GeoClue (location), libepoxy + EGL/GLES + the Mesa
# software driver (the offscreen 3D backend). Plus CMake/Ninja, Xvfb and a font
# for Pango to lay out.
#
# The GStreamer plugin set matters as much as the library: base + good carry
# appsrc/appsink, videoconvert and mp4mux but NO codec. VideoIO's encoder is
# x264enc/x265enc (plugins-ugly) parsed by h264parse/h265parse (plugins-bad)
# with avenc_aac for audio and avdec_* for playback (libav), so without those
# three packages the port has no encoder and no H.264 decoder at all.
#
# Usage: scripts/ci/install-linux-native-deps.sh [extra packages...]
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd -P)"

# Through the shared helper: it refreshes the index once when a .deb 404s,
# which is what a point release landing mid-job looks like -- three openssl
# packages took the desktop job out on 2026-08-25.
bash "$HERE/apt-get-install.sh" \
  cmake ninja-build pkg-config unzip xvfb fonts-dejavu-core \
  libgtk-3-dev libcairo2-dev libpango1.0-dev libgdk-pixbuf-2.0-dev libglib2.0-dev \
  libfontconfig1-dev libfreetype-dev \
  libcurl4-openssl-dev libssl-dev \
  libgstreamer1.0-dev libgstreamer-plugins-base1.0-dev gstreamer1.0-plugins-base gstreamer1.0-plugins-good \
  gstreamer1.0-plugins-bad gstreamer1.0-plugins-ugly gstreamer1.0-libav \
  libwebkit2gtk-4.1-dev libsecret-1-dev libnotify-dev libgeoclue-2-dev \
  libepoxy-dev libegl1-mesa-dev libgles2-mesa-dev libgl1-mesa-dri \
  "$@"
