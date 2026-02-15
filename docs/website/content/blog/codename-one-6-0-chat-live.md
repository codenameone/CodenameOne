---
title: Codename One 6.0 "Chat" is now Live
slug: codename-one-6-0-chat-live
url: /blog/codename-one-6-0-chat-live/
original_url: https://www.codenameone.com/blog/codename-one-6-0-chat-live.html
aliases:
- /blog/codename-one-6-0-chat-live.html
date: '2019-02-26'
author: Shai Almog
---

![Header Image](/blog/codename-one-6-0-chat-live/codenameone-6-release-banner.jpg)

We are thrilled to announce the release of [Codename One](https://www.codenameone.com/) 6.0 – Chat. Codename One is an open source “Write Once Run Anywhere” mobile platform for Java and Kotlin developers!  
With this release we introduced [Codename One Build](https://cloud.codenameone.com/buildapp/index.html) which is one of the biggest overhauls to the Codename One workflow since its inception. We also refined and updated many underlying technologies e.g. the xcode 10.1 migration, `WKWebView` support, push replies, badges on Android and much more.

You can check out the details below for the full review but first if you are new to Codename One here’s a short primer. Codename One is the only platform that:

  * Has **Write Once Run Anywhere** with no special hardware requirements and 100% code reuse

  * Compiles **Java** or **Kotlin** into native code for iOS, UWP (Universal Windows Platform), Android & even JavaScript

  * Is **Open Source & Free for commercial use** with an enterprise grade commercial support

  * Is Easy to use with **100% portable Drag & Drop GUI builder**

  * Has Full access to underlying native OS capabilities using the **native** OS programming language (e.g. Objective-C) without compromising portability

  * Has full **control over every pixel** on the screen! Just override paint and draw or use a glass pane to draw anywhere…​

  * Lets you **use native widgets (views) and mix them with Codename One components** within the same hierarchy (heavyweight/lightweight mixing)

  * Supports **seamless Continuous Integration** out of the box

To learn more about Codename One check out the [about page](/about/) you can [download it for free right now](/download/).

Version 6.0 is nicknamed Chat because of the WhatsApp Clone application that was developed with it for [the online course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) in the Codename One Academy.

### Highlights of this Release

The top 5 features of this release are covered in this short video, check out further details below…​

  * **Codename One Build** — we can now monitor builds from [Android](https://play.google.com/store/apps/details?id=com.codename1.build.app) and [iOS](/blog/build-app-on-ios/). The app is also available for every device through [web PWA](https://cloud.codenameone.com/buildapp/index.html). It works with push notification and is built with Codename One!

__ |  Currently the iOS version is still in beta due to the tedious appstore approval process   
---|---  
  
  * **xcode 10.1 Migration** — builds on the Codename One cloud implicitly use [xcode 10.1](/blog/xcode-10-1-migration-phase-2/). We migrated from xcode 9.2 to satisfy Apples requirements, this has been seamless for [the most part](/blog/file-chooser-xcode-10/)

  * **VM Changes** — we now support `java.util.Objects` and some additional [methods from Class](/blog/vm-enhancments-full-screen-xml/)

  * **New Switch API** — [Switch](/blog/switch-progress-pull/) replaces the old `OnOffSwitch` API which is pretty old by now

  * **Reply Push Notifications** — the final piece of [RFE 2208 Rich Push Notifications](https://github.com/codenameone/CodenameOne/issues/2208) is now [implemented](/blog/rich-push-notification-improved/). You can now prompt a user for a reply via a push message

  * **Support for Badges on Android** — we can now mark an Android icon with a [numeric badge](/blog/validate-owner-badges-imageviewer-picker-range/)

  * **Material Design Infinite Progress** — [InfiniteProgress](/blog/switch-progress-pull/) now has a material design mode that includes the custom circle animation we see in material design

  * **WKWebView Support** — Apple includes two implementations of a “WebView”. We now support [both](/blog/wkwebview/)

  * **CSS Improvements** — underline border is now [supported natively](https://github.com/codenameone/CodenameOne/commit/b6a85e5c8ab161405a18d51871b48df43744e806). Round rectangle is also [supported natively](https://github.com/codenameone/CodenameOne/commit/66c87a6f2896bed0b1ed2834860b230e4ec8648f) and lets you activate the angle only on [specific corners](https://github.com/codenameone/CodenameOne/commit/52172ba9297ccfefaeb9497fae39b3360edeea5d) as per [this RFE](https://github.com/codenameone/CodenameOne/issues/2350)

  * **Picker Improvements** — `Picker` now lets you define [start/end date](https://github.com/codenameone/CodenameOne/issues/2573)

  * **FontImage rotateAnimation** — `FontImage` lets you animate an icon so it [rotates infinitely](/blog/validation-regex-masking/) effectively making every component into an `InfiniteProgress`

  * **Added Ownership to Component Hierarchy** — [ownership](/blog/validate-owner-badges-imageviewer-picker-range/) allows us to create a relationship between components other than `Component` → `Container`

  * **Added Animation Safe Revalidate** — `revalidte()` is a powerful tool but if it’s invoked when an animation is in progress it might produce unpredictable behavior. [This method](/blog/validate-owner-badges-imageviewer-picker-range/) solves that problem

  * **Button Lists** — [List is discouraged](/blog/avoiding-lists/) but we still want lists that use a model to represent buttons, radio buttons and checkboxes [button lists](/blog/button-lists/) can fit in that niche

  * **XML Mapping in Properties** — this is still an experimental feature but [XML parsing/generating](/blog/vm-enhancments-full-screen-xml/) is now supported for `PropertyBusinessObject`

  * **PWA Install Prompt** — a new API lets us [install PWA’s](/blog/install-home-screen/) directly

  * **New Full Screen API** — the Desktop and JavaScript targets allow running the app in full screen mode by leveraging this [new API](/blog/vm-enhancments-full-screen-xml/)

  * **Facebook SDK Updated** — we updated the Facebook SDK to use the [latest version](https://github.com/codenameone/CodenameOne/issues/2641)

There are many other features both big and small. Check out our [blog](https://www.codenameone.com/blog/) and the github [project history](https://github.com/codenameone/CodenameOne/commits/master).

### Onwards to 7.0 – Video

We took a lot of time for 6.0 but I’m not sure if that’s enough. We might take longer to deliver 7.0. Currently the timeline is unchanged but we’ll have to see.

We will have a Netflix clone tutorial in the [Codename One Academy](https://codenameone.teachable.com/). Hence the moniker of the next release.

### We Need your Help

If you think we are doing a good job and appreciate our help please help us by:

  * [Spreading the word](/blog/how-you-can-help-spread-codenameone/)

  * [Edit our docs](/blog/tip-edit-docs-fun-profit/)

  * [Edit our sources and submit bug fixes](/blog/how-to-use-the-codename-one-sources/)

  * Or just sign up for enterprise accounts which literally keep the lights on here…​ If your company can afford it please take the time and upgrade to enterprise, this will allow us to work on the things that are important for your company!

Thanks for reading this far and if you have any thoughts/suggestions of any kind please let us know!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
