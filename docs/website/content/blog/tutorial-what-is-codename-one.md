---
title: Tutorial – What is Codename One
slug: tutorial-what-is-codename-one
url: /blog/tutorial-what-is-codename-one/
original_url: https://www.codenameone.com/blog/tutorial-what-is-codename-one.html
aliases:
- /blog/tutorial-what-is-codename-one.html
date: '2017-11-08'
author: Shai Almog
---

![Header Image](/blog/tutorial-what-is-codename-one/learn-codenameone-1.jpg)

I published this video a while back but it was longer and a bit confusing (over 40 minutes). Since some developers watch it before getting into Codename One I thought it would be in order to streamline it into a more manageable length and transcribe the content like I did with the newer videos. I also cleaned it up a bit and the result is below.

You can also read the full transcript within this blog post and use the captions that are a part of the video.

### Transcript

In this video I’m going to explain “what is Codename One” but I’m going to take the scenic route…​ I’m going to go through the full history of our platform and I’m going to explain the underlying technology. How it works. What makes it different from other tools at least at a high level. If you want a 3 minute hello world video this isn’t it, go to the download section of Codename One dot com and check out the 3 minute videos there. Here I try to go deeper.

Let’s start with the historic timeline of Codename One.

First there was LWUIT, Chen started this project at Sun Microsystems back in 2006-2007 with the goal of write once run anywhere that actually works for mobile phones. Back then there were only J2ME & RIM platforms and device fragmentation was a major problem. The standards were implemented poorly by the various vendors and they were very hard to use. So Chen took inspiration from Swing which was the leading GUI API on the desktop and adapted those concepts to mobile.

LWUIT was open sourced by Sun at JavaOne 2008. It became a huge success and was the top open source mobile project from Sun Microsystems but as LWUIT succeeded JavaME was dying. We had ports of LWUIT running on Android, RIM and the iPhone but we couldn’t support or publish them. This was due to company bureaucracy that didn’t improve as Oracle purchased Sun Microsystems.

In late 2011 we quit Sun and formed Codename One. We made a clean break from LWUIT by moving to a different package space and remodeling a lot of the API’s with a focus on smartphones. We ported everything to iOS, Android and other platforms. The big change was a change in concept. LWUIT was mostly a UI kit. Codename One was a completely inclusive environment that includes everything from the IDE plugin to the virtual machine, GUI builder and all the surrounding API’s. Codename One had a far larger and more ambitious scope than LWUIT.

This proved successful at least on a technical level as more than 100M apps were installed on devices relatively quickly and on a very diverse set of device types.

As the platform matured we expanded the scope of Codename One further and today we target not just Android & iOS. We target Universal Windows Platform which is the Microsoft standard for unified apps on Arm & intel. We target JavaScript which allows you to compile the Java applications into a JavaScript application including threads and everything so it can run in the browser. We even support desktop development which started as a curiosity due to user requirements and has picked up so well we use it ourselves. Recently we added Kotlin support and are adding new features on a weekly basis.

So I gave all of that background but still didn’t get to “what the hell is it?”. The problem is that it’s so big and hard to explain so lets break it into 6 pieces and I’ll go over each one of these pieces in great detail soon.  
First: It’s a virtual machine for all devices. The virtual machine is the environment that lets Java run on the device in a portable way.

It’s an API for all devices. The Application Programming Interface provides us with functionality that is distilled commonality from the various devices.

It’s an IDE plugin. In fact this is the only piece of Codename One you actually need. By installing the IDE plugin we automatically deliver everything else on the list

It’s a set of tools from device simulators to theme designer, GUI builder etc. Those are delivered within the plugin but deserve their own bullet as they are technically the same regardless of the IDE you use.

The cloud based system is the most confusing part. No, you don’t need the cloud to run a Codename One app. The resulting app is native. However, to build iOS apps you need a Mac and xcode which is Apples development tool. To build a Windows UWP application you need a Windows machine and Visual Studio which is Microsofts development tool. Well, we have Macs and Windows and Linux machines in the cloud. We seamlessly translate your Java bytecode to native projects and compile them using the official tools such as xcode then return the binary directly to you so you can install it on your phone or upload it to apple, Microsoft, google etc. Cool right?

Here’s a fun fact. When we started pitching the company to investors we just stuck Codename One in the slides as a placeholder and investors liked it. We later on came up with the “idea” that these 6 different pieces constitute “one” product for “one code base” and that made sense as a company name.

But first lets start with the more in depth explanation of our VM’s. We currently have 3 virtual machines that we work with. Android has it’s own native Java support and we just use that. For desktop we use standard Java too. iOS doesn’t have a virtual machine and doesn’t allow JIT compilation which is a common practice in most virtual machines. Furthermore, historic attempts to build iOS virtual machines ran into issues of size and into problems with Apples frequent changes to their development toolchain. Our open source Parpar VM solves this problem by avoiding compilation. It translates Java bytecode to C which is one of the officially supported languages in iOS and much faster than Objective-C or Swift. We can then compile this C code using Apples compiler which guarantees that every change Apple makes will be easy to adapt to.  
This isn’t just a “theoretical claim”. Since we released our VM Apple migrated from 32 bit to 64 bit and added support for bitcode compilation. All of these worked out of the box with ParparVM with no change from us. Even native developers struggled with some of those changes which were mostly seamless to Codename One developers.  
ParparVM is very performant and can reach C levels of performance in some cases. It also produces a very small binary clocking under 5mb for applications which is very small for an iOS app. Similar attempts at JVM’s often produce binaries that exceed the 100mb limit… One of the cool things you can do with ParparVM is run the code using Apples xcode and debug or profile on the device. This is a very powerful way to discover things about the application and work with native OS code.

The UWP port uses iKVM which is a project that brought Java bytecode support to .net. Initially this project didn’t work with UWP but we worked with community members to update iKVM and published our iKVM fork as open source. This again means that we use native .net support when building windows applications.

The web port uses a 3rd party VM called TeaVM. This VM is very efficient at translating bytecode to JavaScript and can produce very small binaries when compared to similar technologies. We stripped out support for some JVM classes and as a result apps can be loaded with no cache within seconds. The VM supports threads by breaking down the code in very creative ways which keeps the code very efficient.

The Codename One Application Programming Interface is a single set of API’s that abstracts common device functionality you would expect to have on a mobile phone. This includes everything from widgets to camera, media, files, networking etc.

The trick with the API is our internal porting layer which is very clearly defined and relatively thin. That means 97% of Codename One’s code is in Java and this makes it more consistent to developers across platforms.

The user interface uses a lightweight UI approach which is a common term used to define Swing widgets. you might recall that Codename One was heavily inspired by Swing…  
Lightweight UI means Codename One draws most of its widgets itself and uses the native platform mostly as a canvas and event handling environment. I’ll discuss this more in depth later on…

The API tries to be simple and unified so it does the “right thing” when possible. We can do that because the API is statically linked. That means that when an app is compiled our API becomes a part of the application. This means that if we change the API it will never impact apps in production. This isn’t true for native OS API’s who need to change very carefully so they won’t break applications that might rely on edge case functionality.

Codename One ships plugins for the 3 top Java IDE’s specifically IntelliJ, Eclipse and NetBeans.

The plugin literally includes everything that’s a part of Codename One. Since the build happens in the cloud servers you don’t need to install native OS tools or anything like that.

Essentially the plugins are relatively “stupid” as they mostly invoke Ant and our tools such as the simulator etc. This is a good thing. I use NetBeans as I’m used to it from my years at Sun. However, almost everything I do here is identical if you do it in Eclipse or IntelliJ.  
Recently we started making them even “dumber” by moving the settings from the IDE specific settings UI to the Codename One Settings tool which is common between all 3 IDE’s. This allowed us to move much faster.

The tools include 4 major groups. The first is the group of build tools which is really the ant script you have in the project and some extensions for that.

The device simulator includes skins representing various device types that you can download dynamically. It also includes a lot of additional functionality to test device behavior such as network monitor, performance analyzer, component inspector etc.

The resource editor also known as the Codename One Designer is our workhorse tool dating back to the days of LWUIT. It allows you to theme the application, localize it, manage builtin images and even included the old GUI builder of Codename One.

The new GUI builder is far more powerful and more closely matches common Java GUI builders in terms of functionality and technology.

I’ve discussed the cloud build before but it’s important enough to recap. We have Macs, Windows and Linux machines that do the build for you. This is more than just having the build platforms. Setting up the native environments and all the prerequisites for a proper build is a nightmare. The cloud build servers essentially mask this whole process completely. Notice that we have an offline build feature that allows you to skip the cloud build but we don’t recommend that. It’s mostly for government or highly regulated industries where the word cloud is a synonym to “no management approval”. In those cases offline build becomes a lesser of two evils.

The cloud is mostly abstracted, you just right click the project and send a build. Everything else is seamless thanks to the tie in between the build tools and the build severs.

You don’t need to install anything other than the plugin. Not for Android or iOS. You can work with Mac, Windows or Linux and everything should “just work”.

This is a huge contributor to the seamlessness and simplicity of Codename One that allows you to get started “right away”.

Codename One’s UI uses a lightweight UI approach which is a common term used to define Swing widgets.  
As I mentioned before Codename One draws most of its widgets itself and uses the native platform mostly as a canvas and event handling environment.  
Lightweight frameworks are very common especially in Java where Swing and JavaFX are both lightweight. QT is another example of a lightweight framework. Heavyweight frameworks include SWT and AWT from the Java side of the fence and other common tools such as Xamarin, Appcelerator etc.

As I mentioned before lightweight frameworks draw their own widgets. A native framework needs to have a native widget for every API. That might not sound like a big deal but platforms have very nuanced differences in threads, events etc. This means very nuanced bugs on the heavyweight framework side. In fact many heavyweight frameworks don’t define themselves as write once run anywhere tools since that is so hard to accomplish with this architecture.

A lightweight architecture is really complex internally. Codename One is a hugely complex API developed for over a decade. Heavyweight API’s tend to be smaller and thinner wrappers around the native OS code. For the developer that means running more into OS differences since the framework doesn’t really hide them from you.

Layout, theming, translation and localization are all really hard to do in a portable way. Heavyweight frameworks make you copy data to native OS structures and formats. They make you maintain images in the native OS locations which is radically different between iOS and Android. In lightweight framework this is all portable.

The tools of lightweight frameworks are generally all inclusive which is usually not the case for heavyweight frameworks. This isn’t always true for all cases but it’s a very common concept. This becomes obvious with hardware requirements where heavyweight frameworks require a Mac for iOS development. That isn’t true for Codename One. The cloud build works thanks to the lightweight architecture as the simulator can have better fidelity.

So lets compare concrete advantages of lightweight vs. heavyweight.  
Lightweight allows for extreme portability where what you see in the desktop simulator becomes a closer facsimile of what you will see in the device.

It also gives you far more control as most of the code is written in Java you can override almost any behavior in a very portable way. So lightweight is far more customizable. The flip side is that heavyweight can be more performant in some cases. That’s debatable as performance is a very complex beast. I find that caching is by far the most important performance optimization and it’s also very portable. Usually code that is slow on Android will be slow on iOS so a smart optimization will work well for a lightweight framework.

That essentially means you get a more consistent result overall. Bugs are very portable too and so a fix in iOS will often fix an issue for the Android version as well. The heavyweight widget approach will have better consistency with the native OS. For instance in a Samsung customized device the buttons might match Samsungs styling more closely. I’m not sure that’s an advantage. When I program my button to have a specific color I want it to keep that color and behavior. I don’t want a device manufacturer or OS vendor to change the look of my application after the fact.

In general lightweight frameworks are MUCH easier to use as they are simpler. In some cases native code in the heavyweight approach is easier but this isn’t as true with Codename One where heavyweight widgets can be integrated with lightweight widgets… Codename One is still a lightweight framework but if you need a complex heavyweight widget such as Google Maps you can just embed it into a Codename One application and it will work across platforms consistently!

Which brings us to this. Codename One supports native interfaces, that allows you to invoke native code from Java and that native code can create an Android view or iOS UIView etc which you can just add into the component hierarchy.

One of the cool things we allow is z-ordering of native widgets which means you can add a google map and add lightweight components that reside on top of that map such as components representing cars, landmarks etc.

The Codename One lightweight API is VERY performant. E.g in iOS it is implemented on top of Open GL.

The lightweight API is VERY customizable, you can literally override the paint behavior of every component or place a drawing region and just draw on top of everything…

Thanks for baring with me. I hope this was helpful and informative.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **marksluser** — January 1, 2021 at 11:24 pm ([permalink](https://www.codenameone.com/blog/tutorial-what-is-codename-one.html#comment-24370))

> marksluser says:
>
> Great blog post.
>
> Very in-depth and description of how codename one works. I like the comparison of lightweight/heavyweight widgets and how those approaches affect the entire development process. Its also great that Codename One can still run a native widget. I also like Codename Ones approach to developing and building on the iOS platform, its a pain.  
> I come from a desktop Java swing background and I have been looking at developing 2 different applications:  
> -an desktop file/database exploring app (I first considered Eclipse SWT)  
> -an mobile social media app (I am considering React Native)
>
> I looks like I could develop both of these in Codename One and still get performat access to the local file system and other native apis like the camera.
>
> One question, can I build an application in Codename One that will run as a desktop app in Linux GTK+ ? I only saw MacOs and Windows listed as native desktop apps.
>
> Thank you,
>
> -Mark
>



### **Shai Almog** — January 2, 2021 at 5:54 am ([permalink](https://www.codenameone.com/blog/tutorial-what-is-codename-one.html#comment-24371))

> Shai Almog says:
>
> We don’t have a standard build dedicated to that although it’s technically trivial.
>
> You can generate a JAR file relatively easily which will work for “other” desktop platforms. Packaging for Linux is a bit more challenging since there are so many potential targets, dependencies etc.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
