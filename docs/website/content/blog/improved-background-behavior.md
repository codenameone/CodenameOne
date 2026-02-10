---
title: Improved Background Behavior
slug: improved-background-behavior
url: /blog/improved-background-behavior/
original_url: https://www.codenameone.com/blog/improved-background-behavior.html
aliases:
- /blog/improved-background-behavior.html
date: '2016-08-15'
author: Shai Almog
---

![Header Image](/blog/improved-background-behavior/background-fetch.jpg)

A couple of years ago at Google IO one of the prominent speakers turned to the audience and asked them: “Raise your hands if you understand the activity lifecycle”. He then proceeded to call them “liars”, claiming that after all his years at Google he still doesn’t get it properly…​

As a guy who worked on VM’s and understands some of the nuance I totally get his point. Lifecycle is hard. On Android it seems the developers took a difficult subject and made it even harder. With that in mind our implementation of background behavior on Android was lacking in some regards and Steve did a major overhaul of the implementation. Like all overhauls this could trigger some regressions so please keep your eyes open for such cases.

This change should be seamless and improve the applications robustness if you use background features.

### Material Icons Everywhere

This went in a while back, we now use material icons in the `MediaPlayer` class by default for play/pause/forward & back.

We will also default to using material icons to represent tree folders/nodes in the next update to make it better looking and consistent by default.

We hope to use material icons by default on all UI elements. Getting to the older pieces of code is sometimes challenging as we don’t always notice them. If you can think of a component that can use a material icon makeover sound off in the comments.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
