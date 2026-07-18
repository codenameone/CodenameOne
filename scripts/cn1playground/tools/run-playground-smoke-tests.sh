#!/bin/bash
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

cd "$ROOT"
# The playground is pinned to the in-repo 8.0-SNAPSHOT (see pom.xml), so the
# registry is generated from the LOCAL framework sources -- the same API the
# playground builds against. Release-source mode would 404 on Central for a
# SNAPSHOT version and would describe a different API anyway.
echo "Regenerating CN1 access registry from local workspace sources..."
CN1_ACCESS_USE_LOCAL_SOURCES=true bash "$ROOT/tools/generate-cn1-access-registry.sh"

echo "Verifying Component is present in generated registry..."
if ! grep -q 'index.put("com.codename1.ui.Component"' "$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"; then
  echo "GeneratedCN1Access is missing com.codename1.ui.Component" >&2
  exit 1
fi

echo "Verifying key com.codename1.ui classes are present in generated registry..."
for cls in Button Container Dialog Display Form Label List TextField BrowserComponent; do
  if ! grep -q "index.put(\"com.codename1.ui.${cls}\"" "$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"; then
    echo "GeneratedCN1Access is missing com.codename1.ui.${cls}" >&2
    exit 1
  fi
done

echo "Verifying package-private/internal sentinel classes are NOT generated..."
for cls in com.codename1.ui.Accessor com.codename1.io.IOAccessor; do
  if grep -q "index.put(\"${cls}\"" "$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"; then
    echo "GeneratedCN1Access unexpectedly includes internal class ${cls}" >&2
    exit 1
  fi
done

mvn -pl common -am -DskipTests install
mvn -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSmokeHarness
mvn -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSyntaxMatrixHarness
# This harness checks only the native CN1 chrome. Keep its BrowserComponent as
# a placeholder instead of provisioning a full JCEF runtime during the test.
mvn -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dcn1.javase.implementation=jmf \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundLayoutHarness
mvn -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundPreviewResolutionHarness
mvn -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSamplesHarness
