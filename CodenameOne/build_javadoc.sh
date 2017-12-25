#/bin/sh

rm -Rf dist/javadoc
rm -Rf build/tempJavaSources
/Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/bin/java -jar ~/dev/java/JavaDocSourceEmbed/target/JavaDocSourceEmbed-1.0-SNAPSHOT.jar src build/tempJavaSources

/usr/bin/find build/tempJavaSources ../Ports/CLDC11/src -name "*.java" | /usr/bin/grep -v /impl/ | /usr/bin/xargs /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/bin/javadoc -protected -d dist/javadoc -windowtitle "Codename One API"
