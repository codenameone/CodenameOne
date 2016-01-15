#/bin/sh

/usr/bin/find src ../../CodenameOne/CLDC11/src -name "*.java" | /usr/bin/grep -v /impl/ | /usr/bin/xargs /Library/Java/JavaVirtualMachines/jdk1.8.0_45.jdk/Contents/Home/bin/javadoc -protected -d dist/javadoc -windowtitle "Codename One API"
