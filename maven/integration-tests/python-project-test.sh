#!/bin/bash
# A Python project (scripts/python-samples/*) is imported into an archetype
# application and built for every target, by Maven and by Gradle. Checks, for
# each sample:
#
#   - cn1:import-python-project copies the .py sources into
#     common/src/main/python, records the entry module in
#     codenameone_settings.properties and writes a main class that runs it;
#   - the build transpiled them (the generated module classes, the registry
#     and, for a program that imports Java classes, the JavaInterop bridge)
#     and compiled them with the application;
#   - every target's upload jar (staged, never sent) carries the transpiled
#     classes and the Python runtime, so the iOS, Android, JavaScript and
#     desktop builders all get them;
#   - the Gradle build uploads the same entries as the Maven build.
#
# Needs the reactor installed (mvn install, with -Pflutter-modules for the
# python modules) and a JDK 17+; see inc/gradle.sh.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"
source "$SCRIPTPATH/inc/compat.sh"
SAMPLES="$SCRIPTPATH/../../scripts/python-samples"

WORKDIR="$SCRIPTPATH/build/python-project"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

FAILED=0

check_python_upload() {
  local jar="$1"
  assert_zip_has "$jar" '(^|/)com/codename1/generated/python/PythonModules\.class$'
  assert_zip_has "$jar" '(^|/)com/codename1/python/runtime/Py\.class$'
}

for sample_dir in "$SAMPLES"/*/; do
  sample=$(basename "$sample_dir")
  echo "== $sample"
  W="$WORKDIR/$sample"
  mkdir -p "$W"
  PKG=com.acme.py
  compat_generate_app "$sample" "$W" $PKG PyApp
  MAPP="$W/app"
  (cd "$MAPP" && mvn_local "com.codenameone:codenameone-maven-plugin:$CN1_VERSION:import-python-project" \
      "-Dcn1.python.import=$sample_dir") > "$W/import.log" 2>&1 \
    || { tail -40 "$W/import.log"; fail "$sample: import-python-project"; }
  ls "$MAPP"/common/src/main/python/*.py > /dev/null 2>&1 || fail "$sample: the import copied no Python sources"
  grep -q '^codename1.python.mainModule=' "$MAPP/common/codenameone_settings.properties" \
    || fail "$sample: the import recorded no entry module"
  grep -q 'PythonModules.runMain()' "$MAPP/common/src/main/java/com/acme/py/PyApp.java" \
    || fail "$sample: the main class does not run the Python program"

  compat_build_common "$sample" "$W" "$MAPP"
  classes="$MAPP/common/target/classes"
  [ -e "$classes/com/codename1/generated/python/PythonModules.class" ] \
    || fail "$sample: the build produced no PythonModules"
  if grep -rqE '^from java\.' "$sample_dir" --include='*.py'; then
    [ -e "$classes/com/codename1/generated/python/JavaInterop.class" ] \
      || fail "$sample: a program importing Java classes produced no JavaInterop bridge"
  fi

  compat_convert_to_gradle "$sample" "$W" "$MAPP"
  GAPP="$W/app-gradle"
  ls "$GAPP"/src/main/python/*.py > /dev/null 2>&1 || fail "$sample: the Gradle conversion dropped src/main/python"

  compat_stage_all "$sample" "$W" "$MAPP" "$GAPP" check_python_upload
done

[ $FAILED -eq 0 ] || exit 1
echo "python-project-test: OK"
