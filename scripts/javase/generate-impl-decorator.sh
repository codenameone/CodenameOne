#!/bin/bash
# Regenerates Ports/JavaSE/src/com/codename1/impl/CodenameOneImplementationDecorator.java
# from the current com.codename1.impl.CodenameOneImplementation API.
#
# Run this whenever CodenameOneImplementation gains, loses or changes a method.
# DecoratorCoverageTest in maven/javase fails the build when the generated file
# is out of sync.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

CORE_CLASSES="$REPO_ROOT/maven/core/target/classes"
if [ ! -d "$CORE_CLASSES" ]; then
    CORE_CLASSES="$HOME/.m2/repository/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"
fi
if [ ! -e "$CORE_CLASSES" ]; then
    echo "Cannot find compiled core. Build it first: cd maven && mvn install -pl core -DskipTests" >&2
    exit 1
fi

GENERATOR_SRC="$REPO_ROOT/Ports/JavaSE/src/com/codename1/impl/javase/tools/ImplementationDecoratorGenerator.java"
OUTPUT="$REPO_ROOT/Ports/JavaSE/src/com/codename1/impl/CodenameOneImplementationDecorator.java"

BUILD_DIR="$(mktemp -d)"
trap 'rm -rf "$BUILD_DIR"' EXIT

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

"$JAVAC" -nowarn -cp "$CORE_CLASSES" -d "$BUILD_DIR" "$GENERATOR_SRC"
"$JAVA" -cp "$BUILD_DIR:$CORE_CLASSES" com.codename1.impl.javase.tools.ImplementationDecoratorGenerator "$OUTPUT"
