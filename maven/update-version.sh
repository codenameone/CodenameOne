#!/bin/bash
# update-version.sh
# Created by Steve Hannah.  March 24, 2021
#
# Synopsis:
# ---------
#
# Updates the version of this project and commits the version changes in Git.  If the version
# is a release version (i.e. not ending in '-SNAPSHOT', then it will also add a git tag).
#
# Typically you would run this before and after running the deploy-to-sonatype.sh script.  Before
# to set the release version, and after to change to a new SNAPSHOT version.
#
#
# Usage:
#  bash update-version.sh VERSION
#
# Arguments:
#  VERSION    The version number to update to.
#
# Examples:
#  bash update-version.sh 8.0.1
#     Updates to version 8.0.1
#
#  bash update-version.sh 8.0.2-SNAPSHOT
#     Updates to version 8.0.2-SNAPSHOT
#
# Typical Workflow is:
# bash update-version.sh 8.0.1 && bash deploy-to-sonatype.sh && bash update-version.sh 8.0.2-SNAPSHOT
set -e
if [ -z $1 ]; then
  echo "Usage bash update-version.sh VERSION"
  echo "  Where VERSION is the version number to update to."
  echo "  E.g. bash update-version.sh 7.0.15-SNAPSHOT"
  exit 1
fi
version=$1
oldVersion=$(bash print-version.sh)
if [ $version == $oldVersion ]; then
  echo "Version is same as old version. Not updating version"
  exit 0
fi
echo "Version: $version"
mvn versions:set -DnewVersion=$version
mvn versions:commit

# Archetype metadata and IT fixtures: mvn versions:set does not reach inside
# the archetype-resources templates or the IT archetype.properties files, so
# bump the embedded version references by hand. Mirrors what the historical
# cn1-maven-archetypes/update-version.sh did before the archetypes were
# inlined into this repo.
for a in cn1app-archetype cn1lib-archetype; do
  f=./$a/src/main/resources/META-INF/maven/archetype-metadata.xml
  [ -f "$f" ] && perl -pi -e "s/<defaultValue>$oldVersion<\/defaultValue>/<defaultValue>$version<\/defaultValue>/g" "$f"
  # cn1app-archetype ships basic + netbeans ITs; cn1lib-archetype only ships
  # basic. Skip missing fixtures rather than tripping perl with -i.
  for proj in basic netbeans; do
    f=./$a/src/test/resources/projects/$proj/archetype.properties
    if [ -f "$f" ]; then
      perl -pi -e "s/^cn1Version=$oldVersion\$/cn1Version=$version/g" "$f"
      perl -pi -e "s/^cn1PluginVersion=$oldVersion\$/cn1PluginVersion=$version/g" "$f"
    fi
  done
done

# Game Builder editor (scripts/gamebuilder) lives outside the maven/ reactor
# with its own coordinates, so the mvn versions:set above does not reach it.
# Rewrite its module version and the cn1.version/cn1.plugin.version it builds
# against (kept in lock-step with the reactor) so the published
# com.codenameone:codenameone-gamebuilder matches the plugin version that the
# cn1:gamebuilder goal resolves from Maven Central. Done with perl rather than
# versions:set because at release time the old SNAPSHOT plugin it references is
# not yet resolvable in a clean .m2, which would make versions:set fail.
for f in ../scripts/gamebuilder/pom.xml \
         ../scripts/gamebuilder/common/pom.xml \
         ../scripts/gamebuilder/javase/pom.xml; do
  [ -f "$f" ] && perl -pi -e "s{<version>\Q$oldVersion\E</version>}{<version>$version</version>}g" "$f"
done
gbParent=../scripts/gamebuilder/pom.xml
if [ -f "$gbParent" ]; then
  perl -pi -e "s{<cn1\.version>\Q$oldVersion\E</cn1\.version>}{<cn1.version>$version</cn1.version>}g" "$gbParent"
  perl -pi -e "s{<cn1\.plugin\.version>\Q$oldVersion\E</cn1\.plugin\.version>}{<cn1.plugin.version>$version</cn1.plugin.version>}g" "$gbParent"
fi

echo "Committing version change in git"
git add -u .
# Note: the -u is to prevent adding files that aren't added to git yet.  Only changed
# files.  This is to help avoid accidents.
git add -u ../scripts/gamebuilder
git commit -m "Updated version to $version"
if [[ "$version" == *-SNAPSHOT ]]; then
  echo "This is a snapshot version so not adding a tag"
else
  echo "Adding git tag for 'v${version}'"
  git tag -a "v${version}" -m "Version ${version}"
fi