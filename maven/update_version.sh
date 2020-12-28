#!/bin/bash
set -e
if [ -z "$1" ]; then
	echo "No version supplied"
	exit 1
fi
mvn versions:set -DnewVersion=$1
cd factory
mvn versions:set -DnewVersion=$1
cd ../core
mvn versions:set -DnewVersion=$1
cd ../javase
mvn versions:set -DnewVersion=$1
cd ../codenameone-maven-plugin
mvn versions:set -DnewVersion=$1