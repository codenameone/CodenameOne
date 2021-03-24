#!/bin/bash
# deploy-to-sonatype.sh
# Created by Steve Hannah, March 24, 2021
#
# Synopsis:
# --------
#
# Deploys project to Maven central.  You should run update-version.sh before and after
# running this to set the release version, and then create a new SNAPSHOT version.
#
#
# Typical Workflow is:
# bash update-version 8.0.1 && bash deploy-to-sonatype.sh && bash update-version 8.0.2-SNAPSHOT
set -e
version=$(bash print-version.sh)
if [[ "$version" == *-SNAPSHOT ]]; then
  echo "This is a snapshot version so not deploying"
else
  echo "Deploying version ${version} to sonatype staging"
  MAVEN_ARGS="-e"
  if [ ! -z $MAVEN_GPG_PASSPHRASE ]; then
    MAVEN_ARGS="-Dgpg.passphrase='$MAVEN_GPG_PASSPHRASE' -e"
  fi
  export GPG_TTY=$(tty)
  mvn deploy -Psign-artifacts $MAVEN_ARGS
fi
