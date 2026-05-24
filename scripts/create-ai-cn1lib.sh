#!/usr/bin/env bash
# Bootstrap a new AI cn1lib repository from cn1lib-archetype.
#
# Usage:   ./scripts/create-ai-cn1lib.sh <feature-name>
# Example: ./scripts/create-ai-cn1lib.sh mlkit-text
#
# Produces a directory `cn1-ai-<feature>` containing:
#   - pom.xml wired to the current cn1Version
#   - Platform stubs (common/ios/android/javase)
#   - .github/workflows/publish.yml (auto-publish on merge to master)
#   - README with required GitHub secrets reference
#
# The resulting directory is meant to be `git init`'d, pushed to a
# new repo under github.com/codenameone, and given the four secrets
# (MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD,
# MAVEN_GPG_PRIVATE_KEY, MAVEN_GPG_PASSPHRASE). After that, any push
# to master that lands with a -SNAPSHOT version triggers a release.

set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <feature-name>" >&2
    echo "Example: $0 mlkit-text" >&2
    exit 1
fi

FEATURE="$1"
ARTIFACT="cn1-ai-${FEATURE}"
PACKAGE_SUFFIX="${FEATURE//-/.}"      # mlkit-text -> mlkit.text
GROUP_PACKAGE="com.codename1.ai.${PACKAGE_SUFFIX}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TARGET_DIR="$(pwd)/${ARTIFACT}"

if [[ -e "${TARGET_DIR}" ]]; then
    echo "Refusing to overwrite ${TARGET_DIR}" >&2
    exit 1
fi

# Resolve the project's current cn1Version from the maven aggregator
# pom so the bootstrapped cn1lib is pinned to the same release line.
CN1_VERSION="$(grep -m1 '<cn1Version>' "${REPO_ROOT}/maven/pom.xml" | sed -e 's/.*<cn1Version>//' -e 's|</cn1Version>.*||')"
if [[ -z "${CN1_VERSION}" ]]; then
    echo "Failed to read <cn1Version> from maven/pom.xml" >&2
    exit 1
fi

echo "Bootstrapping ${ARTIFACT} (cn1Version=${CN1_VERSION}, package=${GROUP_PACKAGE})"

mvn archetype:generate \
    -DarchetypeGroupId=com.codenameone \
    -DarchetypeArtifactId=cn1lib-archetype \
    -DarchetypeVersion="${CN1_VERSION}" \
    -DgroupId=com.codenameone \
    -DartifactId="${ARTIFACT}" \
    -Dversion=0.1.0-SNAPSHOT \
    -Dpackage="${GROUP_PACKAGE}" \
    -DinteractiveMode=false \
    -DoutputDirectory="$(pwd)"

# Drop in the auto-publish workflow.
mkdir -p "${TARGET_DIR}/.github/workflows"
cat > "${TARGET_DIR}/.github/workflows/publish.yml" <<'YAML'
name: Publish cn1lib to Maven Central
on:
  push:
    branches: [master]
permissions:
  contents: write
jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'temurin'
          cache: 'maven'
          server-id: central
          server-username: MAVEN_USERNAME
          server-password: MAVEN_PASSWORD
          gpg-private-key: ${{ secrets.MAVEN_GPG_PRIVATE_KEY }}
          gpg-passphrase: MAVEN_GPG_PASSPHRASE

      - name: Detect version bump
        id: ver
        run: |
          v=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)
          if [[ "$v" == *-SNAPSHOT ]]; then
            release="${v%-SNAPSHOT}"
            echo "release=$release" >> "$GITHUB_OUTPUT"
            echo "skip=false"      >> "$GITHUB_OUTPUT"
          else
            echo "skip=true"       >> "$GITHUB_OUTPUT"
          fi

      - name: Set release version
        if: steps.ver.outputs.skip == 'false'
        run: mvn -B versions:set -DnewVersion=${{ steps.ver.outputs.release }} -DgenerateBackupPoms=false

      - name: Deploy to Maven Central
        if: steps.ver.outputs.skip == 'false'
        run: mvn -B deploy -Psign-artifacts -DskipTests
        env:
          MAVEN_USERNAME:       ${{ secrets.MAVEN_CENTRAL_USERNAME }}
          MAVEN_PASSWORD:       ${{ secrets.MAVEN_CENTRAL_PASSWORD }}
          MAVEN_GPG_PASSPHRASE: ${{ secrets.MAVEN_GPG_PASSPHRASE }}

      - name: Tag and bump to next SNAPSHOT
        if: steps.ver.outputs.skip == 'false'
        run: |
          git config user.name  "GitHub Actions Bot"
          git config user.email "github-actions@codenameone.com"
          git tag "v${{ steps.ver.outputs.release }}"
          IFS=. read -r maj min pat <<< "${{ steps.ver.outputs.release }}"
          next="${maj}.${min}.$((pat+1))-SNAPSHOT"
          mvn -B versions:set -DnewVersion="$next" -DgenerateBackupPoms=false
          git commit -am "Bump to $next"
          git push origin master --tags
YAML

# Drop in a README pointing at the required secrets and AI dep table.
cat > "${TARGET_DIR}/README.md" <<README
# ${ARTIFACT}

Codename One cn1lib for the **${FEATURE}** AI feature. Part of the
\`com.codename1.ai.*\` family.

## Usage

Add to your app's pom.xml:

\`\`\`xml
<dependency>
    <groupId>com.codenameone</groupId>
    <artifactId>${ARTIFACT}</artifactId>
    <version>0.1.0</version>
    <type>cn1lib</type>
</dependency>
\`\`\`

The Codename One build server detects use of \`${GROUP_PACKAGE}.*\`
classes during scan and injects the matching CocoaPod / Swift Package
/ Android Gradle dep + Info.plist usage strings + permissions. You do
**not** need to declare any build hints manually unless you want to
override the defaults; see \`AiDependencyTable.java\` in the Codename
One core repo for the full table.

## Releases

Push to \`master\` with a \`-SNAPSHOT\` version -> workflow strips the
suffix, publishes to Maven Central, tags \`v<release>\`, and bumps
\`pom.xml\` to the next patch \`-SNAPSHOT\`. Required GitHub secrets:

| Secret | Source |
|---|---|
| \`MAVEN_CENTRAL_USERNAME\` | Sonatype OSSRH user token |
| \`MAVEN_CENTRAL_PASSWORD\` | Sonatype OSSRH user token password |
| \`MAVEN_GPG_PRIVATE_KEY\` | ASCII-armored signing key |
| \`MAVEN_GPG_PASSPHRASE\` | Passphrase for the above |

See \`~/dev/CodenameOne/BuildCloud/SECRETS.md\` for the canonical
keypair the core repo uses.
README

echo
echo "Done. Next steps:"
echo "  cd ${ARTIFACT}"
echo "  git init && git add . && git commit -m 'Initial commit'"
echo "  gh repo create codenameone/${ARTIFACT} --public --source=. --push"
echo "  # then set the four Maven Central secrets on the new repo"
