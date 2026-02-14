---
title: Codename One 3.8 is Live
slug: codename-one-3-8-live
url: /blog/codename-one-3-8-live/
original_url: https://www.codenameone.com/blog/codename-one-3-8-live.html
aliases:
- /blog/codename-one-3-8-live.html
date: '2017-11-13'
author: Shai Almog
---

![Header Image](/blog/codename-one-3-8-live/codenameone-3-8.jpg)

We are thrilled to announce the release of [Codename One](https://www.codenameone.com/) 3.8. Codename One is an open source “Write Once Run Anywhere” mobile solution for Java developers!  
This new release significantly refines the native look and feel of Codename One, it brings the GUI builder to a new level with styling support. It finally adds Mac OS appstore distribution support which means all the major appstores are now supported targets for Codename One applications.

Codename One is the only platform that…​

  * Has **Write Once Run Anywhere** with no special hardware requirements and 100% code reuse

  * Compiles **Java** or **Kotlin** into native code for iOS, UWP (Universal Windows Platform), Android & even JavaScript

  * Is **Open Source & Free for commercial use** with an enterprise grade commercial support

  * Is Easy to use with **100% portable Drag & Drop GUI builder**

  * Has Full access to underlying native OS capabilities using the **native** OS programming language (e.g. Objective-C) without compromising portability

  * Has full **control over every pixel** on the screen! Just override paint and draw or use a glass pane to draw anywhere…​

  * Lets you **use native widgets (views) and mix them with Codename One components** within the same hierarchy (heavyweight/lightweight mixing)

To learn more about Codename One check out the [about page](https://www.codenameone.com/about.html) you can [download it for free right now](https://www.codenameone.com/download.html).

As part of the release we significantly refined our developer guide which is now also available in [print form on Amazon](https://www.amazon.com/dp/1549910035). Notice that this guide is available for free [here](https://www.codenameone.com/manual/) & in [pdf format](https://www.codenameone.com/files/developer-guide.pdf). This developer guide is a community effort which you can contribute to as explained [here](https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html).

### Highlights of this Release

The top 5 features of this release are covered in this short video, check out further details below…​

  * **Improved Native Look & Feel** – We changed the core look of [buttons](https://www.codenameone.com/blog/pixel-perfect-material-buttons-part-2.html), [labels](https://www.codenameone.com/blog/pixel-perfect-material-buttons.html), [text components](https://www.codenameone.com/blog/pixel-perfect-text-input-part-2.html), [ripple effect](https://www.codenameone.com/blog/factional-padding-margin-rounded-border-ripple-caps-google-connect.html) and more. The goal is to make Codename One applications indistinguishable from native OS apps out of the box

![Before: Codename One 3.7 text Input \(on Android\)](/blog/codename-one-3-8-live/pixel-perfect-text-field-android-codenameone-before.png)

Figure 1. Before: Codename One 3.7 text Input (on Android)

![After: Codename One 3.8 text Input \(on Android\)](/blog/codename-one-3-8-live/pixel-perfect-text-field-android-codenameone-font.png)

Figure 2. After: Codename One 3.8 text Input (on Android)

  * **Kotlin Support** – [Kotlin](https://www.codenameone.com/blog/kotlin-support-public-beta.html) is now officially supported by Codename One and works out of the box

  * **On Top Side Menu** – The [on top side menu](https://www.codenameone.com/blog/pixel-perfect-on-top-menu.html) adapts the side menu UI to render on-top of the application instead of below but it’s really a complete rewrite of the old `SideMenuBar` which was implemented in a problematic way. The new on-top mode works better with native peers such as maps and can be extended more easily

  * **GUI Builder Styling Support** – There are a lot of enhancements and refinements in the new GUI builder one of the big ticket features is the new [style UI](https://www.codenameone.com/blog/always-on-top-style-parser.html) which allows you to style an element without leaving the GUI builder

  * **Mac OS Appstore Support** – We now support building [signed Mac OS apps](https://www.codenameone.com/blog/mac-appstore-builds-device-farms.html) which means we now support all the major vendor appstores. We already support iOS/Android stores and Windows/Microsoft’s store (via the UWP port). The Mac appstore was the last major vendor whose store we didn’t support out of the box

  * **Signal Handling & Fast UTF in ParparVM** – [ParparVM](https://github.com/codenameone/CodenameOne/tree/master/vm) is our open source iOS VM. It now [handles low level OS signals](https://www.codenameone.com/blog/vm-changes-updates.html) to catch illegal access and convert it to Java exceptions. This means performance is slightly better but more importantly: you can catch errors even when they originate from native code. We also made significant improvements to the UTF-8 decoding logic which should make apps that rely on localized data faster and more memory efficient

  * **Theme Enhancements** – We added many new capabilities into the Codename One themes specifically: [Fractional padding/margin](https://www.codenameone.com/blog/factional-padding-margin-rounded-border-ripple-caps-google-connect.html), [Rounded border](https://www.codenameone.com/blog/factional-padding-margin-rounded-border-ripple-caps-google-connect.html), [Underline borders](https://www.codenameone.com/blog/millimeter-underline.html) & more

  * **Table Sorting** – You can now [sort](https://www.codenameone.com/blog/sortable-table.html) a table by clicking on the column header

There are many other features both big and small. Check out our [blog](https://www.codenameone.com/blog/) and the github [project history](https://github.com/codenameone/CodenameOne/commits/master).

### Lowlights

As we always do with a release we’d like to shine a spotlight on the things this version could do better and the things the next version can improve. Overall we are thrilled with this release but here are a few things we can do better:

  * On device debugging – this was planned before for 3.7 but didn’t make it. We have a running proof of concept but that also highlights the amount of work needed to bring this to production grade. We didn’t think it will make it for 3.8 and I’m not optimistic about 4.0 with our current workload. We think this will be a great enhancement but right now we think theming is more important

  * Improved default themes & material design – we made huge strides in this area but we are still way behind and our demos still don’t reflect the progress we made. Hopefully by the time 4.0 rolls around we’ll be in a different place entirely

  * Theme & Localization – Steve added some better theming to the new GUI builder. We think we can improve on this further and generally improve theming. Localization is something that regressed a bit from the old GUI builder which allowed for great automatic localization. We need something more “seamless” in this department

### Onwards to 4.0

We have way more time for the 4.0 release so we can probably fit in more things than we did in 3.8. One of the difficulties in 3.8 is that a lot of the time between 3.7 and 3.8 was spent in summer months that are less productive. We fully expect 4.0 to be far richer in terms of features.

By the time 4.0 rolls around we should have two new major demos/tutorials in the [Codename One Academy](https://codenameone.teachable.com/).

  * The Uber style application

  * A social network style application

We’ve already laid some ground work for the Uber style app and we plan to push it out before the end of the year. This continues the 3 major trends we are trying to drive:

  * Better design

  * Better docs

  * More “ready made templates”

Another big focus which we’ll see in 4.0 is quality and continuous integration. Our QA process is now open as part of our continuous integration support. We are now running automated tests of all our commits on device farms which should make future versions of Codename One far more stable.

### We Need your Help

We got a record number of community pull requests during the 3.8 timeline, that is fantastic!

If you think we are doing a good job and appreciate our help please help us by:

  * [Spreading the word](https://www.codenameone.com/blog/how-you-can-help-spread-codenameone.html)

  * [Edit our docs](https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html)

  * [Edit our sources and submit bug fixes](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html)

  * Or just sign up for enterprise accounts which literally keep the lights on here…​ If your company can afford it please take the time and upgrade to enterprise, this will allow us to work on the things that are important for your company!

Thanks for reading this far and if you have any thoughts/suggestions of any kind please let us know!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Dalvik** — November 14, 2017 at 2:33 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23539))

> Great video, loved the narration…
>
> I really liked the new look and feel changes at least based on the screenshots, I need to adapt some of my code/themes to use this. I’m personally pretty excited about a better TDD workflow. I found this a bit hard to do up to now.
>



### **Francesco Galgani** — November 15, 2017 at 1:58 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23876))

> What’s code you used to generate the “Figure 2. After: Codename One 3.8 text Input (on Android)”? Is it an Instant UI generated using Properties?
>



### **James** — November 16, 2017 at 4:18 am ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23748))

> James says:
>
> Is there a plugin update for Eclipse? When I run Check For Updates, it lists a 3.8 update but then fails to install and says the repository can’t be found. If I look in Eclipse Marketplace, I see 3.7 as the latest version.
>



### **Shai Almog** — November 16, 2017 at 5:28 am ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23885))

> Shai Almog says:
>
> That code is discussed here: [https://www.codenameone.com…](<https://www.codenameone.com/blog/pixel-perfect-text-input-part-2.html>)
>
> It’s also in the new developer guide update.
>



### **Shai Almog** — November 16, 2017 at 5:29 am ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23616))

> Shai Almog says:
>
> Thanks I do need to update the eclipse marketplace listing. It should work for the update. What’s your update URL? What’s the error? What’s your eclipse version?
>



### **James** — November 18, 2017 at 5:21 am ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23680))

> James says:
>
> I’m using Eclipse Version: Oxygen.1a (4.7.1a), Build id: M20171009-0410. The error I get is shown below. I went to the Marketplace and saw 3.8 listed and found a button there that said Update. I’m now updated to 3.8.0.
>
> An error occurred while collecting items to be installed  
> session context was:(profile=SDKProfile, phase=org.eclipse.equinox.internal.p2.engine.phases.Collect, operand=, action=).  
> No repository found containing: osgi.bundle,CodenameOnePlugin,3.8.0  
> No repository found containing: org.eclipse.update.feature,CodenameOneFeature,3.8.0
>



### **Shai Almog** — November 18, 2017 at 5:34 am ([permalink](https://www.codenameone.com/blog/codename-one-3-8-live.html#comment-23567))

> Shai Almog says:
>
> Odd. I have Oxygen installed and I installed via the marketplace too. It worked as expected for me. Are you behind a proxy by any chance?  
> If you try to install manually by using the update site: [https://www.codenameone.com…](<https://www.codenameone.com/files/eclipse/site.xml>) does it work?  
> Do you maybe have an older Codename One update site imported from an older version of eclipse?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
