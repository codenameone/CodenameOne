#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source $SCRIPTPATH/inc/env.sh
cd $SCRIPTPATH/build
if [ -d coderad ]; then
  rm -rf coderad
fi

curl -L https://github.com/shannah/CodeRAD/archive/refs/tags/v1.0.zip > master.zip
unzip master.zip
rm master.zip
mvn com.codenameone:codenameone-maven-plugin:${CN1_VERSION}:generate-cn1lib-project \
  -DartifactId=coderad \
  -DgroupId=com.example \
  -Dversion=1.0-SNAPSHOT \
  -DinteractiveMode=false \
  -Dcn1Version=${CN1_VERSION} \
  -DsourceProject=CodeRAD-1.0

rm -rf CodeRAD-1.0

cd coderad
mvn package


