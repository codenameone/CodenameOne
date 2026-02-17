---
title: Codename One 3.7 is Live
slug: codename-one-3-7-live
url: /blog/codename-one-3-7-live/
original_url: https://www.codenameone.com/blog/codename-one-3-7-live.html
aliases:
- /blog/codename-one-3-7-live.html
date: '2017-06-26'
author: Shai Almog
---

![Header Image](/blog/codename-one-3-7-live/codenameone-3-7.jpg)

[Codename One](/) 3.7, the "Write Once Run Anywhere" mobile solution for Java developers is now live!  
This exciting new release brings with it a surprising new overhaul of the Codename One GUI builder which now includes support to "auto-layout" allowing developers to place components with the ease/power of Photoshop or Illustrator..

Codename One is the only platform that…​

  * Has Write Once Run Anywhere with no special hardware requirements and 100% code reuse

  * Compiles Java into native code for iOS, UWP (Universal Windows Platform), Android & even JavaScript

  * Is Open Source & Free with an enterprise grade commercial offering

  * Is Easy to use with 100% portable Drag & Drop GUI builder

  * Has Full access to underlying native OS capabilities using the native OS programming language (e.g. Objective-C) without compromising portability

  * Lets you use native widgets (views) and mix them with Codename One components within the same hierarchy (heavyweight/lightweight mixing)

To learn more about Codename One check out the [about page](/about-us/) you can [download it for free right now](/getting-started/).

### From Lowlight to Feature

When we released Codename One 3.6 we had this to say:

> The one thing we really didn’t get out in this release is a new Codename One course which we’ve been working on for a while. We hope we’ll get it done during the 3.7 era but video production is always a big effort and we just don’t have enough hours in the day…​

— Codename One 3.6 release announcement 

Yesterday we [announced the new Codename One Academy](/blog/launching-codename-one-academy.html) which includes 3 new media rich courses that will cover everything needed to build gorgeous real world apps. This is new original materials with a 2 year monthly update commitment roadmap!

As is our custom with some releases we are also running a promotion on these courses and our [special offer expires on July 3rd](/blog/launching-codename-one-academy.html) so hurry up!

### Highlights of this Release

Here are the top 5 features of this release illustrated in this short video, check out further details below.

  * **GUI Builder Auto Layout** – We now support an inset based GUI builder mode that allows positioning components in a far more fluid way. We are still working on tutorials for this new mode and it’s highly experimental but you can start using it right now!  
In addition to that we made numerous enhancements to the UX of the GUI builder further refining it

  * **Live Style Customization** – The UI design of the application can be [customized directly from the simulator](/blog/edit-styles-simulator.html)

  * **Z-Ordering on All OS’s** – You can mix native components e.g. maps, video. With Codename One components and draw on top of them

  * **Properties SQL & UI Binding** – Support for database mapping (ORM), parsing & UI binding/generation seamlessly using the properties API

  * **Rest API for Terse Networking** – New builder style REST API that abstracts some of the verbosity of `ConnectionRequest`

  * **VM API Enhancements** – Added some core VM classes and API’s such as [java.lang.Number](https://github.com/codenameone/CodenameOne/issues/1783), `CharSequence`, [exception chaining](https://github.com/codenameone/CodenameOne/issues/1991) etc.

  * **Terse Syntax** – New [CN API](/blog/static-global-context.html) allows developers to write more concise code, new helper API’s such as `addAll` & a jquery style [component selector](/blog/jquery-css-style-selectors-for-cn1.html)

  * **Security Oriented API’s** – New API’s for detecting certificates on https servers as well as API’s for touch ID, jailbreak detection and [more](/manual/security.html)

  * **Thread Helper & Threadsafe SQLite API’s** – A new API for communicating with threads & a new [threadsafe wrapper for sqlite](/blog/threadsafe-sqlite.html)

  * **Better Desktop/Web API’s** – We now support API’s such as mouse cursor customization, split pane, mouse hover events etc. which allow more elaborate desktop apps. We also include an experimental new "desktop skin" to debug desktop apps

  * **Experimental "On Top" Sidemenu & Form Layered Pane** – Side menu can now be [on top](/blog/sidemenu-on-top.html) of the UI and potentially on top of the entire form with a new layered pane mode

  * **ParparVM Performance Improvements** – Some of the code such as method invocations will now compile to the C equivalent of that code in terms of performance, many basic numeric operations are much faster thanks to VM optimizations and code is up to 40% smaller over 3.6

  * **Two Factor Authentication in Certificate Wizard** – The certificate wizard now supports 2 factor authentication in your apple account

  * **Faster iOS Builds** – Build times in the Apple servers are up to x3 faster for some users with exceptionally long build times.

  * **Test Recorder & Toolbar** – The test recorder now [works correctly with the toolbar](/blog/test-recorder-toolbar.html)

### Lowlights

As we always do with a release we’d like to shine a spotlight on the things 3.7 can do better and the things 3.8 can improve on. Overall we are thrilled with this release but here are a few things we can do better:

  * On device debugging – this was planned for 3.7 but didn’t make it. We have a running proof of concept but that also highlights the amount of work needed to bring this to production grade. Since 3.8 is relatively close by it’s hard for us to say if it will be a feature of 3.8.

  * Improved default themes & material design – this is an area we need to spend more time on. We are attacking it one component at a time but that is often challenging. We can use your help in filing issues and pull requests to improve the situation. If you see something that doesn’t look good or doesn’t look native [go to the issue tracker & create a new issue](http://github.com/codenameone/CodenameOne/issues/new) in it include a screenshot of how it looks now and a screenshot of how it should look. This helps us focus on the things that are important to our users. Even if we know about the problem an issue helps us focus!

  * Theme & Localization – The new GUI builder is starting to take shape and it’s time to turn our attention to the other rolls that the old designer tool handles. We already made some improvements to styling but we can go further.

### Onwards to 3.8

We completed almost everything we wanted to do for 3.7, to be fair that’s not to hard as we delayed the release due to the bootcamp and that gave us extra time.

The next version will be out in mid November which already feels damn close by now so I’m not sure which ones of the lowlights above we’ll be able to address by then but those are our biggest priorities not necessarily in that order.

The one feature that we did announce for 3.8 is [Kotlin support](/blog/kotlin-wora-ios-iphone-windows-android.html). We ran a survey whose results included a bit of a surprise to us, it’s something I’ll discuss in an upcoming post.

### We Need your Help

[Spread the word](http://www.codenameone.com/blog/how-you-can-help-spread-codenameone.html), please let people know about us.

Sign up for enterprise accounts, besides the huge benefits of an enterprise account these are the guys that keep the lights on here and allow us to build Codename One. If your company can afford it please take the time and upgrade to enterprise, this will allow us to work on the things that are important for your company!

Thanks for reading this far and if you have any thoughts/suggestions of any kind please feel free to post below!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Dalvik** — June 27, 2017 at 1:11 pm ([permalink](/blog/codename-one-3-7-live/#comment-23423))

> Dalvik says:
>
> I’ve just tried the new GUI builder, at first it was a bit confusing as it felt a bit like I can put a component anywhere but then I watched the video and it "clicked".  
> I really like the drag and drop UI of the GUI builder, it’s pretty smooth… Was it built with JavaFX?


### **Shai Almog** — June 27, 2017 at 1:16 pm ([permalink](/blog/codename-one-3-7-live/#comment-24208))

> Shai Almog says:
>
> Thanks.  
> No, it’s actually written in Codename One… Just like the Codename One Settings. Just goes to show you what you can do with some attention to detail.


### **Mac Flanegan** — June 27, 2017 at 1:30 pm ([permalink](/blog/codename-one-3-7-live/#comment-23525))

> Mac Flanegan says:
>
> Congratulations on the updates of this new version!
>
> PS: In Delphi this feature is called "Anchors" what I think would be a more appropriate name.


### **Shai Almog** — June 27, 2017 at 1:37 pm ([permalink](/blog/codename-one-3-7-live/#comment-24149))

> Shai Almog says:
>
> Thanks!  
> Were anchors automatically placed and bound with percentage or millimeter units to other components?  
> I haven’t used it in years so I don’t recall…
>
> I was for constraints and in Java there is a Spring layout (which I glad he didn’t pick as Spring is already deeply used in Java).


### **Pugazhendi E** — June 27, 2017 at 1:46 pm ([permalink](/blog/codename-one-3-7-live/#comment-23217))

> Pugazhendi E says:
>
> Congrats 🙂


### **Mac Flanegan** — June 28, 2017 at 5:08 pm ([permalink](/blog/codename-one-3-7-live/#comment-23572))

> Mac Flanegan says:
>
> In Delphi this a more simple feature. The reference is "always" the parent component. But the idea is the same. Delphi has constraints too.
>
> AutoLayout reminds me the automatic arrangement of components, as it already exists in codename. So the suggestion to use Anchors. In my opinion Anchors says more about what this feature does.
>
> Before I see it working, I even thought: – but the codenameonde already does autolayout …
>
> But again, congratulations to the new features …


### **Shai Almog** — June 29, 2017 at 3:54 am ([permalink](/blog/codename-one-3-7-live/#comment-23580))

> Shai Almog says:
>
> Autolayout is a term used in iOS so we chose it to keep it familiar.


### **Francesco Leoni** — June 29, 2017 at 6:35 am ([permalink](/blog/codename-one-3-7-live/#comment-23589))

> Francesco Leoni says:
>
> Hi Shai, congratulations on the new release!
>
> Is it possible to open an old Form (created using the previous GUuiBuilder) with the new GuiBuilder, or to convert an old Form to the new format?  
> At present I can use the new GB interface only wher I create a brand new Form.


### **Shai Almog** — June 29, 2017 at 8:56 am ([permalink](/blog/codename-one-3-7-live/#comment-23459))

> Shai Almog says:
>
> Thanks!
>
> Right now there are 3 modes:  
> – Old GUI builder – that’s the old designer tool where we still have theming. That’s a completely separate tool with the state machine etc.  
> You can use the conversion tool command line as explained here: [https://www.codenameone.com…](</blog/using-designer-command-line-options/>) to convert that old GUI to the new GUI builder. Notice that it will not take advantage of the new auto-layout…
>
> – New GUI builder code created pre-3.7. These forms have auto-layout off by default for maximum compatibility as this might break some existing logic.
>
> – New GUI builder forms created with autolayout
>
> If you are referring to converting the GUI builder code pre-3.7 to post 3.7 you can easily do that but it will require a bit of hacking. Just open the .gui XML file and set the layout of the form to be layered layout. Then set autoLayout="true" in the top level component tag. This will enable the new layout mode on an older form. Everything might be "messed up" after you do that which might require some work of fixing.


### **Francesco Leoni** — June 29, 2017 at 7:55 pm ([permalink](/blog/codename-one-3-7-live/#comment-23636))

> Francesco Leoni says:
>
> Thankyou so much for your reply. Unfortunately that didn’t do the trick: after modifying the top level component like this (see below), the GUI builder is not starting at all.
>
> component type="Form" layout="LayeredLayout" title="MyApp" name="GuiComponent" autoLayout="true">
>
> Lauching Gui builder from commandline I can see some null pointer exceptions like the one below:
>
> [EDT] 0:0:0,316 – Exception: java.lang.NullPointerException – null  
> java.lang.NullPointerException  
> at com.codename1.ui.layouts.LayeredLayout$LayeredLayoutConstraint.setInsets([LayeredLayout.java](<http://LayeredLayout.java>):1871)  
> at com.codename1.apps.guibuilder.GuiPersister.createComponent([GuiPersister.java](<http://GuiPersister.java>):282)  
> at com.codename1.apps.guibuilder.GuiPersister.load([GuiPersister.java](<http://GuiPersister.java>):99)  
> at com.codename1.apps.guibuilder.GUIBuilder.connected([GUIBuilder.java](<http://GUIBuilder.java>):76)  
> at com.codename1.apps.guibuilder.GUIBuilder.start([GUIBuilder.java](<http://GUIBuilder.java>):107)  
> at com.codename1.apps.guibuilder.desktop.GUIBuilderMain$[11.run](<http://11.run)([GUIBuilderMain.java](http://GUIBuilderMain.java)>:427)  
> at com.codename1.ui.Display.processSerialCalls([Display.java](<http://Display.java>):1056)  
> at com.codename1.ui.Display.mainEDTLoop([Display.java](<http://Display.java>):873)  
> at [com.codename1.ui.RunnableWr…](<http://com.codename1.ui.RunnableWrapper.run)([RunnableWrapper.java](http://RunnableWrapper.java)>:120)  
> at [com.codename1.impl.Codename…](<http://com.codename1.impl.CodenameOneThread.run)([CodenameOneThread.java](http://CodenameOneThread.java)>:176)


### **Shai Almog** — June 30, 2017 at 3:59 am ([permalink](/blog/codename-one-3-7-live/#comment-23349))

> Shai Almog says:
>
> Sorry, it seems I remembered this incorrectly and the attribute should be autolayout lower case not camel case.


### **Francesco Galgani** — June 30, 2017 at 11:27 am ([permalink](/blog/codename-one-3-7-live/#comment-23376))

> Francesco Galgani says:
>
> Are these new features (in particular the new Gui Builder) covered in the three courses of the Codename One Academy?


### **Shai Almog** — June 30, 2017 at 12:46 pm ([permalink](/blog/codename-one-3-7-live/#comment-23518))

> Shai Almog says:
>
> Some of them are, some are coming soon and some are not.  
> In particular the GUI builder should be coming soon.
>
> I started working on a module but it ended up being too problematic to document a tool that’s changing so quickly (case in point here). Hopefully this marks a stable point where I can start working on a GUI builder module which will hopefully be the first module I add after finishing this work.
>
> I’m considering building parts of the Uber app using the GUI builder but I’m not sure if it will work well with that use case.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
