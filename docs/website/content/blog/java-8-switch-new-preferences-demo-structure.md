---
title: Java 8 Switch, New Preferences & Demo Structure
slug: java-8-switch-new-preferences-demo-structure
url: /blog/java-8-switch-new-preferences-demo-structure/
original_url: https://www.codenameone.com/blog/java-8-switch-new-preferences-demo-structure.html
aliases:
- /blog/java-8-switch-new-preferences-demo-structure.html
date: '2016-05-08'
author: Shai Almog
---

![Header Image](/blog/java-8-switch-new-preferences-demo-structure/java-8-lambada.png)

With the 3.4 release we discussed the process of modernizing the demos and also mention that we would  
continue the trend of building Codename One on top of itself. We now have a rough outline of what we are  
going to do possibly starting with the next plugin update.

### Java 8 Switch

New builds will use Java 8 by default for all projects. In the past we needed you to define the build hint  
`java.version=8` and if you left it out we defaulted to `java.version=5`.

With the this will now be the exact opposite where the default will assume Java 8 unless you are using  
versioned builds.

This is a part of a wider switch that we will carry into the IDE plugin and into the Codename One libraries.  
The Codename One plugins will require Java 8 to install starting with the next update, we’re not sure if there  
is a way to enforce this with the IDE dependencies but we will start assuming that Java 8

### New Preferences UI

One of the hard things about maintaining the plugin for 3 platforms is the preferences UI which we need to  
update across all platforms. With new changes coming up to the Windows UWP build we will need to make  
changes to the preferences and carrying them to all IDE’s might be challenging.

So we are focusing around one UI which is the one we introduced with the [IntelliJ IDEA plugin](/blog/a-new-idea.html).  
This UI is written in Codename One and solves a lot of bugs in the old preferences UI’s both in Eclipse & NetBeans.

![Preferences UI written in Codename One](/blog/java-8-switch-new-preferences-demo-structure/a-new-idea-preferences.png)

Figure 1. Preferences UI written in Codename One

This will allow us to map functionality to UI preferences more smoothly as we move Codename One forward.

#### Old Preferences

For now we won’t remove the old preferences UI as the new interface might still be unstable. However, we will  
post a notice that the UI is deprecated and will remove it in a future iteration.

You will find the new preferences option under the Codename One section in an upcoming plugin update.

### New Demo Structure

We decided to move the demos to separate repositories and retire the monolithic  
[codenameone-demos](https://github.com/codenameone/codenameone-demos) repository.  
This will allow us to be more nimble and will also simplify the process of working with these demos.

We will recreate the individual demos one by one in separate repositories and integrate them into the  
new plugin implementation.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Maaike Z** — May 10, 2016 at 9:07 am ([permalink](https://www.codenameone.com/blog/java-8-switch-new-preferences-demo-structure.html#comment-21510))

> Can we also use the Java 8 Date/Time functions or is this just for running the plugin etc?
>



### **Shai Almog** — May 11, 2016 at 5:40 am ([permalink](https://www.codenameone.com/blog/java-8-switch-new-preferences-demo-structure.html#comment-22832))

> Shai Almog says:
>
> No. This applies to the plugin and the default behavior which uses the current Java 8 retrolambda based support.
>
> There is an open source implementation of JSR 310 (date time) which we could possibly add (I already filed an RFE on that a while back) but I’m not sure how practical it is to add that.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
