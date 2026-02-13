---
title: Introducing Codename One 7.0 (AKA Video)
slug: introducing-codename-one-7-0-aka-video
url: /blog/introducing-codename-one-7-0-aka-video/
original_url: https://www.codenameone.com/blog/introducing-codename-one-7-0-aka-video.html
aliases:
- /blog/introducing-codename-one-7-0-aka-video.html
date: '2021-02-05'
author: Shai Almog
---

![Codename One 7.0 - Video](/blog/introducing-codename-one-7-0-aka-video/7.0-Video-1c.jpg)

We’re thrilled to announce the immediate availability of Codename One 7.0 (AKA Video). This has been our most challenging release to date. We constantly shifted the release date due to constantly shifting requirements and pivotal changes.

### Highlights of this Release 

Here are the highlights of Codename One 7.0

### WebRTC Support

This is probably one of the biggest challenges we took to date. We now have one of the best cross platform WebRTC implementations. This took a lot of work and proved to be a huge challenge in every single platform.

### Build App

With the new build app we want to change the process of building/working with Codename One. Right now it’s “just” a more unified settings/build experience but we plan to make it central to Codename One development. This will reduce the importance of the IDE plugins and of the build app.

### CEF Migration

It’s now possible to use proper web APIs and debug web based code embedded in a Codename One application. This also finally removes the problematic JavaFX dependency we had for our simulator and desktop ports.

### [CodeRad](https://github.com/shannah/CodeRAD)

[CodeRAD](https://github.com/shannah/CodeRAD) might end up being the biggest feature of this release. It’s a higher level approach for building Codename One apps that makes building elaborate apps much easier.

### New Website and SSO

This isn’t quite a Codename One feature but it’s a huge change to the way we handle builds, signups etc.

### Other Features

Those were the highlights but there were also a few important smaller features. There are too many to list but here are some that you might find interesting:

→ [Sign in with Apple Support](https://www.codenameone.com/blog/sign-in-with-apple-support.html) – this also includes some improvements to the OAuth support

→ New [KitchenSink](https://play.google.com/store/apps/details?id=com.codename1.demos.kitchen) Demo

→ [AudioRecorderComponent](https://github.com/codenameone/CodenameOne/commit/cc6ba706e1bd407de0fc9405ce7950d8c4392053)

→ [API to set video capture constraints](https://github.com/codenameone/CodenameOne/commit/4d605b9ca8f836f7d771ba4eaec9f73e152fc9d5)

→ Multiple fixes to ParparVM (used in the iOS port) mostly for Kotlin support and wider support of the Java API

→ [Dark Mode API](/blog/dark-mode.html)

→ [Sheet Component](https://www.codenameone.com/blog/sheet-positions.html)

→ [SpanMultiButton Component](https://github.com/codenameone/CodenameOne/commit/b5863f65be564756680a97b1aa90df68a9edfc10)

→ Improved [CSS Image Border](https://github.com/codenameone/CodenameOne/commit/12ac33405c0db920cf73c0c54a42cb3c6186d080) support

→ [Tooltips](https://github.com/codenameone/CodenameOne/commit/94610310187ade1bc0a2adbf0850618c15e36515) Support

→ [Safe download](https://github.com/codenameone/CodenameOne/commit/20dd950faa698bd4722f950763b5e462b5975340) even when minimized

→ Labels (and their subclasses) now [support badging](https://github.com/codenameone/CodenameOne/commit/4875e5f7b250a9015f16bec2709468bf35f2f129)

→ Better Java to JavaScript interaction with [postMessage](https://github.com/codenameone/CodenameOne/commit/64d8464109331c00908c2a18123fa21ac2598e09)

→ [Arrow dialogs](https://github.com/codenameone/CodenameOne/commit/921395f6613e7a2bb883f41d4217797f7d790fa9) now work in a cross platform way

→ [Radar Chart](https://github.com/codenameone/CodenameOne/commit/675a0ca5018705d5080f1ce393f107a4deedb305)

→ [Async Media](https://www.codenameone.com/blog/media-async-play-and-pause-support.html)

### Moving Forward

Codename One 8 will continue in some of the same directions but will also pivot strongly towards more open architectures.

We intend to build more upon the basic framework of Codename One Build. We think it’s better to use a single app to manage the build, settings and everything related to Codename One as opposed to using multiple plugins.

This will let us simplify the plugins and make them easier to update/maintain.

We also plan to migrate to Maven. This is already in progress and will materialize within the next couple of months. This will simplify some aspects of our build process and update/dependency management.

We will use CSS as the default framework effectively deprecating the designer approach. While the designer has many advantages ultimately CSS is the de-facto standard and with live CSS editing it’s a pretty compelling case.

We would also like to strengthen our desktop offering. We believe Codename One on the desktop has a unique value proposition for Java developers that no other framework offers at this time. This is especially true with our CEF support.

Finally, we intend to make it easier to build Codename One from sources and work with the open source code that’s already available.

### How Can You Help?

If you think we are doing a good job and appreciate our help, please help us by:  
  

– [Spreading the word](https://www.codenameone.com/blog/how-you-can-help-spread-codenameone.html)

– [Edit our docs](https://www.codenameone.com/blog/tip-edit-docs-fun-profit.html)

– [Edit our sources and submit bug fixes](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html)

[– Signup/Upgrade](https://www.codenameone.com/pricing.html) your account

If your company can afford it, please take the time and upgrade to enterprise, this will allow us to work on the things that are important for your company!

Thanks for reading this far and if you have any thoughts/suggestions of any kind please let us know!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — February 6, 2021 at 11:15 pm ([permalink](https://www.codenameone.com/blog/introducing-codename-one-7-0-aka-video.html#comment-24400))

> Javier Anton says:
>
> Congratulations and can’t wait to see where CN1 goes from here
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintroducing-codename-one-7-0-aka-video.html)


### **IMIENS CORP** — February 9, 2021 at 8:26 am ([permalink](https://www.codenameone.com/blog/introducing-codename-one-7-0-aka-video.html#comment-24407))

> IMIENS CORP says:
>
> Thank you so much, for adding WebRTC !
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintroducing-codename-one-7-0-aka-video.html)


### **IMIENS CORP** — February 9, 2021 at 8:33 am ([permalink](https://www.codenameone.com/blog/introducing-codename-one-7-0-aka-video.html#comment-24408))

> IMIENS CORP says:
>
> BTW, any examples, docs on WebRTC ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintroducing-codename-one-7-0-aka-video.html)


### **Shai Almog** — February 10, 2021 at 2:06 am ([permalink](https://www.codenameone.com/blog/introducing-codename-one-7-0-aka-video.html#comment-24409))

> Shai Almog says:
>
> Nothing serious yet but initial stuff is in the project page: <https://github.com/shannah/CN1WebRTC>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fintroducing-codename-one-7-0-aka-video.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
