#!/bin/sh

rm -Rf dist/javadoc
rm -Rf build/tempJavaSources
java -jar ~/dev/java/JavaDocSourceEmbed/target/JavaDocSourceEmbed-1.0-SNAPSHOT.jar src build/tempJavaSources

/usr/bin/find build/tempJavaSources ../Ports/CLDC11/src -name "*.java" | /usr/bin/grep -v /impl/ | /usr/bin/xargs javadoc --allow-script-in-comments -protected -d dist/javadoc -windowtitle "Codename One API"
