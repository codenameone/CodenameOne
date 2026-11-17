#!/usr/bin/env bash
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Codename One designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# Create a Codename One project from a shell, with nothing installed first.
#
#   curl -fsSL https://www.codenameone.com/agents/create-app.sh | bash -s -- \
#       --name HabitTracker --package com.acme.habits [--template BAREBONES] ...
#   create-app.sh --list          # the templates, one line each
#   create-app.sh --jdk-only      # just make sure a JDK 17+ exists, print JAVA_HOME
#
# Every option after the script's own is passed to CreateProject, which is the
# start.codenameone.com generator as a command; see `--help`.
#
# What it does, and nothing else:
#   1. Finds a JDK 17 or newer (JAVA_HOME, then java on PATH). When there is
#      none, downloads a Temurin 17 JDK from api.adoptium.net into
#      $CN1_AGENT_HOME/jdk (default ~/.codenameone/agent). No admin rights, no
#      system-wide change.
#   2. Downloads the generator (cn1-create.jar) and the two Codename One jars it
#      runs against into $CN1_AGENT_HOME, once per release.
#   3. Runs it. The project it writes carries its own Maven wrapper, so no Maven
#      install is needed either.
#
# The last line printed on success is `JAVA_HOME=<path>`: export it before
# running ./mvnw in the new project.
set -euo pipefail

SITE="${CN1_AGENT_SITE:-https://www.codenameone.com/agents}"
REPO="${CN1_MAVEN_REPO:-https://repo.codenameone.com/maven2}"
HOME_DIR="${CN1_AGENT_HOME:-$HOME/.codenameone/agent}"
export CN1_TELEMETRY="${CN1_TELEMETRY:-0}"

say() { echo "create-app: $*" >&2; }
die() { say "$*"; exit 1; }

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*)
    # Git Bash: java wants Windows paths and ';' between class path entries.
    die "on Windows run create-app.ps1 from PowerShell (see https://www.codenameone.com/agents/)" ;;
esac

java_major() {
  # Prints the major version of the java at $1, or nothing.
  "$1" -version 2>&1 | sed -n 's/.*version "\([0-9]*\)[."].*/\1/p' | head -n1
}

find_jdk() {
  local candidates=()
  [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")
  if command -v java >/dev/null 2>&1; then
    local j; j="$(command -v java)"
    # Resolve symlinks (macOS /usr/bin/java is a stub; java_home knows the real one).
    if [ "$(uname -s)" = Darwin ] && /usr/libexec/java_home -v 17+ >/dev/null 2>&1; then
      candidates+=("$(/usr/libexec/java_home -v 17+)")
    fi
    candidates+=("$(cd "$(dirname "$j")/.." && pwd)")
  fi
  [ -d "$HOME_DIR/jdk" ] && for d in "$HOME_DIR"/jdk/*/; do
    [ -x "$d/bin/java" ] && candidates+=("${d%/}")
    [ -x "$d/Contents/Home/bin/java" ] && candidates+=("${d%/}/Contents/Home")
  done
  local c m
  for c in "${candidates[@]}"; do
    [ -x "$c/bin/java" ] && [ -x "$c/bin/javac" ] || continue
    m="$(java_major "$c/bin/java")"
    if [ -n "$m" ] && [ "$m" -ge 17 ] 2>/dev/null; then echo "$c"; return 0; fi
  done
  return 1
}

download_jdk() {
  local os arch
  case "$(uname -s)" in
    Darwin) os=mac ;;
    Linux) os=linux ;;
    *) die "unsupported OS $(uname -s); on Windows run create-app.ps1" ;;
  esac
  case "$(uname -m)" in
    x86_64|amd64) arch=x64 ;;
    arm64|aarch64) arch=aarch64 ;;
    *) die "unsupported CPU $(uname -m)" ;;
  esac
  local url="https://api.adoptium.net/v3/binary/latest/17/ga/$os/$arch/jdk/hotspot/normal/eclipse"
  say "no JDK 17+ found; downloading Temurin 17 for $os/$arch into $HOME_DIR/jdk"
  mkdir -p "$HOME_DIR/jdk"
  local tmp; tmp="$(mktemp "$HOME_DIR/jdk/download.XXXXXX")"
  curl -fsSL "$url" -o "$tmp" || die "JDK download failed from $url"
  tar -xzf "$tmp" -C "$HOME_DIR/jdk" || die "could not unpack the JDK"
  rm -f "$tmp"
}

JDK="$(find_jdk || true)"
if [ -z "$JDK" ]; then
  download_jdk
  JDK="$(find_jdk || true)"
  [ -n "$JDK" ] || die "downloaded a JDK but could not find bin/java in $HOME_DIR/jdk"
fi
say "using JDK at $JDK"

if [ "${1:-}" = "--jdk-only" ]; then
  echo "JAVA_HOME=$JDK"
  exit 0
fi

mkdir -p "$HOME_DIR"
curl -fsSL "$SITE/cn1-create.properties" -o "$HOME_DIR/cn1-create.properties.new" \
  || die "could not read $SITE/cn1-create.properties"
VERSION="$(sed -n 's/^cn1.version=//p' "$HOME_DIR/cn1-create.properties.new")"
[ -n "$VERSION" ] || die "no cn1.version in $SITE/cn1-create.properties"
KIT="$(sed -n 's/^kit.build=//p' "$HOME_DIR/cn1-create.properties.new")"
LIB="$HOME_DIR/lib/$VERSION-${KIT:-0}"
if [ ! -f "$LIB/.complete" ]; then
  say "downloading the project generator for Codename One $VERSION"
  mkdir -p "$LIB"
  curl -fsSL "$SITE/cn1-create.jar" -o "$LIB/cn1-create.jar"
  for a in codenameone-core codenameone-javase; do
    curl -fsSL "$REPO/com/codenameone/$a/$VERSION/$a-$VERSION.jar" -o "$LIB/$a.jar"
  done
  touch "$LIB/.complete"
fi

# A complete-app template is its own download (templates/<name>.jar), fetched
# only when asked for and cached under its own hash.
CP="$LIB/cn1-create.jar:$LIB/codenameone-core.jar:$LIB/codenameone-javase.jar"
TEMPLATE=""
prev=""
for a in "$@"; do
  [ "$prev" = "--template" ] && TEMPLATE="$(printf '%s' "$a" | tr '[:upper:]' '[:lower:]')"
  prev="$a"
done
if [ -n "$TEMPLATE" ]; then
  THASH="$(sed -n "s/^template\.$TEMPLATE=//p" "$HOME_DIR/cn1-create.properties.new")"
  if [ -n "$THASH" ]; then
    TJAR="$HOME_DIR/templates/$TEMPLATE-$THASH.jar"
    if [ ! -f "$TJAR" ]; then
      say "downloading the $TEMPLATE template"
      mkdir -p "$HOME_DIR/templates"
      curl -fsSL "$SITE/templates/$TEMPLATE.jar" -o "$TJAR.part" || die "could not download $SITE/templates/$TEMPLATE.jar"
      mv -f "$TJAR.part" "$TJAR"
    fi
    CP="$CP:$TJAR"
  fi
fi
mv -f "$HOME_DIR/cn1-create.properties.new" "$HOME_DIR/cn1-create.properties"

"$JDK/bin/java" -Djava.awt.headless=true -cp "$CP" \
  com.codename1.initializr.model.CreateProject "$@"
echo "JAVA_HOME=$JDK"
