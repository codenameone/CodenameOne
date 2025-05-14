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

echo "Committing version change in git"
git add -u .
# Note: the -u is to prevent adding files that aren't added to git yet.  Only changed
# files.  This is to help avoid accidents.
git commit -m "Updated version to $version"
if [[ "$version" == *-SNAPSHOT ]]; then
  echo "This is a snapshot version so not adding a tag"
else
  echo "Adding git tag for 'v${version}'"
  git tag -a "v${version}" -m "Version ${version}"
fi