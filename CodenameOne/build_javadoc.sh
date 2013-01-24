#/bin/sh

/usr/bin/find src ../../CodenameOne/CLDC11/src -name "*.java" | /usr/bin/grep -v /impl/ | /usr/bin/xargs /Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home/bin/javadoc -protected -d dist/javadoc -windowtitle "Codename One API"
