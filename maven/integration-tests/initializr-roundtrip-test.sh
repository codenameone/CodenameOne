#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"

WORKDIR="$SCRIPTPATH/build/initializr-roundtrip"
SOURCE_WORKDIR="$WORKDIR/source-work"
GENERATED_WORKDIR="$WORKDIR/generated-work"
ARTIFACT_ID="initializr-roundtrip"
SOURCE_PROJECT="$SOURCE_WORKDIR/$ARTIFACT_ID"
GENERATED_PROJECT="$GENERATED_WORKDIR/$ARTIFACT_ID"
APP_NAME="RoundTripInitializrApp"
PACKAGE_NAME="com.acme.initializr.roundtrip"

rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

mkdir -p "$SOURCE_WORKDIR" "$GENERATED_WORKDIR"

cd "$SOURCE_WORKDIR"

mvn archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" \
  -DartifactId="$ARTIFACT_ID" \
  -DgroupId="$PACKAGE_NAME" \
  -Dversion=1.0-SNAPSHOT \
  -DmainName="$APP_NAME" \
  -DinteractiveMode=false

cat > "$SOURCE_PROJECT/generate-app-project.rpf" <<EOF
template.type=maven
template.mainName=$APP_NAME
template.packageName=$PACKAGE_NAME
EOF

cd "$GENERATED_WORKDIR"
mvn "com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project" \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion="${CN1_VERSION}" \
  -DartifactId="${ARTIFACT_ID}" \
  -DgroupId="${PACKAGE_NAME}" \
  -Dversion=1.0-SNAPSHOT \
  -DmainName="${APP_NAME}" \
  -DpackageName="${PACKAGE_NAME}" \
  -DinteractiveMode=false \
  -DsourceProject="$SOURCE_PROJECT"

if [ ! -d "$GENERATED_PROJECT" ]; then
  echo "Generated project missing at $GENERATED_PROJECT"
  exit 1
fi

normalize_project() {
  PROJECT_DIR="$1"
  while read -r SETTINGS_FILE; do
    NORMALIZED_FILE="$SETTINGS_FILE.normalized"
    grep -v '^[[:space:]]*#' "$SETTINGS_FILE" | sort > "$NORMALIZED_FILE"
    if [ ! -s "$NORMALIZED_FILE" ]; then
      echo "Normalization produced an empty codenameone_settings.properties file: $SETTINGS_FILE (expected non-comment property entries)."
      rm -f "$NORMALIZED_FILE"
      exit 1
    fi
    mv "$NORMALIZED_FILE" "$SETTINGS_FILE"
  done < <(find "$PROJECT_DIR" -type f -name "codenameone_settings.properties")
}

normalize_project "$SOURCE_PROJECT"
normalize_project "$GENERATED_PROJECT"

IGNORES_FILE="$WORKDIR/roundtrip.ignores"
cat > "$IGNORES_FILE" <<EOF
generate-app-project.rpf
EOF

DIFF_OUT="$WORKDIR/roundtrip.diff"
set +e
diff -ruN -x target -X "$IGNORES_FILE" "$SOURCE_PROJECT" "$GENERATED_PROJECT" > "$DIFF_OUT"
DIFF_STATUS=$?
set -e

if [ "$DIFF_STATUS" -ne 0 ]; then
  echo "Initializr round-trip comparison failed. Unexpected differences found:"
  cat "$DIFF_OUT"
  exit 1
fi

echo "Initializr round-trip comparison passed."
