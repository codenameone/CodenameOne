#!/bin/bash
# Gate on the jar a generated project actually sends to a build.
#
# #5380 added cn1-binaries-javase to the archetype's javase module at compile
# scope. cn1:build assembles its upload from the compile classpath, so every
# desktop build of every project generated afterwards staged ~157 MB of ffmpeg
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
# covered without anyone remembering this file.
#
# Each staged jar must contain EXACTLY what the build is supposed to send:
#
#   * every entry of the application's common jar -- nothing of the app is
#     missing; and
#   * nothing that did not come from a jar this project built itself (its
#     module jars, and the strings / app extension jars cn1:build generates
#     from the project's own directories). The one exception is by design:
#     local-javascript translates on this machine, so it also carries
#     codenameone-core and java-runtime, and nothing else.
#
# Anything else is a dependency leaking into the upload, whatever its size and
# whether or not it holds natives, and the failure names it.
#
# The targets are staged in three passes:
#
#   generated  the project exactly as the archetype produces it;
#   legacy     rewritten to the pom #5380 shipped (cn1-binaries-javase with no
#              scope), the shape of every project generated since -- the plugin
#              alone has to repair those, without a pom edit;
#   nested     legacy plus a library in common that brings the aggregator in
#              transitively, so it reaches targets that never declared it.
#
# Before the legacy and nested passes the gate proves, from dependency:tree, that
# ffmpeg really is at compile scope in the modules it checks. A pass whose pom
# change did not take effect would otherwise check nothing and pass.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh

APP=myappstaged
LOGDIR=$SCRIPTPATH/build

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
PROJECT_DIR=$(pwd -P)

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

# check_staged_jar LABEL TARGET JAR STAGE_LOG
check_staged_jar() {
  python3 - "$1" "$2" "$3" "$4" "$PROJECT_DIR" <<'EOF'
import os, re, sys, zipfile
label, target, jar, log, project = sys.argv[1:6]

def entries(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: i.file_size for i in z.infolist() if not i.filename.endswith("/")}

# Jars this project built itself. Staged jars and test jars are not inputs.
own = []
for root, dirs, files in os.walk(project):
    for f in files:
        if f.endswith(".jar") and "/target" in root \
                and not f.endswith("-jar-with-dependencies.jar") and not f.endswith("-tests.jar"):
            own.append(os.path.join(root, f))
common = [p for p in own if re.search(r"/common/target/[^/]*-common-[^/]*\.jar$", p)]
if len(common) != 1:
    print("FAIL: %s %s: expected exactly one common jar, found %s" % (label, target, common))
    sys.exit(1)

expected = set()
for p in own:
    expected |= set(entries(p))

# local-javascript bundles core and java-runtime by design. Take them from the
# mojo's own log line, but only if it names exactly those two artifacts.
if target == "local-javascript":
    bundled = re.findall(r"Adding local-javascript dependency to jar-with-dependencies: (\S+)", open(log).read())
    names = sorted(re.sub(r"-[0-9][^/]*\.jar$", "", os.path.basename(b)) for b in bundled)
    if names != ["codenameone-core", "java-runtime"]:
        print("FAIL: %s %s bundles %s; local-javascript must bundle exactly codenameone-core and java-runtime"
              % (label, target, [os.path.basename(b) for b in bundled]))
        sys.exit(1)
    for b in bundled:
        expected |= set(entries(b))

staged = entries(jar)
extra = sorted(set(staged) - expected)
missing = sorted(set(entries(common[0])) - set(staged))
size = os.path.getsize(jar)
print("   %s %s: %d entries, %.1f KB" % (label, target, len(staged), size / 1024.0))
ok = True
if missing:
    ok = False
    print("FAIL: %s %s is missing %d entries of the application's common jar, e.g.:" % (label, target, len(missing)))
    for m in missing[:10]:
        print("      " + m)
if extra:
    ok = False
    total = sum(staged[e] for e in extra)
    print("FAIL: %s %s carries %d entries (%.1f MB uncompressed) that no jar of this project produced:"
          % (label, target, len(extra), total / 1048576.0))
    groups = {}
    for e in extra:
        parts = e.split("/")
        key = "/".join(parts[:3]) if len(parts) > 3 else "/".join(parts[:-1]) or e
        groups[key] = groups.get(key, 0) + staged[e]
    for key, n in sorted(groups.items(), key=lambda kv: -kv[1])[:15]:
        print("      %10.1f KB  %s" % (n / 1024.0, key))
sys.exit(0 if ok else 1)
EOF
}

# stage_all LABEL -- stage and check every target
stage_all() {
  local label=$1 checked=0
  while read -r PLATFORM TARGET; do
    [ -z "$PLATFORM" ] && continue
    local log=$LOGDIR/$APP-$label-stage-$TARGET.log
    echo "== [$label] staging $TARGET (platform $PLATFORM)"
    if ! mvn -B -ntp package -DskipTests -Dopen=false \
        -Dcodename1.platform=$PLATFORM -Dcodename1.buildTarget=$TARGET \
        -Dcodename1.stageOnly=true < /dev/null > "$log" 2>&1; then
      echo "FAIL: [$label] staging $TARGET did not complete. Last lines of $log:"
      tail -40 "$log"
      FAILED=1
      continue
    fi
    # The mojo names the jar it staged; take it from there rather than guessing
    # (a glob for "javascript" also matches the local-javascript jar).
    local jar
    jar=$(sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' "$log" | tail -1)
    if [ -z "$jar" ] || [ ! -f "$jar" ]; then
      echo "FAIL: [$label] $TARGET never reported a staged jar, so nothing was checked. See $log."
      FAILED=1
      continue
    fi
    case "$jar" in
      *-"$TARGET"-jar-with-dependencies.jar) ;;
      *) echo "FAIL: [$label] $TARGET staged $jar, which is not named for that target."; FAILED=1; continue ;;
    esac
    if ! check_staged_jar "$label" "$TARGET" "$jar" "$log"; then
      FAILED=1
    fi
    checked=$((checked + 1))
  done <<< "$PAIRS"
  if [ "$checked" -ne "$PAIR_COUNT" ]; then
    echo "FAIL: [$label] checked $checked of $PAIR_COUNT targets."
    FAILED=1
  fi
}

stage_all generated

# A cached staged jar from before a fix must not be reused. Projects hit by #5380
# already hold a ffmpeg-filled jar from their failed build, and a plugin update
# changes nothing on their classpath, so a timestamp-only cache would upload it
# again. Reproduce that faithfully: install the parent and common, plant a stale
# jar newer than all of them with no record of its inputs, then invoke cn1:build
# directly -- no package phase, so nothing is rebuilt and the timestamp check
# cannot be what replaces it. Only the record of the jar's inputs can.
STALE_TARGET=mac-os-x-desktop
STALE_JAR=$(sed -n 's/.*codename1.stageOnly is set: staged \(.*\) ([0-9]* bytes) for .*/\1/p' \
  "$LOGDIR/$APP-generated-stage-$STALE_TARGET.log" | tail -1)
if [ -z "$STALE_JAR" ] || [ ! -f "$STALE_JAR" ]; then
  echo "FAIL: no $STALE_TARGET jar to plant a stale copy over."
  FAILED=1
elif ! mvn -B -ntp -q -pl .,common install -DskipTests -Dcodename1.platform=javase \
    < /dev/null > "$LOGDIR/$APP-install-common.log" 2>&1; then
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
  STALE_LOG="$LOGDIR/$APP-stage-stale.log"
  if ! mvn -B -ntp -pl javase -Dcodename1.platform=javase -Dcodename1.buildTarget=$STALE_TARGET \
      -Dcodename1.stageOnly=true cn1:build < /dev/null > "$STALE_LOG" 2>&1; then
    echo "FAIL: restaging $STALE_TARGET over a stale jar did not complete. See $STALE_LOG."
    FAILED=1
  elif python3 -c 'import sys, zipfile; sys.exit(0 if "org/bytedeco/ffmpeg/linux-x86_64/libavcodec.so" in zipfile.ZipFile(sys.argv[1]).namelist() else 1)' "$STALE_JAR"; then
    echo "FAIL: a stale staged jar was reused; restaging must replace a jar it did not record."
    FAILED=1
  elif ! check_staged_jar stale "$STALE_TARGET" "$STALE_JAR" "$STALE_LOG"; then
    FAILED=1
  fi
fi

# require_compile_ffmpeg LABEL PLATFORM MODULE -- the pass is not vacuous: ffmpeg
# is on MODULE's compile classpath, so only the plugin keeps it out of the upload.
require_compile_ffmpeg() {
  if ! mvn -B -ntp dependency:tree -pl common,$3 -Dcodename1.platform=$2 < /dev/null 2>/dev/null \
      | sed -n "/Building .*-$3 /,\$p" | grep -q "org.bytedeco:ffmpeg-platform:jar:.*:compile"; then
    echo "FAIL: [$1] ffmpeg-platform is not at compile scope in $3, so this pass would check nothing."
    exit 1
  fi
}

# legacy: undo the archetype fix, leaving the pom #5380 generated.
python3 - pom.xml javase/pom.xml <<'EOF3'
import re, sys
for path in sys.argv[1:]:
    s = open(path).read()
    s2, n = re.subn(r"(<artifactId>cn1-binaries-javase</artifactId>(?:\s*<version>[^<]*</version>)?\s*<type>pom</type>)\s*<scope>runtime</scope>",
                    r"\1", s)
    assert n == 1, "%s: expected one runtime-scoped cn1-binaries-javase, found %d" % (path, n)
    open(path, "w").write(s2)
EOF3
require_compile_ffmpeg legacy javase javase
stage_all legacy

# nested: a library the application depends on -- a cn1lib, say -- that itself
# depends on cn1-binaries-javase must not drag ffmpeg into any target's upload.
# The library is a pom with no classes of its own, so whatever it contributes to
# a staged jar is leakage.
NESTED_GROUP=com.example.stagedjargate
NESTED_ARTIFACT=nested-desktop-runtime
NESTED_POM=$LOGDIR/$NESTED_ARTIFACT.pom
cat > "$NESTED_POM" <<EOF3
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$NESTED_GROUP</groupId>
  <artifactId>$NESTED_ARTIFACT</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>
  <dependencies>
    <dependency>
      <groupId>com.codenameone</groupId>
      <artifactId>cn1-binaries-javase</artifactId>
      <version>$CN1_VERSION</version>
      <type>pom</type>
    </dependency>
  </dependencies>
</project>
EOF3
if ! mvn -B -ntp -q install:install-file -Dfile="$NESTED_POM" -DpomFile="$NESTED_POM" -Dpackaging=pom \
    < /dev/null > "$LOGDIR/$APP-install-nested.log" 2>&1; then
  echo "FAIL: could not install the nested-aggregator library. See $APP-install-nested.log."
  exit 1
fi
python3 - common/pom.xml "$NESTED_GROUP" "$NESTED_ARTIFACT" <<'EOF4'
import sys
path, group, artifact = sys.argv[1:4]
s = open(path).read()
anchor = "<dependencies>"
assert s.count(anchor) >= 1, "no <dependencies> in common/pom.xml"
dep = ("<dependencies>\n        <dependency>\n            <groupId>%s</groupId>\n"
       "            <artifactId>%s</artifactId>\n            <version>1.0</version>\n"
       "            <type>pom</type>\n        </dependency>" % (group, artifact))
open(path, "w").write(s.replace(anchor, dep, 1))
EOF4
# iOS never declares the aggregator: ffmpeg reaches it only through the library.
require_compile_ffmpeg nested ios ios
stage_all nested

if [ "$FAILED" -ne 0 ]; then
  echo "FAIL: at least one target stages something other than what it should send."
  exit 1
fi
echo "PASS: every target stages exactly the application (plus core and java-runtime for local-javascript),"
echo "      as generated, on the #5380 pom, and with the desktop runtime reached transitively;"
echo "      and a stale jar is not reused."
