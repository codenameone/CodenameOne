---
title: Open Source cn1libs
slug: open-source-cn1libs
url: /blog/open-source-cn1libs/
original_url: https://www.codenameone.com/blog/open-source-cn1libs.html
aliases:
- /blog/open-source-cn1libs.html
date: '2016-08-02'
author: Shai Almog
---

![Header Image](/blog/open-source-cn1libs/extensions.png)

When we initially designed the cn1lib file format we looked at jar files as a starting point. We wanted a way to distribute binary libraries that support the native code access, restrictions and ideally code completion.

One of the big failures of Jar files as a standard is bad support for code completion. This requires IDE’s to specify a JavaDoc (or source) directory so the completion logic can refer to that for hints. However, we wanted to keep the “binary” aspects of jars…​

Our solution was simple, we used a special “doclet” to generate stub files representing the source files in the cn1lib thus providing a source classpath that included all the javadoc comments. Since these sources are stubs you can distribute binary cn1libs without too much of an IP concern (no more than typical Java jars).

At this time, all (or almost all?) cn1libs are open source. This makes that effort somewhat redundant for those libraries where the advantage of peeking at the source code can be preserved…​

You can do this by editing the cn1lib’s `build.xml` file and modifying the stubs target which should look like this:
    
    
    <target name="Stubs">
        <delete dir="build/stubs"/>
        <javadoc sourcepath="src"
            classpath="lib/CodenameOne.jar:lib/CLDC11.jar"
            docletpath="Stubber.jar"
            doclet="com.codename1.build.client.StubGenerator">
            <fileset dir="${src.dir}" excludes="*.java,${excludes}" includes="${includes}">
                <filename name="**/*.java"/>
            </fileset>
         </javadoc>
    </target>

To look like this, it’s essentially a copy task instead of the doclet code:
    
    
    <target name="Stubs">
        <delete dir="build/stubs"/>
         <mkdir dir="build/stubs" />
         <copy todir="build/stubs">
             <fileset dir="${src.dir}">
                <filename name="**/*.java"/>
             </fileset>
         </copy>
    </target>

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
