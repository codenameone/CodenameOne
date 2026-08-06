#!/bin/bash
# Builds the GUI Builder against the current working tree and launches it.
#
# The verification steps are not paranoia. `mvn install` reported BUILD SUCCESS several times while
# leaving a stale jar in the local repository, so the editor ran without changes that had just been
# compiled and the same defect was "fixed" repeatedly without ever reaching the running application.
# Every stage here asserts that the artifact it just installed actually contains the source it was
# built from, and stops if it does not.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOCAL_REPO="${CN1_LOCAL_REPO:-/tmp/cn1-local-repo}"
JDK8="${JAVA8_HOME:-$(/usr/libexec/java_home -v 1.8)}"
JDK21="${JAVA21_HOME:-$(/usr/libexec/java_home -v 21)}"
CORE_JAR="$LOCAL_REPO/com/codenameone/codenameone-core/8.0-SNAPSHOT/codenameone-core-8.0-SNAPSHOT.jar"

run_tests=1
launch=1
for arg in "$@"; do
  case "$arg" in
    --no-tests) run_tests=0 ;;
    --no-launch) launch=0 ;;
    *) echo "usage: $0 [--no-tests] [--no-launch]" >&2; exit 2 ;;
  esac
done

# Asserts a class inside the installed core jar contains a symbol the working tree defines. javap on
# the installed artifact is the only check that cannot be satisfied by a stale build.
verify_core_class() {
  local class_path="$1" symbol="$2"
  local dir extracted
  dir="$(mktemp -d)"
  extracted="$dir/$(basename "$class_path")"
  unzip -o -p "$CORE_JAR" "$class_path" > "$extracted"
  if ! javap -p "$extracted" | grep -q "$symbol"; then
    echo "STALE ARTIFACT: $CORE_JAR is missing '$symbol' from $class_path." >&2
    echo "The install reported success but packaged an older class. Re-run with a clean target." >&2
    rm -rf "$dir"
    exit 1
  fi
  rm -rf "$dir"
}

echo "==> core (JDK 8)"
cd "$REPO_ROOT/maven"
# mvn clean is not enough: the core module has produced a jar mixing freshly compiled classes with
# stale ones, which is how fixes kept reaching the tests but not the running editor. Remove the
# output directory outright.
rm -rf core/target factory/target css-compiler/target
JAVA_HOME="$JDK8" PATH="$JDK8/bin:$PATH" \
  mvn -q -Dmaven.repo.local="$LOCAL_REPO" -pl factory,core,css-compiler \
    -DskipTests -Dmaven.javadoc.skip=true clean install
JAVA_HOME="$JDK8" verify_core_class "com/codename1/ui/editor/CodeView.class" "protectedStartMarker"
JAVA_HOME="$JDK8" verify_core_class "com/codename1/ui/editor/EditorView.class" "multiKeyModeRestore"
JAVA_HOME="$JDK8" verify_core_class "com/codename1/ui/CodeEditor.class" "setCursorPosition"
JAVA_HOME="$JDK8" verify_core_class "com/codename1/ui/Form.class" "focusedHandlesInput"

echo "==> javase port (JDK 8)"
JAVA_HOME="$JDK8" PATH="$JDK8/bin:$PATH" \
  mvn -q -Dcn1.binaries="$REPO_ROOT/maven/target/cn1-binaries" -Dmaven.repo.local="$LOCAL_REPO" \
    -pl javase -DskipTests -Dmaven.javadoc.skip=true clean install

cd "$REPO_ROOT/scripts/guibuilder"
if [ "$run_tests" = "1" ]; then
  echo "==> tests (JDK 21)"
  JAVA_HOME="$JDK21" PATH="$JDK21/bin:$PATH" \
    mvn -nsu -Dmaven.repo.local="$LOCAL_REPO" -pl javase -am test -Dcodename1.platform=javase
fi

echo "==> package (JDK 21)"
JAVA_HOME="$JDK21" PATH="$JDK21/bin:$PATH" \
  mvn -nsu -q -Dmaven.repo.local="$LOCAL_REPO" -pl javase -am -Pexecutable-jar package \
    -Dcodename1.platform=javase -Dmaven.test.skip=true

if [ "$launch" = "0" ]; then
  echo "Built: scripts/guibuilder/javase/target/codenameone-guibuilder-8.0-SNAPSHOT.jar"
  exit 0
fi

# The binding holds absolute paths, so it is generated rather than tracked.
cat > demo-project/guibuilder.input <<EOF
projectDir=$PWD/demo-project
guiDir=$PWD/demo-project/src/main/guibuilder
sourceDir=$PWD/demo-project/src/main/java
cssFile=$PWD/demo-project/src/main/css/theme.css
initialForm=com.example.NestedLayoutsForm
EOF

pkill -f "codenameone-guibuilder-8.0-SNAPSHOT.jar" 2>/dev/null || true
sleep 1
echo "==> launching"
nohup "$JDK21/bin/java" \
  -Dguibuilder.input="$PWD/demo-project/guibuilder.input" \
  -Dapple.awt.application.name="Codename One GUI Builder" \
  -Dsun.awt.application.name="Codename One GUI Builder" \
  -Xdock:name="Codename One GUI Builder" \
  --add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED \
  --add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED \
  ${CN1_EXTRA_ARGS:-} \
  -jar javase/target/codenameone-guibuilder-8.0-SNAPSHOT.jar > /tmp/guibuilder.log 2>&1 &
sleep 12
if pgrep -f "codenameone-guibuilder-8.0-SNAPSHOT.jar" > /dev/null; then
  echo "GUI Builder running; log at /tmp/guibuilder.log"
else
  echo "GUI Builder failed to start:" >&2
  tail -20 /tmp/guibuilder.log >&2
  exit 1
fi
