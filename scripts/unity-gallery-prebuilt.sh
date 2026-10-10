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
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Codename One through http://www.codenameone.com/ if you
# need additional information or have any questions.
#
# Carries the compiled Unity gallery of scripts/hellocodenameone from the one
# machine that has the .NET SDK to the ones that build the application
# without it.
#
#   scripts/unity-gallery-prebuilt.sh pack   <out.tgz>
#   scripts/unity-gallery-prebuilt.sh unpack <in.tgz>
#
# scripts/hellocodenameone/common compiles scripts/unity-compat-samples/gallery
# with the compile-unity goal. That goal compiles C# -- which needs the .NET
# SDK, and the two Maven modules behind the unity-compat profile, which need
# it to build -- only when what it finds under common/target/unity was not made
# from the same project with the same runtime. It decides that by content
# alone, never by a path or a time, so the output of one build is good on
# another machine, another operating system and another checkout of the same
# commit.
#
# `pack` runs after a build of the application's common module on a machine
# with the SDK. The archive holds:
#
#   repo/     the Maven artifacts the goal resolves, in repository layout:
#             codenameone-unity-compat (jar, references jar, pom),
#             codenameone-cil-translator (jar, pom) and the jars the
#             translator runs with, each with its pom and its parents'
#   target/   common/target/unity/{state.txt,installed.txt,app,resources,runtime}
#             and common/target/generated-sources/unity
#
# `unpack` puts both where a build reads them: the artifacts in the local
# Maven repository and the output under scripts/hellocodenameone/common/target.
# Run it after the checkout and before the first Maven command that builds the
# application; every later build of that checkout finds it.
#
# Two things undo it, and both end in the goal asking for the .NET SDK:
#
#   - removing common/target (`mvn clean` on the application, a fresh clone).
#     Removing common/target/classes alone is harmless; the goal refills it.
#   - other jars than the archive's. The check covers every class of the
#     runtime, the references and the translator, so a codenameone-unity-compat
#     or codenameone-cil-translator built anywhere else -- or an archive of
#     another commit -- is a different project as far as the goal can tell.
#     `unpack` therefore replaces those two artifacts in the repository; it
#     adds the third-party ones only where they are missing.
#
# The local repository is MAVEN_REPO_LOCAL when that is set and
# ~/.m2/repository when not.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP="${UNITY_GALLERY_APP_DIR:-$ROOT/scripts/hellocodenameone}"
COMMON="$APP/common"
REPO="${MAVEN_REPO_LOCAL:-$HOME/.m2/repository}"
GROUP_PATH="com/codenameone"
RUNTIME="codenameone-unity-compat"
TOOL="codenameone-cil-translator"

log() { echo "[unity-gallery-prebuilt] $1"; }
die() { echo "[unity-gallery-prebuilt] $1" >&2; exit 1; }

usage() {
  die "usage: $0 pack <out.tgz> | unpack <in.tgz>"
}

# The first <tag> value of a pom's own coordinates or of its <parent> block.
pom_parent_field() {
  sed -n '/<parent>/,/<\/parent>/p' "$1" | sed -n "s:.*<$2>\\(.*\\)</$2>.*:\\1:p" | head -n 1
}

# Adds a pom and every parent it names to the staged repository.
stage_pom_chain() {
  local rel="$1" stage="$2" pom group artifact version
  while [ -n "$rel" ]; do
    pom="$REPO/$rel"
    [ -f "$pom" ] || die "$pom is missing; build the application's common module first"
    mkdir -p "$stage/$(dirname "$rel")"
    cp "$pom" "$stage/$rel"
    group="$(pom_parent_field "$pom" groupId)"
    artifact="$(pom_parent_field "$pom" artifactId)"
    version="$(pom_parent_field "$pom" version)"
    if [ -z "$group" ] || [ -z "$artifact" ] || [ -z "$version" ]; then
      break
    fi
    rel="$(echo "$group" | tr . /)/$artifact/$version/$artifact-$version.pom"
    # The framework's own parent is whatever the consumer built; it is
    # never taken from an archive.
    case "$rel" in "$GROUP_PATH"/*) break ;; esac
  done
}

pack() {
  local out="$1" version stage work tool_cp entry rel
  [ -f "$COMMON/target/unity/state.txt" ] || die "$COMMON/target/unity/state.txt is missing: build the common module of $APP first, on a machine with the .NET SDK"
  [ -d "$COMMON/target/unity/app" ] || die "$COMMON/target/unity/app is missing"
  [ -d "$COMMON/target/generated-sources/unity" ] || die "$COMMON/target/generated-sources/unity is missing"
  version="$(sed -n 's:.*<cn1.version>\(.*\)</cn1.version>.*:\1:p' "$APP/pom.xml" | head -n 1)"
  [ -n "$version" ] || die "no cn1.version in $APP/pom.xml"

  work="$(mktemp -d "${TMPDIR:-/tmp}/unity-gallery-pack.XXXXXX")"
  # shellcheck disable=SC2064
  trap "rm -rf '$work'" EXIT
  stage="$work/stage"
  mkdir -p "$stage/repo/$GROUP_PATH/$RUNTIME/$version" "$stage/repo/$GROUP_PATH/$TOOL/$version" \
    "$stage/target/unity" "$stage/target/generated-sources"

  for entry in "$RUNTIME/$version/$RUNTIME-$version.jar" "$RUNTIME/$version/$RUNTIME-$version-references.jar" \
      "$RUNTIME/$version/$RUNTIME-$version.pom" "$TOOL/$version/$TOOL-$version.jar" \
      "$TOOL/$version/$TOOL-$version.pom"; do
    [ -f "$REPO/$GROUP_PATH/$entry" ] || die "$REPO/$GROUP_PATH/$entry is missing: install both Unity modules (-Dunity-compat) into this repository first"
    cp "$REPO/$GROUP_PATH/$entry" "$stage/repo/$GROUP_PATH/$entry"
  done

  # What the translator runs with, as Maven resolves it from the installed pom
  # -- the same resolution the goal performs.
  tool_cp="$work/tool-classpath.txt"
  mvn -B -q -Dmaven.repo.local="$REPO" -f "$REPO/$GROUP_PATH/$TOOL/$version/$TOOL-$version.pom" \
    dependency:build-classpath -DincludeScope=runtime -Dmdep.pathSeparator=: \
    -Dmdep.outputFile="$tool_cp" >"$work/classpath.log" 2>&1 \
    || { cat "$work/classpath.log" >&2; die "could not resolve the class path of $TOOL"; }
  # The file ends without a newline, which `read` reports as a failure
  # while still giving the line.
  tr ':' '\n' <"$tool_cp" | while IFS= read -r entry || [ -n "$entry" ]; do
    [ -n "$entry" ] || continue
    case "$entry" in
      "$REPO"/*) rel="${entry#"$REPO"/}" ;;
      *) die "$entry is not in $REPO" ;;
    esac
    mkdir -p "$stage/repo/$(dirname "$rel")"
    cp "$entry" "$stage/repo/$rel"
    stage_pom_chain "${rel%.jar}.pom" "$stage/repo"
  done

  for entry in state.txt installed.txt app resources runtime; do
    [ -e "$COMMON/target/unity/$entry" ] || die "$COMMON/target/unity/$entry is missing"
    cp -R "$COMMON/target/unity/$entry" "$stage/target/unity/$entry"
  done
  cp -R "$COMMON/target/generated-sources/unity" "$stage/target/generated-sources/unity"
  {
    echo "cn1.version=$version"
    # The digest is the first line; under it the file lists what it is of.
    echo "state=$(head -n 1 "$COMMON/target/unity/state.txt")"
  } >"$stage/pack-info.txt"

  mkdir -p "$(dirname "$out")"
  # Through a redirection, here and in `unpack`: GNU tar reads a colon in
  # the name of its archive as "host:path", and the temporary directory of a
  # Windows runner is D:\a\_temp.
  tar -czf - -C "$stage" pack-info.txt repo target >"$out"
  log "packed $(cd "$stage" && find repo -type f | wc -l | tr -d ' ') repository files and $(cd "$stage" && find target -type f | wc -l | tr -d ' ') output files into $out"
  (cd "$stage" && find repo -type f | sort) | while IFS= read -r entry; do log "  $entry"; done
}

unpack() {
  local in="$1" work rel
  [ -f "$in" ] || die "$in does not exist"
  [ -d "$COMMON" ] || die "$COMMON does not exist"
  work="$(mktemp -d "${TMPDIR:-/tmp}/unity-gallery-unpack.XXXXXX")"
  # shellcheck disable=SC2064
  trap "rm -rf '$work'" EXIT
  tar -xzf - -C "$work" <"$in"
  [ -f "$work/target/unity/state.txt" ] || die "$in is not an archive this script packed"

  (cd "$work/repo" && find . -type f | sed 's:^\./::' | sort) | while IFS= read -r rel; do
    case "$rel" in
      "$GROUP_PATH/$RUNTIME"/*|"$GROUP_PATH/$TOOL"/*) ;;
      *) [ -f "$REPO/$rel" ] && continue ;;
    esac
    mkdir -p "$REPO/$(dirname "$rel")"
    cp "$work/repo/$rel" "$REPO/$rel"
  done
  # Maven records where it downloaded a file from, and refuses one whose
  # record names a repository this build does not have. These were copied.
  rm -f "$REPO/$GROUP_PATH/$RUNTIME"/*/_remote.repositories "$REPO/$GROUP_PATH/$TOOL"/*/_remote.repositories

  rm -rf "$COMMON/target/unity" "$COMMON/target/generated-sources/unity"
  mkdir -p "$COMMON/target/generated-sources"
  cp -R "$work/target/unity" "$COMMON/target/unity"
  cp -R "$work/target/generated-sources/unity" "$COMMON/target/generated-sources/unity"
  log "unpacked $in ($(tr '\n' ' ' <"$work/pack-info.txt")) into $REPO and $COMMON/target"
}

[ $# -eq 2 ] || usage
case "$1" in
  pack) pack "$2" ;;
  unpack) unpack "$2" ;;
  *) usage ;;
esac
