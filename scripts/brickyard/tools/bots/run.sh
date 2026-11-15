#!/bin/bash
# Runs BrickyardBots against a local backend, with the engine jars from a
# local Maven repository (default: the checkout's .m2-repo).
#   tools/bots/run.sh --experience <id> --password <pw> leo@brickyard.example
set -e
here=$(cd "$(dirname "$0")" && pwd)
repo=${MAVEN_REPO_LOCAL:-$here/../../../../.m2-repo}
v=${CN1_VERSION:-8.0-SNAPSHOT}
cp=""
for a in codenameone-luau-runtime codenameone-rbx-compat codenameone-core; do
  jar="$repo/com/codenameone/$a/$v/$a-$v.jar"
  [ -f "$jar" ] || { echo "missing $jar: install it first (mvn -pl $a install)" >&2; exit 1; }
  cp="$cp:$jar"
done
exec java -cp "${cp#:}" "$here/BrickyardBots.java" "$@"
