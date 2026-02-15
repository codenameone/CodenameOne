---
title: Codename One 4.0 "Taxi" is now Live
slug: codename-one-4-0-taxi-live
url: /blog/codename-one-4-0-taxi-live/
original_url: https://www.codenameone.com/blog/codename-one-4-0-taxi-live.html
aliases:
- /blog/codename-one-4-0-taxi-live.html
date: '2018-03-19'
author: Shai Almog
---

![Header Image](/blog/codename-one-4-0-taxi-live/codenameone-4-0-release-image-taxi.jpg)

We are thrilled to announce the release of [Codename One](https://www.codenameone.com/) 4.0 – Taxi. Codename One is an open source “Write Once Run Anywhere” mobile solution for Java & Kotlin developers!  
This new release overhauled the way Codename One is updated, added support for Progressive Web Apps (PWA’s), overhauled device skins & updated the backend iOS build tools. A major focus of this release is better support for peer (native) components, stability, unit testing and continuous integration.

Codename One is the only platform that…​

  * Has **Write Once Run Anywhere** with no special hardware requirements and 100% code reuse

  * Compiles **Java** or **Kotlin** into native code for iOS, UWP (Universal Windows Platform), Android & even JavaScript

  * Is **Open Source & Free for commercial use** with an enterprise grade commercial support

  * Is Easy to use with **100% portable Drag & Drop GUI builder**

  * Has Full access to underlying native OS capabilities using the **native** OS programming language (e.g. Objective-C) without compromising portability

  * Has full **control over every pixel** on the screen! Just override paint and draw or use a glass pane to draw anywhere…​

  * Lets you **use native widgets (views) and mix them with Codename One components** within the same hierarchy (heavyweight/lightweight mixing)

  * Supports **seamless Continuous Integration** out of the box

To learn more about Codename One check out the [about page](/about/) you can [download it for free right now](/download/).

Version 4.0 is nicknamed Taxi because of the Uber Clone application that was developed with it for [the online course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) in the Codename One Academy.

![Uber sidemenu next to the clone](/blog/codename-one-4-0-taxi-live/uber-app-side-menu-thumb.png)

Figure 1. Uber sidemenu next to the clone

![The Uber login form next to the clone](/blog/codename-one-4-0-taxi-live/side-by-side-thumb.png)

Figure 2. The Uber login form next to the clone

### Highlights of this Release

The top 5 features of this release are covered in this short video, check out further details below…​

  * **Progressive Web App Support (PWA)** – Progressive Web Apps allow us to try an application on the web and seamlessly transition to a native app. This makes user acquisition easier and installation frictionless. Codename One is the only tool in the world that [supports PWA’s seamlessly](/blog/progressive-web-apps/)

  * **New Device Skins** – We updated the look of Codename One by releasing [33 new device skins](/blog/new-skins-san-francisco-font/) including iPhone X & Pixel 2 XL. We included support for non-rectangular device skins and better device fidelity. We also added the ability to grab a screenshot that includes the skin frame around it

  * **Xcode 9.2** – Codename One apps are built using xcode 9.2. This [change is seamless](/blog/xcode-9-on-by-default/) for most developers as the update happened on the build servers. Xcode 9.2 requires additional permission messages which are [added automatically by the simulator](/blog/xcode-9-mode/)

  * **Update Framework** – Updates to Codename One libraries are now delivered using a [unified framework](/blog/new-update-framework/) instead of separate adhoc tools

  * **Continuous Integration Support** – We now support [Travis CI](/blog/travis-ci-integration/) out of the box seamlessly. Adding support for additional CI tools should be just as easy

  * **New Async JavaScript Interop API** – The Java → JavaScript bridge with the embeddable browser component was completely replaced. The [new implementation](/blog/new-async-java-javascript-interop-api/) should be faster than the old system

  * **Builtin Unit Tests** – Unit tests to Codename One are [integrated into the core repository](/blog/updates-holidays/) and are executed with every commit

  * **Improved Peer Components** – Multiple bugs and minor issues were fixed in the peer component layer this effectively enabled the Uber clone to [work properly with the native map](/blog/map-layout-update/)

  * **Better Hello World** – The new Codename One projects [generate better code](/blog/new-default-code/) that handles things such as network errors more effectively

  * **GUI Builder Refinements** – There were many refinements to the new GUI builder most notably:

    * Improved support for layout nesting in auto-layout mode – you can use all the existing layout managers within an autolayout parent

    * New Window Manager allows you to customize the positioning of the windows & palettes

    * Tabs component is supported again

  * **Test Push In the Simulator** – The simulator now supports [testing push notification](/blog/meltdown-updates/)

There are many other features both big and small. Check out our [blog](https://www.codenameone.com/blog/) and the github [project history](https://github.com/codenameone/CodenameOne/commits/master).

### Lowlights

As we always do with a release we’d like to shine a spotlight on the things this version could do better and the things the next version can improve. Overall we are thrilled with this release but here are a few things we can do better:

  * On device debugging – I wasn’t optimistic about getting this out for 4.0 and I’m still not optimistic about 5.0. We already have a lot on our plate for 5.0 and this is a huge feature

  * Improved default themes & material design – we did a lot of work on the skins but didn’t move the native theme or make a separate material design theme. We need to do a lot of work on the default hello world applications to make them look great out of the door.

Overall while we implemented a lot of features in 4.0 we didn’t really address most of the problems we highlighted in this section when 3.8 was released. I’m not sure if we have enough time in the 5.0 cycle to improve that but hopefully we can at least move theming more aggressively again.

### Onwards to 5.0 – Social

The 5.0 release cycle is relatively short & we already have a lot of things planned for it.

We should have the new social app tutorial in the [Codename One Academy](https://codenameone.teachable.com/) which will cover cloning Facebook.

Check out our [survey results](/blog/survey-results-2018/) to see the future apps we’ll release into the academy. Even if you never plan to signup to the academy this should be interesting as it gives you a good notion of what can be built with Codename One.

Other than that we’ll try to launch better docs and designs. We’ve put a lot of effort into improving our design capabilities and one of the big things we’d like to pick up again is app templates. In the past we released a few free themes as Codename One stubs. We’d like to do that again so developers can start from “something”.

### We Need your Help

If you think we are doing a good job and appreciate our help please help us by:

  * [Spreading the word](/blog/how-you-can-help-spread-codenameone/)

  * [Edit our docs](/blog/tip-edit-docs-fun-profit/)

  * [Edit our sources and submit bug fixes](/blog/how-to-use-the-codename-one-sources/)

  * Or just sign up for enterprise accounts which literally keep the lights on here…​ If your company can afford it please take the time and upgrade to enterprise, this will allow us to work on the things that are important for your company!

Thanks for reading this far and if you have any thoughts/suggestions of any kind please let us know!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Dalvik** — March 20, 2018 at 10:58 am ([permalink](/blog/codename-one-4-0-taxi-live/#comment-23631))

> Congratulations, I hadn’t noticed half of these features as they went in 😉


### **Bluewater** — March 22, 2018 at 6:05 am ([permalink](/blog/codename-one-4-0-taxi-live/#comment-24138))

> Nice work! Were any of the deprecated APIs removed or is 4.0 fully backward compatible with 3x code?


### **Shai Almog** — March 22, 2018 at 8:43 am ([permalink](/blog/codename-one-4-0-taxi-live/#comment-23817))

> Shai Almog says:
>
> Thanks!  
> It should be compatible. We didn’t remove any deprecated API’s but there are changes and some things can always break.


### **Ross Taylor** — March 31, 2018 at 10:31 am ([permalink](/blog/codename-one-4-0-taxi-live/#comment-23743))

> Ross Taylor says:
>
> Towards around 1:57 – 59 of the video, I notice a slight flicker of the menu when displayed over the map. Is this a problem of the Google Map you were referring?


### **Shai Almog** — April 1, 2018 at 4:07 am ([permalink](/blog/codename-one-4-0-taxi-live/#comment-23724))

> Shai Almog says:
>
> That’s related to the incoming animation. I used a background painter in one of the elements and I think I have an issue in that.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
