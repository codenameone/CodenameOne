---
title: Big Changes and CEF
slug: big-changes-jcef
url: /blog/big-changes-jcef/
original_url: https://www.codenameone.com/blog/big-changes-jcef.html
aliases:
- /blog/big-changes-jcef.html
date: '2020-07-16'
author: Shai Almog
---

![Header Image](/blog/big-changes-jcef/generic-java-2.jpg)

Today we released one of the biggest changes to Codename Ones simulator in ages. We added the ability to use CEF (Chrome Embedding Framework). This is **currently** off by default but even if you don’t use it you might feel the impact so it’s crucial that you read this post.

__ |  Updated July 31st with additional platform instructions below   
---|---  
  
__ |  Updated August 2nd with correction to the Linux install script   
---|---  
  
__ |  Updated August 4th with another correction to the Linux install script   
---|---  
  
### The TL;DR

The big change for those of you who don’t care about the details is this: FX will no longer install automatically if it’s missing. That might be a good thing for some applications. But if you rely on media/web things might break in the simulator/debugger.

The short term workaround is to install a JVM that supports JavaFX out of the box such as [ZuluFX](https://www.azul.com/downloads/zulu-community/) and make sure your IDE uses it. Make sure it’s first in your path and that JAVA_HOME points at it.

Another option is to migrate to CEF which might not be an option right now if your needs are mostly media related. Read on for the details.

### Why?

I wrote before about our [desire to kick JavaFX and its problems to the curve](https://www.codenameone.com/blog/moving-away-from-fx.html). There’s no way around it. It’s outdated and buggy.

CEF is literally Chrome. It’s modern and up to date, so newer browser features behave as expected. It’s also easy to debug and has a lot of other great features we can use. It would also free us from some of the JVM dependencies and let us build smaller desktop apps moving forward.

The reason we really need it at this moment is support for WebRTC which isn’t available in the JavaFX version of the browser but is available in Chromium.

__ |  You can easily debug CEF `BrowserComponent` in Chrome by navigating to <http://localhost:8088/> in Chrome   
---|---  
  
### How will this Impact Me?

Hopefully you won’t run into any problem. If you’re using a JDK that doesn’t include JavaFX you might run into a problem in this transition period and things might fail. We recommend [ZuluFX](https://www.azul.com/downloads/zulu-community/) for now.

__ |  Once this transition period is done this should work with any JVM   
---|---  
  
By default CEF is off but you can turn it on explicitly instead of installing JavaFX.

### Turn on CEF

**This post was updated on July 31st with details on all platforms**

When complete we will automatically download and install CEF on the first activation effectively disabling the JavaFX mode.

If the `~/.codenameone/cef` directory is present we assume CEF is installed and try to load it instead of JavaFX.

#### Mac Install

To install manually download [this file](https://github.com/shannah/codenameone-cef/blob/master/dist/cef-mac.zip?raw=true). Then perform the following command in the terminal:
    
    
    unzip ~/Downloads/cef-mac.zip -d ~/.codenameone

To uninstall CEF in case of a problem do:
    
    
    rm -Rf ~/.codenameone/cef

#### Windows Install

If you’re using Win32 download [this file](https://github.com/shannah/codenameone-cef/blob/master/dist/cef-win32.zip?raw=true).

For Win64 download [this file](https://github.com/shannah/codenameone-cef/blob/master/dist/cef-win64.zip?raw=true).

Open your user directory and search for the `.codenameone` directory. In that directory unzip the downloaded zip file. It should include a `cef` directory. If not make sure to unzip the content into a directory named `cef`.

You can uninstall it by deleting the `cef` directory at any time.

#### Linux Install

__ |  We only support 64 bit Linux at this time. If there are developers using 32 bit Linux as their desktops please let us know   
---|---  
  
Download the file [this file](https://github.com/shannah/codenameone-cef/blob/master/dist/cef-linux64.zip?raw=true).

Then install using:
    
    
    mkdir ~/.codenameone/cef
    unzip cef-linux64.zip -d ~/.codenameone/cef
    chmod 755 ~/.codenameone/cef/lib/linux64/jcef_helper

To uninstall CEF in case of a problem do:
    
    
    rm -Rf ~/.codenameone/cef

#### What’s Missing?

With the CEF pipeline media is implemented using the browser component. So videos literally play in the Chrome browser (seamlessly, you wouldn’t know). This removes the need for JavaFX completely and simplifies a lot of things.

However, there’s one missing piece at the moment: h264 support.

By default JCEF doesn’t include the h264 codec due to patent restrictions. This isn’t a problem for our use case but it means we need to get a binary build of CEF working and the build environment for Chrome is “tough”. So right now h264 isn’t working.

Other than that we’re still missing Windows and Linux support. We’re also missing an installer that will deliver CEF seamlessly. All of those will ship together as part of an update in the next couple of weeks once all issues are resolved.

### How does this Work?

Up until now the JavaSE port had one version which was `JavaSEPort`. This is now a base class for three implementations:

  * **JavaFX** — a compatibility mode implementation which is currently the default.

  * **CEF** — the new mode which will run if the cef directory is available/

  * **JMF** — a special case that uses Java Media Framework for media playback instead of JavaFX or JCEF. It has the advantage of being very small. It works very similarly to the CEF approach by searching for the JMF jar in the `.codenameone` directory and using it if it’s available. We’re not sure this is a use case worth pursuing.

On launch we pick the best option. If CEF is available under the `.codenameone` directory we pick that implementation. This uses the native library and integrates directly into the UI.

### Up Next

Once this migration is done we’ll follow up with some posts on debugging under CEF etc. Please let us know if you run into trouble ASAP.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Durank** — July 17, 2020 at 12:12 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24298))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> when this post it will be available to windows? this means that the actual simulator will not work?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — January 20, 2021 at 2:26 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24384))

> Shai Almog says:
>
> See the section titled: “Windows Install” above…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Steve Hannah** — July 17, 2020 at 1:48 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24299))

> [Steve Hannah](https://lh3.googleusercontent.com/a-/AAuE7mBmUCgKSZtJ2cqeHgj6bdPY2AAQ10roHlMpgRWc) says:
>
> Windows will be available soon.  
> The simulator will still work without CEF. If you are on Windows, and your app doesn’t use Media or BrowserComponent, then it will still work fine (without CEF). If your app uses Media or BrowserComponent, then just make sure you’re using a JDK that has JavaFX, such as ZuluFX – and it will work fine.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Angelo** — August 3, 2020 at 1:53 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24303))

> Angelo says:
>
> I think that the CEF install instructions for Linux are wrong, because the zip file has not cef as root so the unzipping happens, but I had to create manually the cef folder and copy the zip content there. I hope it is useful.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Angelo** — August 3, 2020 at 2:01 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24304))

> Angelo says:
>
> As for the cef loading into the IDE, no updates are available for IntellJ Idea so, being that it seems that I cannot receive the codename updates as soon as they are published, I have to wait for the IDE update. Unless there is a workaround that you know to force the update.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — August 4, 2020 at 3:34 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24305))

> Shai Almog says:
>
> Ugh. I’ll fix the instructions for that. Thanks.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — August 4, 2020 at 3:36 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24302))

> Shai Almog says:
>
> We push updates separately from the plugins. That way we can support 3 IDEs with more common code. Just update via Codename One Settings using the menu on the top right.  
> If you still have the old settings UI it’s under the Basic section.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Angelo** — August 4, 2020 at 7:42 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24309))

> Angelo says:
>
> The settings app found that there was a lock file in the .codename directory. I had to delete it to perform the update.  
> It is possible that also the other Codename plugin updates on my environment were blocked by that. Maybe the automatic update procedure should check it and inform the user, in case.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Richard Matovu** — August 9, 2020 at 9:13 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24317))

> [Richard Matovu](https://lh3.googleusercontent.com/a-/AOh14Ggv6DF23oF-udh-1mkmXOx1WryNt3gP1I1fADw2) says:
>
> I have intergrated CEF in my project and it runs well. When i came to the point where it had to show a browser component, it showed an error “An internal application error occured: java.lang.RuntimeException: Failed to create CEF browser” and in the console it displayed:  
> CEF Args: [–disable-gpu, –disable-software-rasterizer, –disable-gpu-compositing, –touch-events=enabled, –enable-media-stream, –device-scale-factor=4, –force-device-scale-factor=4, –autoplay-policy=no-user-gesture-required, –enable-usermedia-screen-capturing]  
> java.lang.RuntimeException: Failed to create CEF browser  
> at com.codename1.impl.javase.cef.JavaCEFSEPort.createCEFBrowserComponent(JavaCEFSEPort.java:106)  
> at com.codename1.impl.javase.cef.JavaCEFSEPort.createBrowserComponent(JavaCEFSEPort.java:81)  
> at com.codename1.ui.BrowserComponent$9.run(BrowserComponent.java:531)  
> [EDT] 0:0:19,935 – Exception: java.lang.RuntimeException – Failed to create CEF browser  
> at com.codename1.ui.Display.processSerialCalls(Display.java:1331)  
> at com.codename1.ui.Display.edtLoopImpl(Display.java:1274)  
> at com.codename1.ui.Display.mainEDTLoop(Display.java:1162)  
> at com.codename1.ui.RunnableWrapper.run(RunnableWrapper.java:120)  
> at com.codename1.impl.CodenameOneThread.run(CodenameOneThread.java:176)  
> Caused by: java.lang.reflect.InvocationTargetException  
> at java.desktop/java.awt.EventQueue.invokeAndWait(EventQueue.java:1367)  
> at java.desktop/java.awt.EventQueue.invokeAndWait(EventQueue.java:1342)  
> at com.codename1.impl.javase.cef.JavaCEFSEPort.createCEFBrowserComponent(JavaCEFSEPort.java:99)  
> … 7 more  
> Caused by: java.lang.UnsatisfiedLinkError: /home/donrix/.codenameone/cef/lib/linux64/libjcef.so: libjawt.so: cannot open shared object file: No such file or directory  
> at java.base/java.lang.ClassLoader$NativeLibrary.load0(Native Method)  
> at java.base/java.lang.ClassLoader$NativeLibrary.load(ClassLoader.java:2442)  
> at java.base/java.lang.ClassLoader$NativeLibrary.loadLibrary(ClassLoader.java:2498)  
> at java.base/java.lang.ClassLoader.loadLibrary0(ClassLoader.java:2694)  
> at java.base/java.lang.ClassLoader.loadLibrary(ClassLoader.java:2640)  
> at java.base/java.lang.Runtime.loadLibrary0(Runtime.java:830)  
> at java.base/java.lang.System.loadLibrary(System.java:1873)  
> at org.cef.SystemBootstrap$1.loadLibrary(SystemBootstrap.java:24)  
> at org.cef.SystemBootstrap.loadLibrary(SystemBootstrap.java:36)  
> at org.cef.CefApp.startup(CefApp.java:536)  
> at com.codename1.impl.javase.cef.CEFBrowserComponent.create(CEFBrowserComponent.java:178)  
> at com.codename1.impl.javase.cef.CEFBrowserComponent.create(CEFBrowserComponent.java:170)  
> at com.codename1.impl.javase.cef.CEFBrowserComponent.create(CEFBrowserComponent.java:167)  
> at com.codename1.impl.javase.cef.JavaCEFSEPort.createCEFBrowserComponent(JavaCEFSEPort.java:112)  
> at com.codename1.impl.javase.cef.JavaCEFSEPort$2.run(JavaCEFSEPort.java:102)  
> at java.desktop/java.awt.event.InvocationEvent.dispatch(InvocationEvent.java:303)  
> at java.desktop/java.awt.EventQueue.dispatchEventImpl(EventQueue.java:770)  
> at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:721)  
> at java.desktop/java.awt.EventQueue$4.run(EventQueue.java:715)  
> at java.base/java.security.AccessController.doPrivileged(Native Method)  
> at java.base/java.security.ProtectionDomain$JavaSecurityAccessImpl.doIntersectionPrivilege(ProtectionDomain.java:85)  
> at java.desktop/java.awt.EventQueue.dispatchEvent(EventQueue.java:740)  
> at java.desktop/java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:203)  
> at java.desktop/java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:124)  
> at java.desktop/java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:113)  
> at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:109)  
> at java.desktop/java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:101)  
> at java.desktop/java.awt.EventDispatchThread.run(EventDispatchThread.java:90)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — August 10, 2020 at 5:09 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24311))

> Shai Almog says:
>
> Thanks. Can you please post an issue about this in the issue tracker. Please also include some information about your system such as env output, distro etc. The issue tracker is here: <https://github.com/codenameone/CodenameOne/issues/>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Carlos Verdier** — August 14, 2020 at 9:28 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24323))

> [Carlos Verdier](https://lh3.googleusercontent.com/-y-v_mMAwszk/AAAAAAAAAAI/AAAAAAAAAAA/AMZuucmcoea9nf4P3gRHGzB7T7jxG98R1w/photo.jpg) says:
>
> Not working for me. Whenever I try to play a video or open a browser, it refuses with this output:
>
> “/Users/carlos/.codenameone/cef/macos64/libjcef.dylib: dlopen(/Users/carlos/.codenameone/cef/macos64/libjcef.dylib, 1): no suitable image found. Did find:  
> /Users/carlos/.codenameone/cef/macos64/libjcef.dylib: code signature in (/Users/carlos/.codenameone/cef/macos64/libjcef.dylib) not valid for use in process using Library Validation: library load disallowed by system policy”
>
> Tested on Mac OS Catalina 10.15.6
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Steve Hannah** — August 14, 2020 at 12:52 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24324))

> [Steve Hannah](https://lh3.googleusercontent.com/a-/AAuE7mBmUCgKSZtJ2cqeHgj6bdPY2AAQ10roHlMpgRWc) says:
>
> I haven’t been able to reproduce this error, but I’ve found lots of bug reports around the internet related to Catalina. It could be related to the JDK that you’re using, if it is signed/notarized. Or it could be related to restrictive settings in Catalina. 
>
> Can you file this in the issue tracker so we can track it. As a starting point, what JDK are you using?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Artur Hefczyc** — August 27, 2020 at 9:20 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24331))

> [Artur Hefczyc](https://avatars3.githubusercontent.com/u/1848738?v=4) says:
>
> On Macs downloaded files are often automatically unzipped. In such a case, you can add that info to install the cef using move command instead:  
> “`  
> mv ~/Downloads/cef ~/.codenameone/  
> “`
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Artur Hefczyc** — August 28, 2020 at 7:21 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24326))

> [Artur Hefczyc](https://avatars3.githubusercontent.com/u/1848738?v=4) says:
>
> I have installed cef as instructed above. However, when I run my app in simulator I still see  
> “`  
> CSS> JavaFX is loaded  
> “`  
> in the console output. Is this expected? I mean, do you still use JavaFX in simulator even if cef is installed?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — August 29, 2020 at 4:34 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24330))

> Shai Almog says:
>
> I think that’s a buggy printout from the prior condition. It indicates you have JavaFX in your system but it probably still uses CEF anyway. I filed an issue on that here: <https://github.com/codenameone/CodenameOne/issues/3245>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Artur Hefczyc** — September 11, 2020 at 3:44 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24334))

> [Artur Hefczyc](https://avatars3.githubusercontent.com/u/1848738?v=4) says:
>
> So, how to make sure I am running in CEF mode instead of JavaFX? Is there any way to confirm the env my app us running in?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — September 11, 2020 at 3:45 am ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24332))

> Shai Almog says:
>
> If <http://localhost:8088/> in chrome shows the debugging options for the browser then CEF is working as expected
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Artur Hefczyc** — September 11, 2020 at 4:49 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24336))

> [Artur Hefczyc](https://avatars3.githubusercontent.com/u/1848738?v=4) says:
>
> Neither with my app running in simulator or running as a compiled desktop app Chrome shows anything at this address:  
> “`  
> This site can’t be reached  
> localhost refused to connect.  
> “`
>
> The cef folder exist:  
> “`  
> $ ll ~/.codenameone/cef  
> total 720  
> -rw-r–r–@ 1 usr staff 170K Jul 10 15:12 jcef-tests.jar  
> -rw-r–r–@ 1 usr staff 185K Jul 10 15:12 jcef.jar  
> drwxr-xr-x@ 8 usr staff 256B Jul 10 15:12 macos64  
> “`
>
> Do you have any suggestions on what can be wrong?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Shai Almog** — September 11, 2020 at 4:51 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24333))

> Shai Almog says:
>
> Is this not reachable when a browser component is physically on the screen?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)


### **Julio Valeriron Ochoa** — January 19, 2021 at 8:49 pm ([permalink](https://www.codenameone.com/blog/big-changes-jcef.html#comment-24383))

> Julio Valeriron Ochoa says:
>
> how can I configure new cfe simulator in windows?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbig-changes-jcef.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
