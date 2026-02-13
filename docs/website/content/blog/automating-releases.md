---
title: Automating Releases
slug: automating-releases
url: /blog/automating-releases/
original_url: https://www.codenameone.com/blog/automating-releases.html
aliases:
- /blog/automating-releases.html
date: '2015-05-31'
author: Shai Almog
---

![Header Image](/blog/automating-releases/continuous-integration-1.png)

Our website deployment has become even more complex thanks to the [demos section](/demos.html).  
The crux of it is in updating the demos with every small update to the JavaScript build process which is why  
we implemented a build option based on the work we did for our [CI (Jenkins) integration](/blog/continuous-integration.html).  
This work essentially allows to build a Codename One app synchronously which is useful when you want  
to do things such as continuous integration or release engineering.  
Notice that the synchronous build feature is an enterprise only feature since its overuse can have a very heavy toll on our servers. 

Essentially we copied the existing build.xml to a separate file to prevent updates from overriding it. We then added  
targets such as this for the kitchen sink: 
    
    
    <target name="build-for-javascript-sync" depends="clean,copy-javascript-override,copy-libs,jar,clean-override">
        <codeNameOne 
            jarFile="${dist.jar}"
            displayName="${codename1.displayName}"
            packageName = "${codename1.packageName}"
            mainClassName = "${codename1.mainName}"
            version="${codename1.version}"
            icon="${codename1.icon}"
            vendor="${codename1.vendor}"
            subtitle="${codename1.secondaryTitle}"
            automated="true"
            targetType="javascript"
            />
    </target>

This is effectively a copy and paste of the `build-for-javascript` target where we added the line  
`automated="true"` to indicate that this build works in a singular process.   
After the build completes we are left with a `result.zip` file in the `dist` folder. Which  
we unzip to find all the files from the build server: 
    
    
    <mkdir dir="build/demoTmp/unzipped" />
    <unzip src="build/demoTmp/${demoname}-1.0.zip" dest="build/demoTmp/unzipped" />
    <delete file="build/demoTmp/unzipped/index.html" />
    <copy  todir="demos/${demoname}">
        <fileset dir="build/demoTmp/unzipped"/>
    </copy>

While the sample above shows the JavaScript build target it can be applied to any of the Codename One build  
targets and is remarkably useful for a release engineering process. When you need to release one version for all platforms  
on a frequent basis even a minute automation like this makes a big difference.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Blessing Mahlalela** — February 9, 2017 at 4:35 pm ([permalink](https://www.codenameone.com/blog/automating-releases.html#comment-23223))

> Blessing Mahlalela says:
>
> Hi during development I noticed that if I try to send multiple builds ie Send Android, Send iOS.. on Netbeans etc. I would receive a compile error if I send them too quickly (simultaneously), I think CN1 deletes some files during the send build process. In any case the reason why I saying that is, I have Jenkins setup and would like to return a [result.zip](<http://result.zip>) containing multiple platform result files ie Android, iOS, Web, Desktop. How can I go about doing this on Jenkins build.xml? I am currently able to set ANT targets on Jenkins pre and post build.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fautomating-releases.html)


### **Shai Almog** — February 9, 2017 at 6:32 pm ([permalink](https://www.codenameone.com/blog/automating-releases.html#comment-21563))

> Shai Almog says:
>
> Yes, you can send concurrent builds but not at once. If you use automation to do this your user can’t send a build at that exact time. It’s just a limitation in the way the system was designed as the code that allocates a build needs to reserve a spot.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fautomating-releases.html)


### **Blessing Mahlalela** — February 9, 2017 at 6:43 pm ([permalink](https://www.codenameone.com/blog/automating-releases.html#comment-23319))

> Blessing Mahlalela says:
>
> Ok, I have now managed to call an ANT “build-for-javascript” target from the build xml. Thanks a lot for this, no more sitting and waiting for builds, secondly the automated test recorder will become a great resource to small dev organisations that just don’t have budget for dedicated test teams!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fautomating-releases.html)


### **Blessing Mahlalela** — February 9, 2017 at 6:46 pm ([permalink](https://www.codenameone.com/blog/automating-releases.html#comment-23071))

> Blessing Mahlalela says:
>
> One more question. How can I add multiple ANT arguments on Jenkins? I would like to automate the building of Android, iOS & web on every successful CI build.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fautomating-releases.html)


### **Blessing Mahlalela** — February 9, 2017 at 8:21 pm ([permalink](https://www.codenameone.com/blog/automating-releases.html#comment-23237))

> Blessing Mahlalela says:
>
> Managed to do multiple builds by adding additional ANT build steps. Had to configure signing certificates also.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fautomating-releases.html)


### **Shai Almog** — February 9, 2017 at 8:49 pm ([permalink](https://www.codenameone.com/blog/automating-releases.html#comment-23077))

> Shai Almog says:
>
> Yes we do them one by one since they are synchronous. I hope to write a more detailed blog on doing this in a future update just didn’t get around to doing it.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fautomating-releases.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
