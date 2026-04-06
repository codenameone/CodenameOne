#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -euo pipefail
source "$SCRIPTPATH/inc/env.sh"

WORKDIR="$SCRIPTPATH/build/initializr-roundtrip"
SOURCE_PROJECT="$WORKDIR/source"
GENERATED_ARTIFACT_ID="initializr-roundtrip-generated"
GENERATED_PROJECT="$WORKDIR/$GENERATED_ARTIFACT_ID"
APP_NAME="RoundTripInitializrApp"
PACKAGE_NAME="com.acme.initializr.roundtrip"

rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"

cd "$WORKDIR"

mvn archetype:generate \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" \
  -DartifactId=source \
  -DgroupId="$PACKAGE_NAME" \
  -Dversion=1.0-SNAPSHOT \
  -DmainName="$APP_NAME" \
  -DinteractiveMode=false

cat > "$SOURCE_PROJECT/generate-app-project.rpf" <<EOF
template.type=maven
template.mainName=$APP_NAME
template.packageName=$PACKAGE_NAME
EOF

mvn "com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-app-project" \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeVersion="${CN1_VERSION}" \
  -DartifactId="${GENERATED_ARTIFACT_ID}" \
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

IGNORES_FILE="$WORKDIR/roundtrip.ignores"
cat > "$IGNORES_FILE" <<EOF
generate-app-project.rpf
EOF

DIFF_OUT="$WORKDIR/roundtrip.diff"
set +e
diff -ruN -X "$IGNORES_FILE" "$SOURCE_PROJECT" "$GENERATED_PROJECT" > "$DIFF_OUT"
DIFF_STATUS=$?
set -e

if [ "$DIFF_STATUS" -ne 0 ]; then
  echo "Initializr round-trip comparison failed. Unexpected differences found:"
  cat "$DIFF_OUT"
  exit 1
fi

echo "Initializr round-trip comparison passed."
