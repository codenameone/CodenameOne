---
title: Java Developers can FINALLY Target Windows UWP
slug: java-developers-can-finally-target-windows-uwp
url: /blog/java-developers-can-finally-target-windows-uwp/
original_url: https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html
aliases:
- /blog/java-developers-can-finally-target-windows-uwp.html
date: '2016-06-11'
author: Shai Almog
---

![Header Image](/blog/java-developers-can-finally-target-windows-uwp/universal-windows-apps_thumb.jpg)

After many months of work and effort put in by all of us but especially by [Steve Hannah](http://twitter.com/shannah78) and  
[Fabrício Carvalho Cabeça](https://twitter.com/ravnos_kun) we are finally live with the Windows Universal Platform (AKA UWP)  
native build target!  
As far as I know Codename One is the only native option for Java developers to build native Windows UWP apps.

__ |  Notice that this target is still in technology preview stage! Please report any issue you run into   
---|---  
  
Besides the effort of building the Codename One port with everything it entails (including newer build servers)  
we also leveraged the ambitious [iKVM](http://www.ikvm.net) project which we had to modify extensively with  
the help of some community members specifically [Eugene](https://twitter.com/Geraschenco) who proved very  
helpful during this work!

(I hope I didn’t forget anyone, I didn’t take an active role in this port and I might have missed someone if so please  
accept my apologies).

The whole source of this port as well as our changes to iKVM are available in [our git repository](http://github.com/codenameone/CodenameOne/).

### What does this Mean?

As of May Windows 10 is installed on 300 million PC’s and devices making it a significant platform and appstore.  
Microsoft has traditionally been quite strong in the enterprise and the ability to sell into that market thru it’s  
appstore (with the success of the Surface tablet line) is valuable.

Microsoft has standardized on the Universal Windows Platform which effectively “reinvents” Windows as  
a single platform on all supported devices. In a way this is very much like Java’s Write Once Run Anywhere  
for the Windows ecosystem (mobile, tablets, xbox, desktops etc.).

Windows UWP apps can be sold thru the Microsoft appstore which is currently growing as users are still  
adopting Windows 10.

#### Building an App

You don’t need to make any change to your app to run it on a Windows 10 device/computer. Notice that you  
will need a certificate file to ship such an app, we are still working on instructions to generate such a certificate  
in a simple way but you can configure a license file in the new Codename One settings UI under the  
Windows UWP settings.

__ |  This is currently optional, you can send a build without configuring a certificate to test this on your local machine   
---|---  
  
![Open the new Codename One preferences options in the right click for the project](/blog/java-developers-can-finally-target-windows-uwp/new-codenameone-settings.png)

Figure 1. Open the new Codename One preferences options in the right click of the project

![Scroll down using swipe and click the UWP section](/blog/java-developers-can-finally-target-windows-uwp/uwp-settings-section.png)

Figure 2. Scroll down using swipe and click the UWP section

![You can now set a certficiate file and password for signing a universal app](/blog/java-developers-can-finally-target-windows-uwp/uwp-settings.png)

Figure 3. You can now set a certficiate file and password for signing a universal app

To actually build the native app you can just right click and select the Windows UWP target, don’t be confused  
with the two other options for desktop & the old Windows Phone support…​

![The new Windows UWP target](/blog/java-developers-can-finally-target-windows-uwp/windows-uwp-target.png)

Figure 4. The new Windows UWP target

Installing the build on your machine requires Windows 10 (obviously) but also requires development mode indicating  
that you can “sideload” applications. We need to writeup our own documentation on how to do this but  
for now check out  
[this well written guide](https://support.hockeyapp.net/kb/client-integration-windows-and-windows-phone/how-to-sideload-uwp-applications#3-install-application).

Once you do that your app can run on your machine and we should be able to submit such apps to the Windows  
store:

![Hello world app running on a Windows 10 laptop](/blog/java-developers-can-finally-target-windows-uwp/windows-app-running.png)

Figure 5. Hello world app running on a Windows 10 laptop

### What’s Next?

The next set of steps depend on you. We will try to get apps into the Windows Store in order to complete the process  
but we will consider this port production grade only when significant apps start shipping to the Windows Store.

So we need bug reports and demand from you in order to bring this to that coveted production grade status…​

#### Future of Windows Desktop Port

You will also notice, UWP allows building Windows desktop apps which overlaps with the Windows Desktop  
build target that is currently limited to pro subscribers.

There is still some value in the Windows desktop build in the sense that it’s really a Java SE application with the  
full power of the JRE behind it. If you need that and compatibility to older versions of Windows this can be quite  
powerful…​

However, if you are interested in a smaller native binary and can live with Windows 10 or newer as the baseline  
I’d go for the new port as it should provide a superior native experience with a smaller footprint.

For now we will keep supporting the Windows desktop target and have no plans of removing/deprecating it.

### Build Hints

__ |  This section was added on June 14th and was missing from the original post…​   
---|---  
  
By default debug builds are sent for Windows UWP builds, those default to using x64 architecture only to keep  
the size small. When you send release builds you will receive a proper universal binary.

To toggle these modes we added two build hints:

`uwp.buildType` which can be either `debug` (the default) or `release`.

`uwp.platforms` which defaults to `x64` on debug builds but can be set to `x64|x86|ARM` for universal builds.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — June 12, 2016 at 9:26 pm ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22469))

> This sounds really good. I don’t have much to do with the Windows eco-system, so if I want to try this, I’m guessing any recent Windows mobile should run this, or is there some specific version (of the seemingly ever changing) Windows mobile platform I need to look for ?
>



### **Shai Almog** — June 13, 2016 at 3:53 am ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22731))

> It requires a device that’s running windows 10 and some of the older devices won’t get that upgrade.
>



### **Chibuike Mba** — June 13, 2016 at 7:40 am ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22587))

> WOW!!! CodenameOne rocks, one stone (code base) to kill(target) all the birds(platforms) in the air(out there). Am loving it. Will surely try it in the next version of our app [http://ozioma.net](<http://ozioma.net>). Great job guys. 🙂
>



### **Lukman Javalove Idealist Jaji** — June 13, 2016 at 1:20 pm ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22771))

> I am over the moon with this!!!!!!!!!
>



### **Ben A** — August 8, 2016 at 8:49 pm ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22711))

> Good news for Java devs
>



### **Teguh Kusuma** — September 5, 2016 at 4:24 pm ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22707))

> Teguh Kusuma says:
>
> Is it free for beginner developer?
>



### **Shai Almog** — September 6, 2016 at 3:54 am ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-22915))

> Shai Almog says:
>
> It is available to all subscription levels including the free level.
>



### **Cristian Romascu** — September 29, 2016 at 5:51 am ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-23034))

> Cristian Romascu says:
>
> Hello, i have 3 already made Java apps (using nothing but Java SE), can i convert them to UWP using Codename One?
>



### **Shai Almog** — September 30, 2016 at 6:48 am ([permalink](https://www.codenameone.com/blog/java-developers-can-finally-target-windows-uwp.html#comment-23063))

> Shai Almog says:
>
> No. Codename One supports a subset of Java SE and our own UI API which is more portable than anything available in JavaSE.  
> OTOH you will gain for your effort the portability to iOS, Android, UWP, JavaScript (with threads etc.)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
