---
title: Moving Away from Java FX
slug: moving-away-from-fx
url: /blog/moving-away-from-fx/
original_url: https://www.codenameone.com/blog/moving-away-from-fx.html
aliases:
- /blog/moving-away-from-fx.html
date: '2020-06-18'
author: Shai Almog
---

![Header Image](/blog/moving-away-from-fx/generic-java-1.jpg)

Codename One itself never depended on JavaFX. This kept us small and performant. However, we need JavaFX to support HTML and media in the simulator and on the desktop ports. This was a choice we made easily back in the Java 8 days. JavaFX was integrated into the official JDK and this was an easy choice to make.

Then Java 9 came out and everything broke. Most JVMs ship without JavaFX now and downloading it dynamically for the simulator is error prone to say the least. Even I had problems setting up our environment on some foreign machines. Every day we need to deal with multiple support queries and people who have issues with VM configuration. 99% are due to the pain of dealing with JavaFX installation on top of the VM.

### Not Worth Fixing

There are many approaches we could take to try and solve this. All of them suck but they are possible…​

We could create different versions of Codename One for each platform and ship with our own OpenJDK port that includes everything we need. This would have ballooned the size of install and made it harder for you to customize/tinker with Codename One.

The problem is we’d still be stuck with FX.

The main reason for using FX is the `BrowserComponent`. Swing just doesn’t have a decent browser widget and FX provides the closest thing to a browser we can use…​

The thing is, it still sucks. Newer web standards aren’t supported. Debugging is difficult and it crashes…​ A lot.

### A Better Way

Recently IntelliJ took the same path, they decided to [deprecate the use of JavaFX](https://blog.jetbrains.com/idea/2020/06/intellij-idea-2020-2-eap2-is-here-with-advanced-exception-stack-trace-analysis-emoji-support-on-linux-and-more/) in favor of [JCEF](https://bitbucket.org/chromiumembedded/java-cef/src/master/).

If we migrate to JCEF we’d have access to the latest Chromium APIs and tools. Ideally we’d also enjoy better stability, control and JVM compatibility. The drawback is that we’d need to write native code and possibly increase the Codename One download size.

The big missing piece here is media. We’re still testing the waters on this but a good direction might be to use the media capabilities of Chromium to show things in the simulator and desktop ports.

### Compatibility

In order to maintain compatibility for developers using the desktop port we’ll keep the existing implementation that relies on JavaFX for the short term. Since the desktop port packages the VM within this shouldn’t be a problem.

However, we will change the default build to use JCEF once we deem this stable enough and might eventually sunset the FX port entirely based on your feedback. This will have a big size advantage for developers as we’ll be able to package a smaller VM without the JavaFX dependency.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — June 20, 2020 at 10:59 pm ([permalink](/blog/moving-away-from-fx/#comment-24277))

> [Francesco Galgani](https://lh6.googleusercontent.com/-4K0ax_DVJf4/AAAAAAAAAAI/AAAAAAAAAAA/AMZuuckEd1kcni0y8k6NMzNtxwOCEPatQQ/photo.jpg) says:
>
> JavaFX is necessary also for the CSS support, in particular for generating 9-piece image borders.
>



### **Shai Almog** — June 21, 2020 at 1:46 am ([permalink](/blog/moving-away-from-fx/#comment-24276))

> Shai Almog says:
>
> Yes, but it won’t be with JCEF as it can be used instead and would probably work better/faster as well.
>



### **Javier Anton** — June 24, 2020 at 8:52 pm ([permalink](/blog/moving-away-from-fx/#comment-24282))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> We recently introduced (.net) CEF in one of the apps I make at work to replace embedded IEs. It increased the build size from 7 to 70MB. The business could not be happier as performance has really improved. People like performance. Desktop build size is not as much of a problem as mobile app size
>



### **Shai Almog** — June 25, 2020 at 2:01 am ([permalink](/blog/moving-away-from-fx/#comment-24279))

> Shai Almog says:
>
> Yes. Actually for the desktop apps I think it will be smaller than the full blown JavaFX VM and we will be able to download it dynamically if necessary in the future.  
> The main problem is the IDE libraries. Here size matters. Every week we release an update and if that update includes a 60mb CEF attachment this can be a problem. So we need this as a separate thing from the main JAR and ideally we’d want to keep it completely separate as our plugin is already freakishly huge.
>



### **Angelo** — July 15, 2020 at 2:47 pm ([permalink](/blog/moving-away-from-fx/#comment-24294))

> Angelo says:
>
> Do you cofirm it’s under active development and it wil be released in a few days?
>



### **Shai Almog** — July 16, 2020 at 2:21 am ([permalink](/blog/moving-away-from-fx/#comment-24297))

> Shai Almog says:
>
> Take a look at our github commit history.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
