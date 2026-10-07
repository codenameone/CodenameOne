#!/usr/bin/env bash
# Run against an imported Gradle app after its first successful build.
set -eo pipefail
SCRIPTPATH=$(cd "$(dirname "$0")" && pwd)
source "$SCRIPTPATH/inc/env.sh"
source "$SCRIPTPATH/inc/gradle.sh"
APP=$(cd "${1:?Pass the imported Gradle project directory}" && pwd)
FIXTURE="$APP/src/main/java/cn1incremental"
[ ! -e "$FIXTURE" ] || fail "incremental fixture already exists: $FIXTURE"
mkdir -p "$FIXTURE"
trap 'rm -rf "$FIXTURE"' EXIT
cat > "$FIXTURE/Api.java" <<'JAVA'
package cn1incremental;
public class Api {
    public static int accept(android.view.View view) { return view == null ? 0 : 1; }
}
JAVA
cat > "$FIXTURE/Caller.java" <<'JAVA'
package cn1incremental;
public class Caller {
    public static int call(android.view.View view) { return Api.accept(view) + 1; }
}
JAVA
run_gradle "$APP" classes --refresh-dependencies > "$APP/incremental-first.log" 2>&1 \
  || { tail -60 "$APP/incremental-first.log"; fail "initial Android API fixture build"; }
# Edit only the caller's body. The unchanged Api.class now contains relocated
# descriptors; incremental javac must never compile against those descriptors.
python3 - "$FIXTURE/Caller.java" <<'PY'
import pathlib, sys
p = pathlib.Path(sys.argv[1])
p.write_text(p.read_text().replace('+ 1', '+ 2'))
PY
run_gradle "$APP" classes > "$APP/incremental-edit.log" 2>&1 \
  || { tail -60 "$APP/incremental-edit.log"; fail "Java-only edit after Android relocation"; }
run_gradle "$APP" classes > "$APP/incremental-unchanged.log" 2>&1 \
  || { tail -60 "$APP/incremental-unchanged.log"; fail "unchanged Android build"; }
grep -q ':compileJava UP-TO-DATE' "$APP/incremental-unchanged.log" \
  || fail "unchanged Java sources should remain up to date"
# The repaired edit must still package relocated bytecode.
"$GRADLE_JDK/bin/javap" -p -classpath "$APP/build/classes/java/main" cn1incremental.Api \
  > "$APP/incremental-api.txt"
grep -q 'com.codename1.androidcompat.android.view.View' "$APP/incremental-api.txt" \
  || fail "edited build left the API unrelocated"
echo "android-gradle-incremental-test: OK (edit rebuilds, unchanged stays up to date, output relocated)"
