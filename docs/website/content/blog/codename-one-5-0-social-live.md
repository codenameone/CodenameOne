---
title: Codename One 5.0 "Social" is now Live
slug: codename-one-5-0-social-live
url: /blog/codename-one-5-0-social-live/
original_url: https://www.codenameone.com/blog/codename-one-5-0-social-live.html
aliases:
- /blog/codename-one-5-0-social-live.html
date: '2018-09-18'
author: Shai Almog
---

![Header Image](/blog/codename-one-5-0-social-live/codenameone-5-release-banner.jpg)

We are thrilled to announce the release of [Codename One](https://www.codenameone.com/) 5.0 – Social. Codename One is an open source “Write Once Run Anywhere” mobile platform for Java and Kotlin developers!  
We postponed the release of this version since it’s so packed with big changes. We made CSS a first class citizen in Codename One and made CSS updates live (no recompile necessary). We moved from screenshots in iOS launches to storyboards. Added support for newer JDK’s. Migrated to Android API level 27. Moved our entire build server infrastructure. Redid push notification and so much more…​

There is SO MUCH more, check out the details below.

However if you are new to Codename One here’s a short primer. Codename One is the only platform that…​

  * Has **Write Once Run Anywhere** with no special hardware requirements and 100% code reuse

  * Compiles **Java** or **Kotlin** into native code for iOS, UWP (Universal Windows Platform), Android & even JavaScript

  * Is **Open Source & Free for commercial use** with an enterprise grade commercial support

  * Is Easy to use with **100% portable Drag & Drop GUI builder**

  * Has Full access to underlying native OS capabilities using the **native** OS programming language (e.g. Objective-C) without compromising portability

  * Has full **control over every pixel** on the screen! Just override paint and draw or use a glass pane to draw anywhere…​

  * Lets you **use native widgets (views) and mix them with Codename One components** within the same hierarchy (heavyweight/lightweight mixing)

  * Supports **seamless Continuous Integration** out of the box

To learn more about Codename One check out the [about page](/about/) you can [download it for free right now](/download/).

Version 5.0 is nicknamed Social because of the Facebook Clone application that was developed with it for [the online course](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) in the Codename One Academy.

![Facebook Native App vs. our Clone](/blog/codename-one-5-0-social-live/login-screen-original-vs-clone-portrait.png)

Figure 1. Facebook Native App vs. our Clone

### Highlights of this Release

The top 5 features of this release are covered in this short video, check out further details below…​

  * **Live CSS Update** — CSS is now deeply and seamlessly integrated into Codename One. When you change the content of a CSS file and save the Codename One simulator [automatically updates on the fly](/blog/live-css-update/)

  * **Rich Push Notifications** — Push notification was overhauled, we moved the last of the functionality from GCM to FCM. We now support [rich push notifications](/blog/tich-push-notification-improved-validation/) that can include images and complex functionality

  * **Launch Screen Storyboards** — Historically iOS used screenshots of apps to fake fast application launches. Codename One automated that process in the past, it’s now discoraged by newer iOS features such as side-by-side multi-tasking. As such we now use [storyboard launch files](/blog/version-4-1-launch-screen-storyboards/). This allows side-by-side multi-tasking and as a bonus speeds up compilation while reducing the app size further

  * **New JDK/OpenJDK** Support — [We now support JDK’s 8 to 11](/blog/uber-book-is-out-jdk-11/) this includes OpenJDK

  * **New Cloud Servers** — We [migrated the last remaining Codename One servers off of Google App Engine](/blog/new-build-cloud/). This allowed us to introduce great new features such as [the ability to increase your free build quota](/blog/increase-your-build-quotas/)

  * **Removed Old IDE Preferences UI** — The old right click IDE preferences UI was causing a lot of confusion due to lack of maintenance. It’s [now gone](/blog/removing-old-preferences/) and replaced completely by Codename One Settings

  * **Android API Level 27** — We moved to [Android’s API Level 27](/blog/moving-to-27-facebook-clone-done/) by default. Since Google requires API level 26 or higher at this time. We’ll probably update API levels faster due to this policy

  * **Lightweight Picker** — The `Picker` component was rewritten as a [lightweight component](/blog/lightweight-picker-device-detection/) instead of a native one. This allows far more customization and cross platform consistency for one of our most problematic widgets

  * **Low Level Camera API** — [Camera Kit](/blog/camerakit-low-level-camera-api/) allows developers to access the native camera view to grab photos/videos and overlay graphics on top of the camera

  * **Pluggable Spatial SQLite** — [Spatial support for SQLite](/blog/spatial-pluggable-sqlite/) lets developers write complex location based applications. This functionality lets developers replace the existing native SQLite implementation with an arbitrary implementation which is very useful for enterprise grade features such as deep encryption, replication etc.

  * **Improved Map Layout** — The map API now includes a [native high performance component layout](/blog/map-component-positioning-revisited/) built in

  * **Landscape UIID’s** — Components can [adapt their UIID to landscape](/blog/ios-back-command-behavior/), this enables features such as smaller title font/padding in landscape mode

  * **Multiple Smaller Improvements** :

    * [DateUtil API](/blog/date-util/) for timezone related date functions

    * Sidemenu [right side option](/blog/right-sidemenu-tab-order/) and new tab ordering API

    * Ability to map a list of [property business objects to a table](/blog/table-property-mapping/)

    * [Sendgrid cn1lib](/blog/sendgrid-cn1lib/)

    * [Rest API overhaul](/blog/rest-api-error-handling/) for error handling, properties etc.

    * [On-device webserver](/blog/on-device-web-server/)

    * [Auto-reconnect](/blog/tip-auto-reconnect-web-socket/) for websockets

There are many other features both big and small. Check out our [blog](https://www.codenameone.com/blog/) and the github [project history](https://github.com/codenameone/CodenameOne/commits/master).

### Onwards to 6.0 – Chat

We took a lot of time for 5.0 and I’d like to take a similar duration for 6.0. I think this made 5.0 a better release.

We will have a whatsapp clone tutorial in the [Codename One Academy](https://codenameone.teachable.com/). Hence the moniker of the next release.

Check out our [survey results](/blog/survey-results-2018/) to see the future apps we’ll release into the academy. Even if you never plan to signup to the academy this should be interesting as it gives you a good notion of what can be built with Codename One.

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


### **davidwaf** — September 19, 2018 at 9:10 pm ([permalink](/blog/codename-one-5-0-social-live/#comment-23928))

> Awesome work!! ..as always. Push notifications: there was a mention they will be eventually available even for basic accounts?
>



### **Shai Almog** — September 20, 2018 at 6:50 am ([permalink](/blog/codename-one-5-0-social-live/#comment-24029))

> Shai Almog says:
>
> Thanks!
>
> Our general thought is to enable a small amount monthly push messages for free/basic users (e.g. 100) so they can test push functionality when building the app. The idea is to upgrade to pro only if they find push useful.
>
> Another idea is to increase the quota based on referrals so even the free tier would be able to get enough push messages for a small app.
>



### **Lukman Javalove Idealist Jaji** — September 20, 2018 at 1:04 pm ([permalink](/blog/codename-one-5-0-social-live/#comment-22646))

> Lukman Javalove Idealist Jaji says:
>
> I was gon ask the same question… Is there a timeline as to when this 100 units will be available?
>



### **Shai Almog** — September 25, 2018 at 8:20 am ([permalink](/blog/codename-one-5-0-social-live/#comment-24013))

> Shai Almog says:
>
> No. This depends on two things:  
> – Is it worth our while  
> – Do we have the resources to do the required work (which is extensive)
>
> Currently both are no. So far we don’t see any noticeable impact from the referral program. So investing more time in this probably won’t drive traction. We have a lot on our plates for the end of the year and Q1 2019 so I don’t see this happening soon.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
