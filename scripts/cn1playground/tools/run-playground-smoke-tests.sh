#!/bin/bash
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

cd "$ROOT"
echo "Regenerating CN1 access registry from release sources..."
CN1_ACCESS_USE_LOCAL_SOURCES=false bash "$ROOT/tools/generate-cn1-access-registry.sh"

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
mvn -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundLayoutHarness
