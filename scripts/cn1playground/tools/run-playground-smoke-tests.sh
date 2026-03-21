#!/bin/zsh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

cd "$ROOT"
mvn -pl common -DskipTests test-compile org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSmokeHarness
