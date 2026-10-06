#!/bin/bash
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

cd "$ROOT"
# The API the playground compiles user code against (key classes, editor APIs,
# internal classes kept out of completion) is checked by PlaygroundSmokeHarness
# against the stub library the build generates from the local framework.

echo "Verifying the playground uses CodeEditor for its source panes..."
PLAYGROUND_EDITOR="$ROOT/common/src/main/java/com/codenameone/playground/PlaygroundCodeEditor.java"
if ! grep -q 'new CodeEditor' "$PLAYGROUND_EDITOR"; then
  echo "Playground source pane is not backed by CodeEditor" >&2
  exit 1
fi
for integration in 'setShowLineNumbers(true)' 'setDiagnostics(' 'setTheme('; do
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

# Scope the tripwire to the subsystems the old browser editor lived in; a repo-wide
# grep would fail this job for unrelated legitimate uses (the forbidden name is also
# a macOS monospace font that may appear in docs, skins or font lists). The name is
# split below so this script never matches its own tripwire.
forbidden_editor='mona''co'

# The playground's own tree is small and controlled, so the bare name is forbidden
# there outright.
if git -C "$ROOT/../.." grep -in "$forbidden_editor" -- 'scripts/cn1playground'; then
  echo "Removed browser-editor dependency is still referenced by playground files" >&2
  exit 1
fi

# CodenameOne/src is four thousand files of framework source, and the bare name over
# it is not a tripwire -- it is a guarantee of an eventual false positive, because the
# name is also a country and a font. It already fired: PhoneNumberField's dialing-code
# table lists the principality, and this job went red on a pull request that had
# touched neither the editor nor that file.
#
# So over core the name counts only where it is shaped like a DEPENDENCY -- adjacent
# to '-', '.' or '/'. That is every way the editor is actually referenced (the npm
# package name, the JS namespace before .editor.create, the loader path under /min/vs)
# and none of the ways the word occurs in prose, in a dialing-code table, or in a font
# stack that also names Menlo.
#
# The residual is a bare-word reference from core alone, which the setEngineURL check
# above already covers for the file that would carry it.
#
# Note the workflow only triggers on scripts/cn1playground changes, so this core scan
# runs only alongside a playground change. That is why it went two and a half weeks
# without noticing the table above.
# No \b here, deliberately: `git grep -E` honours neither \b nor \< on macOS (measured
# on Apple Git 2.54 -- `\bpublic\b` matches zero lines in a file with thirty), so a
# word-boundary pattern would be a gate that passes on a developer machine because it
# matches NOTHING and only really runs on Linux CI.
if git -C "$ROOT/../.." grep -inE "[-./]${forbidden_editor}|${forbidden_editor}[-./]" \
    -- 'CodenameOne/src'; then
  echo "Removed browser-editor dependency is still referenced by core files" >&2
  exit 1
fi

# These checks intentionally exercise the locally-installed framework SNAPSHOT.
# Do not let Maven replace it with the latest remote SNAPSHOT between compilation
# and the harness runs.
mvn -nsu -pl common -am -DskipTests install

# Each harness runs inside Maven's own JVM (exec:java). A harness that hangs -- an EDT
# deadlock under Xvfb, say -- would otherwise hold the job until GitHub's six-hour limit
# with no log at all. Past the limit the JVM gets SIGQUIT first, which prints every
# thread's stack into the log, and is killed a minute later.
run_harness() {
  local limit="${PLAYGROUND_HARNESS_TIMEOUT:-900}"
  # Mvn execs java, so this pid is the JVM itself.
  "$@" &
  local pid=$!
  (
    waited=0
    while kill -0 "$pid" 2>/dev/null; do
      if [ "$waited" -ge "$limit" ]; then
        echo "Harness still running after ${limit}s; asking the JVM for a thread dump." >&2
        kill -QUIT "$pid" 2>/dev/null || true
        sleep 60
        kill -9 "$pid" 2>/dev/null || true
        exit 0
      fi
      sleep 5
      waited=$((waited + 5))
    done
  ) &
  local watchdog=$!
  local rc=0
  wait "$pid" || rc=$?
  kill "$watchdog" 2>/dev/null || true
  wait "$watchdog" 2>/dev/null || true
  return "$rc"
}

run_harness mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSmokeHarness
run_harness mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSyntaxMatrixHarness
# This harness checks only the native CN1 chrome. Keep its BrowserComponent as
# a placeholder instead of provisioning a full JCEF runtime during the test.
run_harness mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dcn1.javase.implementation=jmf \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundLayoutHarness
run_harness mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundPreviewResolutionHarness
run_harness mvn -nsu -f common/pom.xml -DskipTests org.codehaus.mojo:exec-maven-plugin:3.0.0:java \
  -Dexec.classpathScope=test \
  -Dexec.mainClass=com.codenameone.playground.PlaygroundSamplesHarness
