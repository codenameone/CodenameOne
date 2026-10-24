#!/usr/bin/env bash
set -euo pipefail

extract_version() {
  local raw="$1"
  local parsed=""
  raw="${raw#refs/tags/}"
  if [[ "$raw" =~ ^v?([0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*)$ ]]; then
    parsed="${BASH_REMATCH[1]}"
  elif [[ "$raw" =~ ([0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z]+)*) ]]; then
    parsed="${BASH_REMATCH[1]}"
  fi
  echo "$parsed"
}

latest_plugin_version_from_maven() {
  local metadata
  metadata="$(curl -fsSL https://repo1.maven.org/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml || true)"
  if [ -z "$metadata" ]; then
    return 1
  fi
  local version
  version="$(printf '%s' "$metadata" | perl -ne 'if (m|<release>([^<]+)</release>|) { print $1; exit 0 }')"
  if [ -z "$version" ]; then
    version="$(printf '%s' "$metadata" | perl -ne 'if (m|<latest>([^<]+)</latest>|) { print $1; exit 0 }')"
  fi
  if [ -n "$version" ]; then
    echo "$version"
    return 0
  fi
  return 1
}

RAW_VERSION="${1:-}"
VERSION=""
if [ -n "$RAW_VERSION" ]; then
  # An explicit tag/argument is authoritative. Maven Central metadata
  # can lag behind a just-pushed release, which used to leave the
  # initializr pinned one version behind whenever the workflow fired
  # before Central had indexed the new artifact.
  VERSION="$(extract_version "$RAW_VERSION")"
  if [ -z "$VERSION" ]; then
    echo "Invalid Codename One version/tag: $RAW_VERSION" >&2
    exit 1
  fi
else
  VERSION="$(latest_plugin_version_from_maven || true)"
fi

if [ -z "$VERSION" ]; then
  echo "Usage: $0 [<cn1-version-or-tag>]" >&2
  echo "Unable to determine Codename One plugin version from argument or Maven Central metadata." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

replace_file() {
  local file="$1"
  local expr="$2"
  perl -0pi -e "$expr" "$file"
}

# What a version written out literally looks like. Not [^<]+: in Java source that
# also matches an expression, so the bump rewrote the matrix test's
# "<cn1.plugin.version>" + version + "</cn1.plugin.version>" into a literal and
# broke the check of a download generated at another version. Likewise it keeps a
# ${property} reference in a pom from being flattened.
VERSION_LITERAL='[0-9][0-9A-Za-z.-]*'

ROOT_INITIALIZR_DIR="$ROOT_DIR/scripts/initializr"
GENERATOR_MODEL="$ROOT_DIR/scripts/initializr/common/src/main/java/com/codename1/initializr/model/GeneratorModel.java"
MATRIX_TEST="$ROOT_DIR/scripts/initializr/common/src/test/java/com/codename1/initializr/model/GeneratorModelMatrixTest.java"
while IFS= read -r pom; do
  replace_file "$pom" "s|<cn1\\.plugin\\.version>$VERSION_LITERAL</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"
  replace_file "$pom" "s|<cn1\\.version>$VERSION_LITERAL</cn1\\.version>|<cn1.version>$VERSION</cn1.version>|g;"
done < <(find "$ROOT_INITIALIZR_DIR" -name '*pom.xml' -not -path '*/target/*' -type f)
# *pom.xml, not pom.xml: the poms the generator ships as resources are named for
# what they are (backend-only-pom.xml), and a bump that matched only pom.xml left
# the backend-only one on 8.0-SNAPSHOT.

# The root pom every Maven download starts from lives inside common.zip. The
# generator rewrites both properties on the way out, so a stale value here never
# reached a download -- but it is what anyone reading the template sees, and it
# had fallen 30 releases behind. Rewritten in place: entry order, names and
# timestamps are kept, so the only difference is that one file.
python3 - "$ROOT_INITIALIZR_DIR/common/src/main/resources/common.zip" "$VERSION" <<'PY'
import re, shutil, sys, tempfile, zipfile
path, version = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(path) as src:
    infos = src.infolist()
    data = {i.filename: src.read(i.filename) for i in infos}
pom = data['pom.xml'].decode('utf-8')
for tag in ('cn1.plugin.version', 'cn1.version'):
    pom, n = re.subn(r'<%s>[0-9][0-9A-Za-z.-]*</%s>' % (re.escape(tag), re.escape(tag)),
                     '<%s>%s</%s>' % (tag, version, tag), pom)
    if n != 1:
        sys.exit('common.zip pom.xml: expected one <%s>, found %d' % (tag, n))
if pom.encode('utf-8') == data['pom.xml']:
    sys.exit(0)  # already at this version: leave the archive's bytes alone
data['pom.xml'] = pom.encode('utf-8')
tmp = tempfile.NamedTemporaryFile(delete=False, suffix='.zip')
tmp.close()
with zipfile.ZipFile(tmp.name, 'w') as out:
    for info in infos:
        out.writestr(info, data[info.filename])
shutil.move(tmp.name, path)
PY

# The cn1-local-workspace profile (scripts/initializr/pom.xml) is built against the
# locally bootstrapped repo HEAD (the current SNAPSHOT), NOT the pinned release, so
# the website CI can demo unreleased API. The global bump above is release-only, so
# restore that profile's versions to the repo SNAPSHOT here -- a version bump must
# never pin it to the release (it broke the website build when it last did).
SNAPSHOT_VERSION="$(perl -0ne 'print "$1\n" and last if m{<artifactId>codenameone</artifactId>\s*<version>([^<]+)</version>}s' "$ROOT_DIR/maven/pom.xml" 2>/dev/null | head -n1)"
SNAPSHOT_VERSION="${SNAPSHOT_VERSION:-8.0-SNAPSHOT}"
replace_file "$ROOT_INITIALIZR_DIR/pom.xml" "s|(<id>cn1-local-workspace</id>.*?<cn1\\.version>)[^<]+(</cn1\\.version>)|\${1}$SNAPSHOT_VERSION\${2}|s;"
replace_file "$ROOT_INITIALIZR_DIR/pom.xml" "s|(<id>cn1-local-workspace</id>.*?<cn1\\.plugin\\.version>)[^<]+(</cn1\\.plugin\\.version>)|\${1}$SNAPSHOT_VERSION\${2}|s;"

replace_file "$GENERATOR_MODEL" "s|private static final String CN1_PLUGIN_VERSION = \\\"[^\\\"]+\\\";|private static final String CN1_PLUGIN_VERSION = \\\"$VERSION\\\";|g;"
# perl's s||| reports nothing when it matches nothing, and the version this
# constant holds decides which Maven layouts a download gets (MAVEN_LAYOUTS_SINCE),
# so a rename that left it behind must stop the bump rather than ship stale.
if ! grep -qF "private static final String CN1_PLUGIN_VERSION = \"$VERSION\";" "$GENERATOR_MODEL"; then
  echo "CN1_PLUGIN_VERSION in $GENERATOR_MODEL was not updated to $VERSION" >&2
  exit 1
fi

# Gradle downloads resolve the Gradle plugin at this same version, so the
# initializr offers Gradle only when its marker is published there. Checked, not
# assumed: the first releases carry no Gradle plugin at all.
GRADLE_MARKER="https://repo.codenameone.com/maven2/com/codenameone/com.codenameone.gradle.plugin/$VERSION/com.codenameone.gradle.plugin-$VERSION.pom"
if curl -fsSIL -o /dev/null "$GRADLE_MARKER"; then
  GRADLE_PUBLISHED=true
else
  GRADLE_PUBLISHED=false
fi
replace_file "$GENERATOR_MODEL" "s#static final boolean GRADLE_PLUGIN_PUBLISHED = (?:true|false);#static final boolean GRADLE_PLUGIN_PUBLISHED = $GRADLE_PUBLISHED;#g;"
echo "Gradle plugin published at $VERSION: $GRADLE_PUBLISHED"
replace_file "$MATRIX_TEST" "s|<cn1\\.plugin\\.version>$VERSION_LITERAL</cn1\\.plugin\\.version>|<cn1.plugin.version>$VERSION</cn1.plugin.version>|g;"
replace_file "$MATRIX_TEST" "s|<cn1\\.version>$VERSION_LITERAL</cn1\\.version>|<cn1.version>$VERSION</cn1.version>|g;"


echo "Updated Initializr Codename One versions to $VERSION"
