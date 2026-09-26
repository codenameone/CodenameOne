#!/bin/bash
# Gate on the jar a generated project actually sends to a build.
#
# #5380 added cn1-binaries-javase to the archetype's javase module at compile
# scope. cn1:build assembles its upload from the compile classpath, so every
# desktop build of every project generated afterwards staged ~300 MB of ffmpeg
# natives (Android, iOS, Linux, macOS, Windows), and the build client refused
# the upload before it reached a server. It went unnoticed for two months: no
# check ever looked at the staged jar, the one CI step that produced one
# (cn1app-desktop-build-test.sh) only asserted on its log, and the canary builds
# javascript on an account that cannot build desktop anyway.
#
# This generates a fresh project from the archetype and stages the upload jar
# for EVERY target the project's own build.sh offers, with -Dcodename1.stageOnly
# so nothing is submitted and no account, SDK or network is needed. The target
# list is read from build.sh, not written here, so a target added there is
# covered without anyone remembering this file. Each staged jar must:
#
#   * contain no native library (.so .dylib .jnilib .dll) -- a hello world has
#     none of its own, so any native in it came from a dependency that should
#     not be on the upload; and
#   * stay under MAX_STAGED_JAR_MB (default 25 MB). The local-javascript jar is
#     the largest legitimate one because it carries core for the local
#     translator.
#
# A failure prints the jar's largest top-level packages, which names the
# dependency responsible.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh

MAX_STAGED_JAR_MB="${MAX_STAGED_JAR_MB:-25}"
APP=myappstaged

cd $SCRIPTPATH/build
if [ -d $APP ]; then
  rm -rf $APP
fi
mvn -B -ntp archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion=$CN1_VERSION \
  -DartifactId=$APP \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=MyApp \
  -DinteractiveMode=false

cd $APP

# "platform target" pairs, exactly as build.sh passes them.
PAIRS=$(grep -o -- '-Dcodename1.platform=[A-Za-z0-9_-]*" "-Dcodename1.buildTarget=[A-Za-z0-9_-]*' build.sh \
  | sed -e 's/-Dcodename1.platform=//' -e 's/" "-Dcodename1.buildTarget=/ /' | sort -u)
PAIR_COUNT=$(printf '%s\n' "$PAIRS" | grep -c . || true)
# build.sh offered 12 targets when this was written. Far fewer means the parse
# above stopped matching, and a gate that checks nothing must not pass.
if [ "$PAIR_COUNT" -lt 8 ]; then
  echo "FAIL: read only $PAIR_COUNT build targets from build.sh; the pattern no longer matches it."
  printf '%s\n' "$PAIRS"
  exit 1
fi

FAILED=0
CHECKED=0
while read -r PLATFORM TARGET; do
  [ -z "$PLATFORM" ] && continue
  LOG=$SCRIPTPATH/build/$APP-stage-$TARGET.log
  echo "== staging $TARGET (platform $PLATFORM)"
  if ! mvn -B -ntp package -DskipTests -Dopen=false \
      -Dcodename1.platform=$PLATFORM -Dcodename1.buildTarget=$TARGET \
      -Dcodename1.stageOnly=true < /dev/null > "$LOG" 2>&1; then
    echo "FAIL: staging $TARGET did not complete. Last lines of $LOG:"
    tail -40 "$LOG"
    FAILED=1
    continue
  fi
  if ! grep -q "codename1.stageOnly is set: staged" "$LOG"; then
    echo "FAIL: $TARGET never reached the staging step, so its jar was not checked. See $LOG."
    FAILED=1
    continue
  fi
  JAR=$(find . -path "*/target/*-$TARGET-jar-with-dependencies.jar" -newer build.sh | head -1)
  if [ -z "$JAR" ]; then
    echo "FAIL: no staged jar for $TARGET under */target."
    FAILED=1
    continue
  fi
  if ! python3 - "$JAR" "$TARGET" "$MAX_STAGED_JAR_MB" <<'EOF'
import os, sys, zipfile
jar, target, max_mb = sys.argv[1], sys.argv[2], float(sys.argv[3])
size = os.path.getsize(jar)
natives, by_package = [], {}
with zipfile.ZipFile(jar) as z:
    for info in z.infolist():
        name = info.filename
        if name.endswith("/"):
            continue
        base = name.rsplit("/", 1)[-1].lower()
        if base.endswith((".so", ".dylib", ".jnilib", ".dll")) or ".so." in base:
            natives.append(name)
        parts = name.split("/")
        key = "/".join(parts[:3]) if len(parts) > 3 else "/".join(parts[:-1]) or "(root)"
        by_package[key] = by_package.get(key, 0) + info.file_size
ok = True
print("   %s: %.1f MB compressed" % (target, size / 1048576.0))
if natives:
    ok = False
    print("FAIL: %s stages %d native libraries, e.g.:" % (target, len(natives)))
    for n in natives[:10]:
        print("      " + n)
if size > max_mb * 1048576:
    ok = False
    print("FAIL: %s staged jar is %.1f MB, over the %.0f MB budget." % (target, size / 1048576.0, max_mb))
if not ok:
    print("   largest contents (uncompressed):")
    for key, total in sorted(by_package.items(), key=lambda kv: -kv[1])[:12]:
        print("      %8.1f MB  %s" % (total / 1048576.0, key))
sys.exit(0 if ok else 1)
EOF
  then
    FAILED=1
  fi
  CHECKED=$((CHECKED + 1))
done <<< "$PAIRS"

# A cached staged jar from before a fix must not be reused. Projects hit by #5380
# already hold a ffmpeg-filled jar from their failed build, and a plugin update
# changes nothing on their classpath, so a timestamp-only cache would upload it
# again. Reproduce that faithfully: install the parent and common, plant a stale
# jar newer than all of them with no record of its inputs, then invoke cn1:build
# directly -- no package phase, so nothing is rebuilt and the timestamp check
# cannot be what replaces it. Only the record of the jar's inputs can.
STALE_TARGET=mac-os-x-desktop
STALE_JAR=$(find . -path "*/target/*-$STALE_TARGET-jar-with-dependencies.jar" | head -1)
if [ -z "$STALE_JAR" ]; then
  echo "FAIL: no $STALE_TARGET jar to plant a stale copy over."
  FAILED=1
elif ! mvn -B -ntp -q -pl .,common install -DskipTests -Dcodename1.platform=javase \
    < /dev/null > "$SCRIPTPATH/build/$APP-install-common.log" 2>&1; then
  echo "FAIL: could not install the parent and common for the stale-jar check. See $APP-install-common.log."
  FAILED=1
else
  # Mtime resolution is a second on some filesystems: be strictly newer than the install.
  sleep 1
  rm -f "$STALE_JAR.inputs"
  python3 - "$STALE_JAR" <<'EOF2'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1], "w") as z:
    z.writestr("org/bytedeco/ffmpeg/linux-x86_64/libavcodec.so", b"stale")
EOF2
  sleep 1
  STALE_LOG="$SCRIPTPATH/build/$APP-stage-stale.log"
  if ! mvn -B -ntp -pl javase -Dcodename1.platform=javase -Dcodename1.buildTarget=$STALE_TARGET \
      -Dcodename1.stageOnly=true cn1:build < /dev/null > "$STALE_LOG" 2>&1; then
    echo "FAIL: restaging $STALE_TARGET over a stale jar did not complete. See $STALE_LOG."
    FAILED=1
  elif python3 -c 'import sys, zipfile; sys.exit(0 if any(n.endswith(".so") for n in zipfile.ZipFile(sys.argv[1]).namelist()) else 1)' "$STALE_JAR"; then
    echo "FAIL: a stale staged jar was reused; restaging must replace a jar it did not record."
    FAILED=1
  else
    echo "   stale $STALE_TARGET jar was replaced on restaging"
  fi
fi

if [ "$FAILED" -ne 0 ]; then
  echo "FAIL: at least one target stages a jar the build would refuse or should not carry."
  exit 1
fi
echo "PASS: $CHECKED staged jars, no natives, each under $MAX_STAGED_JAR_MB MB."
