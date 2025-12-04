#!/usr/bin/env bash
set -euo pipefail

# Quick smoke test for the core-unittests module. Runs a single fast test with
# the same flags the CI workflow uses, but skips the global clean to keep the
# feedback loop short.
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Match the CI JDK to avoid JaCoCo instrumentation issues.
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$REPO_ROOT/maven"

mvn -pl core-unittests -am \
  -DunitTests=true \
  -Dmaven.javadoc.skip=true \
  -Dtest=ButtonGroupTest \
  -DfailIfNoTests=false \
  -Plocal-dev-javase \
  test
