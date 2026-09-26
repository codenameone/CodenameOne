#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
if [ -z $CN1_VERSION ]; then
  CN1_VERSION=$(bash $SCRIPTPATH/../maven/print-version.sh)
fi

if [ ! -d $SCRIPTPATH/build ]; then
  mkdir $SCRIPTPATH/build
fi

# The javaVersion to generate archetype projects with, derived from the running
# JDK. This suite runs on JDK 8 on some legs and the archetype now defaults to
# 17, so a generated project would target a release this JDK cannot produce.
if [ -z "${CN1_ARCHETYPE_JAVA_VERSION:-}" ]; then
  CN1_ARCHETYPE_JAVA_VERSION="$(bash "$SCRIPTPATH/../scripts/ci/archetype-java-version.sh")"
fi
export CN1_ARCHETYPE_JAVA_VERSION
