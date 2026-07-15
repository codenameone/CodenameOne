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
for cls in Button Container Dialog Display Form Label List TextField BrowserComponent CodeEditor RichTextArea; do
  if ! grep -q "index.put(\"com.codename1.ui.${cls}\"" "$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"; then
    echo "GeneratedCN1Access is missing com.codename1.ui.${cls}" >&2
    exit 1
  fi
done

echo "Verifying new editor APIs are available to playground scripts..."
for member in 'setLightweightEditingEnabled(boolean)' 'setContent(String, RichTextFormat)' \
              'setMarkdown(String)' 'setAsciiDoc(String)' 'setRtf(String)'; do
  if ! grep -q "$member" "$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"; then
    echo "GeneratedCN1Access is missing editor API ${member}" >&2
    exit 1
  fi
done

echo "Verifying the playground uses CodeEditor for its source panes..."
PLAYGROUND_EDITOR="$ROOT/common/src/main/java/com/codenameone/playground/PlaygroundCodeEditor.java"
if ! grep -q 'new CodeEditor' "$PLAYGROUND_EDITOR"; then
  echo "Playground source pane is not backed by CodeEditor" >&2
  exit 1
fi
for integration in 'setShowLineNumbers(true)' 'setDiagnostics(converted)' 'setTheme('; do
  if ! grep -q "$integration" "$PLAYGROUND_EDITOR"; then
    echo "Playground CodeEditor integration is missing ${integration}" >&2
    exit 1
  fi
done
if grep -q 'new TextArea(this.source' "$PLAYGROUND_EDITOR"; then
  echo "Playground source pane regressed to a generic TextArea" >&2
  exit 1
fi
if grep -q 'setEngineURL' "$PLAYGROUND_EDITOR"; then
  echo "Playground source pane installs a custom browser editor engine" >&2
  exit 1
fi

forbidden_editor='mona''co'
if git -C "$ROOT/../.." grep -in "$forbidden_editor" -- \
    ':!docs/website/content/blog/funding-open-source-without-the-bait-and-switch.md' \
    ':!docs/website/content/blog/java-one-detailed-trip-report.md' \
    ':!docs/website/content/blog/rich-text-and-code-editing.md'; then
  echo "Removed browser-editor dependency is still referenced by tracked files" >&2
  exit 1
fi

echo "Verifying package-private/internal sentinel classes are NOT generated..."
for cls in com.codename1.ui.Accessor com.codename1.io.IOAccessor; do
  if grep -q "index.put(\"${cls}\"" "$ROOT/common/src/main/java/bsh/cn1/GeneratedCN1Access.java"; then
    echo "GeneratedCN1Access unexpectedly includes internal class ${cls}" >&2
    exit 1
  fi
done

# These checks intentionally exercise the locally-installed framework SNAPSHOT.
# Do not let Maven replace it with the latest remote SNAPSHOT between compilation
# and the harness runs.
mvn -nsu -pl common -am -DskipTests install
mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSmokeHarness
mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSyntaxMatrixHarness
mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundLayoutHarness
mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundPreviewResolutionHarness
mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSamplesHarness
