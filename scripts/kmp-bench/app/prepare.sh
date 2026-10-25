#!/usr/bin/env bash
# Materialises BOTH sides of the Kotlin Multiplatform benchmark into a work
# directory, from ONE copy of the application.
#
#   scripts/kmp-bench/app/prepare.sh --work /tmp/kbench [--maven-repo DIR]
#
# shared/commonMain is the application: plain Compose Multiplatform code with
# no Codename One import in it. Both projects compile that directory as their
# commonMain, so an edit cannot land on one side only.
#
# Outputs:
#   <work>/cn1/   a Codename One project generated from the shipping archetype,
#                 with the source in common/src/main/kmp (the compose profile)
#   <work>/cmp/   a JetBrains Compose Multiplatform project (desktop and iOS)
set -Eeuo pipefail
trap 'rc=$?; echo "prepare.sh: FAILED at line $LINENO (exit $rc)" >&2' ERR

WORK=""
CN1_VERSION="${CN1_VERSION:-}"
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"
while [ $# -gt 0 ]; do
  case "$1" in
    --work) WORK="$2"; shift 2 ;;
    --cn1-version) CN1_VERSION="$2"; shift 2 ;;
    --maven-repo) MAVEN_REPO_LOCAL="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
[ -n "$WORK" ] || { echo "usage: prepare.sh --work <dir>" >&2; exit 2; }
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
if [ -z "$CN1_VERSION" ]; then
  CN1_VERSION="$(sed -n 's:.*<version>\(.*-SNAPSHOT\)</version>.*:\1:p' "$REPO/maven/pom.xml" | head -1)"
fi
MVN_REPO_ARG=""
[ -n "$MAVEN_REPO_LOCAL" ] && MVN_REPO_ARG="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"

rm -rf "$WORK/cn1" "$WORK/cmp"
mkdir -p "$WORK"

# --- Codename One -----------------------------------------------------------

( cd "$WORK" && mvn -B -q $MVN_REPO_ARG archetype:generate \
    -DarchetypeArtifactId=cn1app-archetype -DarchetypeGroupId=com.codenameone \
    -DarchetypeVersion="$CN1_VERSION" -DgroupId=com.example.kmpbench -DartifactId=cn1 \
    -Dversion=1.0-SNAPSHOT -DmainName=Bench -DinteractiveMode=false )
COMMON="$WORK/cn1/common"
cp "$HERE/cn1/Bench.java" "$COMMON/src/main/java/com/example/kmpbench/Bench.java"
mkdir -p "$COMMON/src/main/kotlin/com/example/kmpbench" "$COMMON/src/main/kmp/commonMain"
cp "$HERE/cn1/BenchEntry.kt" "$COMMON/src/main/kotlin/com/example/kmpbench/"
cp -R "$HERE/shared/commonMain/kotlin" "$COMMON/src/main/kmp/commonMain/kotlin"
echo "codename1.arg.ios.newStorageLocation=true" >> "$COMMON/codenameone_settings.properties"

# --- Compose Multiplatform --------------------------------------------------

cp -R "$HERE/cmp/." "$WORK/cmp/"
mkdir -p "$WORK/cmp/composeApp/src"
cp -R "$HERE/shared/commonMain" "$WORK/cmp/composeApp/src/commonMain"
echo "prepared $WORK (cn1 $CN1_VERSION)"
