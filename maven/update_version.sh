#!/bin/bash
set -e
if [ -z "$1" ]; then
	echo "No version supplied"
	exit 1
fi
currdir=$(pwd)
for d in android codenameone-maven-plugin core designer factory ios java-runtime javase javase-svg maven-archetypes parparvm tests
do
    cd "$d"
    mvn versions:set -DnewVersion=$1
    cd "$currdir"
done
mvn versions:set -DnewVersion=$1