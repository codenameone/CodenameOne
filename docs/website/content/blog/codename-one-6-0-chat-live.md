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

To learn more about Codename One check out the [about page](https://www.codenameone.com/about.html) you can [download it for free right now](https://www.codenameone.com/download.html).

Version 6.0 is nicknamed Chat because of the WhatsApp Clone application that was developed with it for [the online course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) in the Codename One Academy.

### Highlights of this Release

The top 5 features of this release are covered in this short video, check out further details below…​

  * **Codename One Build** — we can now monitor builds from [Android](https://play.google.com/store/apps/details?id=com.codename1.build.app) and [iOS](https://www.codenameone.com/blog/build-app-on-ios.html). The app is also available for every device through [web PWA](https://cloud.codenameone.com/buildapp/index.html). It works with push notification and is built with Codename One!

__ |  Currently the iOS version is still in beta due to the tedious appstore approval process   
---|---  
  
  * **xcode 10.1 Migration** — builds on the Codename One cloud implicitly use [xcode 10.1](https://www.codenameone.com/blog/xcode-10-1-migration-phase-2.html). We migrated from xcode 9.2 to satisfy Apples requirements, this has been seamless for [the most part](https://www.codenameone.com/blog/file-chooser-xcode-10.html)

  * **VM Changes** — we now support `java.util.Objects` and some additional [methods from Class](https://www.codenameone.com/blog/vm-enhancments-full-screen-xml.html)

  * **New Switch API** — [Switch](https://www.codenameone.com/blog/switch-progress-pull.html) replaces the old `OnOffSwitch` API which is pretty old by now

  * **Reply Push Notifications** — the final piece of [RFE 2208 Rich Push Notifications](https://github.com/codenameone/CodenameOne/issues/2208) is now [implemented](https://www.codenameone.com/blog/rich-push-notification-improved.html). You can now prompt a user for a reply via a push message

  * **Support for Badges on Android** — we can now mark an Android icon with a [numeric badge](https://www.codenameone.com/blog/validate-owner-badges-imageviewer-picker-range.html)

  * **Material Design Infinite Progress** — [InfiniteProgress](https://www.codenameone.com/blog/switch-progress-pull.html) now has a material design mode that includes the custom circle animation we see in material design

  * **WKWebView Support** — Apple includes two implementations of a “WebView”. We now support [both](https://www.codenameone.com/blog/wkwebview.html)

  * **CSS Improvements** — underline border is now [supported natively](https://github.com/codenameone/CodenameOne/commit/b6a85e5c8ab161405a18d51871b48df43744e806). Round rectangle is also [supported natively](https://github.com/codenameone/CodenameOne/commit/66c87a6f2896bed0b1ed2834860b230e4ec8648f) and lets you activate the angle only on [specific corners](https://github.com/codenameone/CodenameOne/commit/52172ba9297ccfefaeb9497fae39b3360edeea5d) as per [this RFE](https://github.com/codenameone/CodenameOne/issues/2350)

  * **Picker Improvements** — `Picker` now lets you define [start/end date](https://github.com/codenameone/CodenameOne/issues/2573)

  * **FontImage rotateAnimation** — `FontImage` lets you animate an icon so it [rotates infinitely](https://www.codenameone.com/blog/validation-regex-masking.html) effectively making every component into an `InfiniteProgress`

  * **Added Ownership to Component Hierarchy** — [ownership](https://www.codenameone.com/blog/validate-owner-badges-imageviewer-picker-range.html) allows us to create a relationship between components other than `Component` → `Container`

  * **Added Animation Safe Revalidate** — `revalidte()` is a powerful tool but if it’s invoked when an animation is in progress it might produce unpredictable behavior. [This method](https://www.codenameone.com/blog/validate-owner-badges-imageviewer-picker-range.html) solves that problem

  * **Button Lists** — [List is discouraged](https://www.codenameone.com/blog/avoiding-lists.html) but we still want lists that use a model to represent buttons, radio buttons and checkboxes [button lists](https://www.codenameone.com/blog/button-lists.html) can fit in that niche

  * **XML Mapping in Properties** — this is still an experimental feature but [XML parsing/generating](https://www.codenameone.com/blog/vm-enhancments-full-screen-xml.html) is now supported for `PropertyBusinessObject`

  * **PWA Install Prompt** — a new API lets us [install PWA’s](https://www.codenameone.com/blog/install-home-screen.html) directly

  * **New Full Screen API** — the Desktop and JavaScript targets allow running the app in full screen mode by leveraging this [new API](https://www.codenameone.com/blog/vm-enhancments-full-screen-xml.html)

  * **Facebook SDK Updated** — we updated the Facebook SDK to use the [latest version](https://github.com/codenameone/CodenameOne/issues/2641)

There are many other features both big and small. Check out our [blog](https://www.codenameone.com/blog/) and the github [project history](https://github.com/codenameone/CodenameOne/commits/master).

### Onwards to 7.0 – Video

We took a lot of time for 6.0 but I’m not sure if that’s enough. We might take longer to deliver 7.0. Currently the timeline is unchanged but we’ll have to see.

We will have a Netflix clone tutorial in the [Codename One Academy](https://codenameone.teachable.com/). Hence the moniker of the next release.

### We Need your Help

If you think we are doing a good job and appreciate our help please help us by:

  * [Spreading the word](https://www.codenameone.com/blog/how-you-can-help-spread-codenameone.html)

  * [Edit our docs](https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html)

  * [Edit our sources and submit bug fixes](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html)

  * Or just sign up for enterprise accounts which literally keep the lights on here…​ If your company can afford it please take the time and upgrade to enterprise, this will allow us to work on the things that are important for your company!

Thanks for reading this far and if you have any thoughts/suggestions of any kind please let us know!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
