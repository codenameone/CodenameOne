---
title: 'TIP: Nest cn1libs Cross Dependencies'
slug: tip-nest-cn1libs-cross-dependencies
url: /blog/tip-nest-cn1libs-cross-dependencies/
original_url: https://www.codenameone.com/blog/tip-nest-cn1libs-cross-dependencies.html
aliases:
- /blog/tip-nest-cn1libs-cross-dependencies.html
date: '2017-01-08'
author: Shai Almog
---

![Header Image](/blog/tip-nest-cn1libs-cross-dependencies/just-the-tip.jpg)

On occasion developers ask us for cn1lib dependencies, e.g. allowing one cn1lib to use functionality in another cn1lib. This isn’t something we rolled into the cn1lib infrastructure because we strongly believe in simplicity. Dependency management solutions become fragile once nesting sets in and often mask over-engineering which is really dangerous for a tool that depends on small footprint.

A cn1lib is really just an ant project that packages the results into a zip containing nested zips with the data relevant to all the platforms. To add another cn1lib lib to the classpath first you need to unzip that cn1lib.

The `main.zip` file from the cn1lib will include the classes you need to compile. Now we can just add that `main.zip` to our compile path by editing the `build.xml` file. We can change the `javac` statement to include that file:
    
    
    <javac destdir="build/tmp"
        source="1.8"
        target="1.8"
        bootclasspath="lib/CLDC11.jar"
        classpath="myPathToUnzippedCN1Lib/main.zip:${javac.classpath}:${build.classes.dir}">
        <src path="${src.dir}"/>
    </javac>

This will only solve a part of the problem, we also need to edit the classpath in the IDE settings if you are in NetBeans we can do this for both NetBeans and IntelliJ by editing the `nbproject/project.properties` file:
    
    
    javac.classpath=
        ${file.reference.CLDC11.jar}:
        ${file.reference.CodenameOne.jar}:
        ${file.reference.CodenameOne_SRC.zip}:
        ${file.reference.MyCn1Lib-override}:
        ${file.reference.main.zip}
    file.reference.main.zip=myPathToUnzippedCN1Lib/main.zip

Now the dependency will exist, notice you will need to have both your cn1lib and the dependent cn1lib installed in the project. That’s why [our library extension tool](/blog/automatically-install-update-distribute-cn1libs-extensions.html) supports the declaration of dependencies when you submit your library.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Kapeutini Jean Voisin** — March 13, 2018 at 5:27 pm ([permalink](https://www.codenameone.com/blog/tip-nest-cn1libs-cross-dependencies.html#comment-23914))

> Kapeutini Jean Voisin says:
>
> okay, I’ll not use it, thanks
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-nest-cn1libs-cross-dependencies.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
