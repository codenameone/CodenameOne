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
python3 - "$WORK/flutter/pubspec.yaml" "$WORK/asset-packages.txt" <<'PY_INNER'
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
# Every package the asset paths reference, for the staging check further down.
# newline="\n": Windows Python would otherwise write CRLF, and the shell below
# would read each name with a trailing carriage return.
io.open(sys.argv[2], "w", encoding="utf-8", newline="\n").write("\n".join(sorted(referenced)) + "\n")
PY_INNER
# The asset packages the gallery references; staging checks each one landed.
# CRs stripped as well: on the Windows leg a package name read with a trailing
# \r named a directory that does not exist, and the staging check below failed
# for a package that was in the bundle all along.
DECLARED_ASSET_PACKAGES="$(tr -d '\r' < "$WORK/asset-packages.txt" | tr '\n' ' ')"

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
  # help:evaluate prints the value itself. The exec:exec it replaces ran
  # `echo` as a process, and Windows has no echo executable -- it is a cmd
  # builtin -- so the Windows leg failed right here, and because stderr was
  # discarded it said nothing but an exit code. Maven's stderr stays visible.
  CN1_VERSION="$(cd "$REPO/maven" && mvn -q -B $MVN_REPO_ARG help:evaluate \
      -Dexpression=project.version -DforceStdout --non-recursive | tr -d '\r' | tail -1)"
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

# Assets: exactly what Flutter's own build bundles, for EVERY asset package.
#
# `flutter build bundle` writes the asset bundle to build/flutter_assets, the
# same on every platform, and stage_from_flutter_bundle copies what is under
# its packages/ -- so the Codename One build ships the artwork Flutter ships,
# 2x and 3x variants included, and installed size compares like with like.
#
# This used to stage the flutter_gallery_assets package from the pub cache and
# nothing else. The gallery also draws from rally_assets and shrine_images, so
# those screens rendered blank on the Codename One side and the app it measured
# was smaller than Flutter's by that artwork -- a flattering size comparison of
# two different applications. It also contradicted the README, which says the
# assets come from Flutter's bundle.
( cd "$WORK/flutter" && flutter build bundle --release -t lib/main_bench.dart >/dev/null )
BUNDLE="$WORK/flutter/build/flutter_assets"
[ -d "$BUNDLE/packages" ] || {
  echo "flutter build bundle produced no packages/ under $BUNDLE; the Codename One" >&2
  echo "build would render without artwork and its size would not be comparable." >&2
  exit 2; }
# Fatal, not a warning, for any package the gallery declares that did not land:
# a build missing one is a smaller build, and would be measured as a win.
for pkg in $DECLARED_ASSET_PACKAGES; do
  [ -d "$BUNDLE/packages/$pkg" ] || {
    echo "the Flutter bundle is missing the declared asset package $pkg" >&2; exit 2; }
done
python3 "$HERE/stage_assets.py" --from-flutter-bundle "$BUNDLE" "$COMMON/src/main/resources"

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
