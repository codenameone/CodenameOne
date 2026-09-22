#!/usr/bin/env bash
# Materialises BOTH sides of the Flutter benchmark into a work directory.
#
#   scripts/flutter-bench/app/prepare.sh --work /tmp/fbench
#
# The point of this script is that the two sides are built from ONE source
# tree. The gallery is not vendored into this repository; it is taken from the
# Flutter SDK that is already installed, at
# $FLUTTER_ROOT/dev/integration_tests/new_gallery. That has three consequences
# worth stating, because each is a property the benchmark depends on:
#
#   * The comparison cannot drift. Both sides copy the same 159 files, so no
#     edit can land on one side and not the other. An earlier round of this
#     work compared two DIFFERENT galleries -- the SDK one against the
#     standalone GitHub one, 237 files with materially different widget code --
#     and every visual score quoted from it was meaningless.
#   * The reference is pinned to the SDK version, which is recorded in the
#     result so a number can be traced to what produced it.
#   * Nothing third-party enters this repository. What IS committed here is the
#     two entry points, which are ours: cn1/Bench.java and
#     flutter/main_bench.dart.
#
# Outputs:
#   <work>/flutter/   a Flutter project, gallery + platform scaffolding
#   <work>/cn1/       a Codename One project generated from the archetype
#   <work>/prepared.json
set -Eeuo pipefail
# Report WHERE a failure happened. This script drives Maven, Flutter, pub and
# python, several of which can fail with no output at all -- the Windows leg
# twice reported an exit code and nothing else, and the first guess about
# which command produced it was wrong. -E so the trap is inherited by the
# subshells the build steps run in.
trap 'rc=$?; echo "prepare.sh: FAILED at line $LINENO (exit $rc)" >&2' ERR

WORK=""
FLUTTER_ROOT_ARG=""
CN1_VERSION="${CN1_VERSION:-}"
# Concurrent checkouts of this repository share ~/.m2 and overwrite each
# other's snapshots, so a developer machine must point this at a per-checkout
# repository. CI has one checkout and can leave it unset.
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-}"

while [ $# -gt 0 ]; do
  case "$1" in
    --work) WORK="$2"; shift 2 ;;
    --flutter-root) FLUTTER_ROOT_ARG="$2"; shift 2 ;;
    --cn1-version) CN1_VERSION="$2"; shift 2 ;;
    --maven-repo) MAVEN_REPO_LOCAL="$2"; shift 2 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

[ -n "$WORK" ] || { echo "usage: prepare.sh --work <dir>" >&2; exit 2; }
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"

# --- the Flutter SDK, and the gallery inside it -----------------------------

if [ -n "$FLUTTER_ROOT_ARG" ]; then
  FLUTTER_ROOT="$FLUTTER_ROOT_ARG"
elif [ -n "${FLUTTER_ROOT:-}" ]; then
  FLUTTER_ROOT="$FLUTTER_ROOT"
else
  command -v flutter >/dev/null 2>&1 || {
    echo "flutter is not on PATH; pass --flutter-root" >&2; exit 2; }
  # Resolve symlinks first. A package-manager install puts a link in
  # /opt/homebrew/bin, and taking its parent's parent lands on /opt/homebrew
  # rather than on the SDK -- which then fails further down complaining that
  # the SDK has no gallery in it, pointing at a directory that was never one.
  FLUTTER_BIN="$(python3 -c 'import os,shutil,sys; print(os.path.realpath(shutil.which("flutter")))')"
  FLUTTER_ROOT="$(cd "$(dirname "$FLUTTER_BIN")/.." && pwd)"
fi

GALLERY="$FLUTTER_ROOT/dev/integration_tests/new_gallery"
[ -d "$GALLERY/lib" ] || {
  echo "the gallery is not in this Flutter SDK: $GALLERY" >&2
  echo "the benchmark needs a full SDK checkout, not a stripped release" >&2
  exit 2; }

FLUTTER_VERSION="$(cd "$FLUTTER_ROOT" && git rev-parse --short HEAD 2>/dev/null || echo unknown)"
DART_FILES="$(find "$GALLERY/lib" -name '*.dart' | wc -l | tr -d ' ')"
echo "==> gallery: $DART_FILES Dart files from Flutter $FLUTTER_VERSION"

MVN_REPO_ARG=""
[ -n "$MAVEN_REPO_LOCAL" ] && MVN_REPO_ARG="-Dmaven.repo.local=$MAVEN_REPO_LOCAL"

mkdir -p "$WORK"

# --- the Flutter side -------------------------------------------------------

echo "==> preparing the Flutter build"
rm -rf "$WORK/flutter"
mkdir -p "$WORK/flutter"
cp -R "$GALLERY/lib" "$WORK/flutter/lib"
cp "$GALLERY/pubspec.yaml" "$WORK/flutter/pubspec.yaml"
cp "$FLUTTER_ROOT/pubspec.lock" "$WORK/flutter/pubspec.lock"

# The gallery is a MEMBER of the Flutter SDK's pub workspace, so its pubspec
# carries `resolution: workspace` and refuses to resolve anywhere else:
# "found no workspace root including it in parent directories". Lifting the
# package out of the SDK means dropping that line.
#
# And then taking the workspace's LOCKFILE with it, which is not optional. The
# gallery pins almost nothing -- `google_fonts: any` -- so the only thing
# holding its dependencies at the versions the Flutter team tests is the
# workspace root's pubspec.lock. Without it pub resolved google_fonts 8.1.0
# instead of 6.2.1, and the build failed on
# "Member not found: 'GoogleFonts.robotoCondensed'" in three of the studies:
# the benchmark would have been comparing against a gallery that does not
# build, on a dependency nobody chose.
python3 - "$WORK/flutter/pubspec.yaml" <<'PY_INNER'
import io, re, sys
path = sys.argv[1]
lines = [line for line in io.open(path, encoding="utf-8").read().splitlines()
         if line.strip() != "resolution: workspace"]

# The gallery's ASSET packages are not in its own pubspec: the workspace root
# declares them once for every member, so a package lifted out of the workspace
# loses them and the build stops at "Could not resolve package for asset
# packages/rally_assets/logo.png" -- an error about an asset, for a dependency
# that is simply absent.
#
# Derived from the asset paths rather than hard-coded. The gallery references
# three today (flutter_gallery_assets, rally_assets, shrine_images); a list
# written out here would be wrong the first time it gained a fourth, and would
# fail in the same indirect way.
referenced = set()
for line in lines:
    match = re.match(r"\s*-\s+packages/([A-Za-z0-9_]+)/", line)
    if match:
        referenced.add(match.group(1))

declared = set()
in_deps = False
for line in lines:
    if re.match(r"^[a-z_]+:", line):
        in_deps = line.startswith("dependencies:")
        continue
    if in_deps:
        match = re.match(r"\s{2}([A-Za-z0-9_]+):", line)
        if match:
            declared.add(match.group(1))

missing = sorted(referenced - declared)
if missing:
    out = []
    for line in lines:
        out.append(line)
        if line.rstrip() == "dependencies:":
            # Unpinned: the workspace lockfile copied beside this file is what
            # decides the versions.
            for name in missing:
                out.append("  %s: any" % name)
    lines = out
    sys.stderr.write("    declared asset packages: %s\n" % ", ".join(missing))

io.open(path, "w", encoding="utf-8").write("\n".join(lines) + "\n")
PY_INNER

# Platform scaffolding is GENERATED rather than committed: `flutter create` on
# an existing project adds the ios/android/macos/linux/windows/web directories
# and leaves lib/ alone. Committing them would pin us to the scaffolding of
# whichever SDK generated them once.
( cd "$WORK/flutter" && flutter create --project-name gallery --org com.codenameone.bench . >/dev/null )

# Our own entry point, which prints the start-up markers the harness times
# against. The gallery's own main.dart is left in place and unused.
cp "$HERE/flutter/main_bench.dart" "$WORK/flutter/lib/main_bench.dart"

( cd "$WORK/flutter" && flutter pub get >/dev/null )

# --- the Codename One side --------------------------------------------------

echo "==> preparing the Codename One build"
rm -rf "$WORK/cn1"
if [ -z "$CN1_VERSION" ]; then
  CN1_VERSION="$(cd "$REPO/maven" && mvn -q $MVN_REPO_ARG -Dexec.executable=echo \
      -Dexec.args='${project.version}' --non-recursive exec:exec 2>/dev/null | tail -1)"
fi
[ -n "$CN1_VERSION" ] || { echo "could not determine the Codename One version" >&2; exit 2; }
echo "    archetype version $CN1_VERSION"

# Generated from the SHIPPING archetype rather than from a project skeleton
# kept here. That is deliberate: it means the benchmark builds the same way a
# user's project does, and a change that breaks the documented Flutter wiring
# breaks the benchmark too instead of passing unnoticed.
# NOT -q. A generation that fails under -q prints nothing at all, which is how
# the Windows leg reported an exit code and no reason for it; a build step that
# cannot say why it failed is worse than a noisy one.
echo "    maven repository ${MAVEN_REPO_LOCAL:-<default>}"
( cd "$WORK" && mvn -B $MVN_REPO_ARG archetype:generate \
    -DarchetypeArtifactId=cn1app-archetype \
    -DarchetypeGroupId=com.codenameone \
    -DarchetypeVersion="$CN1_VERSION" \
    -DartifactId=cn1 \
    -DgroupId=com.example.bench \
    -Dversion=1.0-SNAPSHOT \
    -DmainName=Bench \
    -DinteractiveMode=false )

COMMON="$WORK/cn1/common"

# The archetype ships the runtime dependency commented out; this is the one
# edit that turns a stock project into a Flutter-capable one, and it is exactly
# what the developer guide tells a user to do.
python3 - "$COMMON/pom.xml" <<'PY'
import io, re, sys
path = sys.argv[1]
text = io.open(path, encoding="utf-8").read()
block = re.search(
    r"<!--\s*\n(\s*<dependency>\s*\n\s*<groupId>com\.codenameone</groupId>\s*\n"
    r"\s*<artifactId>codenameone-flutter-runtime</artifactId>.*?</dependency>)\s*\n\s*-->",
    text, re.S)
if block:
    text = text[:block.start()] + block.group(1) + text[block.end():]
elif "codenameone-flutter-runtime" not in text:
    raise SystemExit("the archetype no longer offers the flutter runtime dependency")
io.open(path, "w", encoding="utf-8").write(text)
PY
grep -q "codenameone-flutter-runtime" "$COMMON/pom.xml" || {
  echo "failed to enable the flutter runtime dependency" >&2; exit 2; }

# The SAME 159 files the Flutter side is building.
mkdir -p "$COMMON/src/main/flutter"
cp -R "$GALLERY/lib/." "$COMMON/src/main/flutter/"

# Our entry point REPLACES the generated one in place, rather than being added
# beside it in a package of its own. The archetype also generates a desktop
# stub (javase/src/desktop/java/.../BenchStub.java) that names the main class,
# so moving the class breaks a file the archetype owns. That is why the project
# is generated into com.example.bench above: the generated layout then already
# matches the package our Bench.java declares.
GENERATED_MAIN="$COMMON/src/main/java/com/example/bench/Bench.java"
[ -f "$GENERATED_MAIN" ] || {
  echo "the archetype did not generate a main class at $GENERATED_MAIN" >&2
  exit 2; }
cp "$HERE/cn1/Bench.java" "$GENERATED_MAIN"

# Assets. Asked of pub's own answer rather than guessed from a cache path:
# `flutter pub get` above wrote .dart_tool/package_config.json, which names
# where every resolved package actually lives, on every platform.
#
# The guess it replaces was "$PUB_CACHE, or ~/.pub-cache" -- the default on
# Linux and macOS only. Windows puts the cache under LOCALAPPDATA, so find
# searched a directory that does not exist and exited non-zero; with its stderr
# sent to /dev/null and pipefail on, the script died on this assignment having
# printed nothing whatsoever. That is a silent failure twice over, so the path
# is no longer guessed and no longer silenced.
ASSETS="$(python3 - "$WORK/flutter/.dart_tool/package_config.json" <<'PY_ASSETS'
import io, json, os, re, sys
try:
    cfg = json.load(io.open(sys.argv[1], encoding="utf-8"))
except (IOError, OSError, ValueError):
    sys.exit(0)
for pkg in cfg.get("packages", []):
    if pkg.get("name") != "flutter_gallery_assets":
        continue
    uri = pkg.get("rootUri", "")
    if uri.startswith("file:"):
        # Parsed here rather than through url2pathname, which is a DIFFERENT
        # function per platform: the POSIX build returns "/C:/Users/..." for a
        # Windows file URI, leading slash and all, so testing this on a Mac
        # would have proved nothing about the platform it is for.
        try:
            from urllib.parse import urlparse, unquote
        except ImportError:
            from urlparse import urlparse
            from urllib import unquote
        path = unquote(urlparse(uri).path)
        if re.match(r"^/[A-Za-z]:", path):
            path = path[1:]
    else:
        # Relative entries are relative to the .dart_tool directory itself.
        path = os.path.join(os.path.dirname(os.path.abspath(sys.argv[1])), uri)
    # Forward slashes so the surrounding shell can test the path on Windows too.
    print(os.path.normpath(path).replace("\\", "/"))
    break
PY_ASSETS
)"

# Fatal, not a warning. A build with no artwork is a SMALLER build, so letting
# it through would report an installed size flattering to Codename One and not
# comparable with Flutter's -- the exact class of quietly-unfair number the rest
# of this harness exists to avoid.
[ -n "$ASSETS" ] && [ -d "$ASSETS/lib" ] || {
  echo "flutter_gallery_assets was not resolved (looked in" >&2
  echo "$WORK/flutter/.dart_tool/package_config.json); the Codename One build" >&2
  echo "would render without artwork and its size would not be comparable." >&2
  exit 2; }
python3 "$HERE/stage_assets.py" "$ASSETS/lib" "$COMMON/src/main/resources"

cat > "$WORK/prepared.json" <<JSON
{
  "flutter_root": "$FLUTTER_ROOT",
  "flutter_version": "$FLUTTER_VERSION",
  "gallery_dart_files": $DART_FILES,
  "cn1_version": "$CN1_VERSION",
  "flutter_project": "$WORK/flutter",
  "cn1_project": "$WORK/cn1"
}
JSON

echo "==> prepared"
echo "    flutter: $WORK/flutter"
echo "    cn1:     $WORK/cn1"
