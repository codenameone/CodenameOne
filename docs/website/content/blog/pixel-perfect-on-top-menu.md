---
title: Pixel Perfect – On Top Menu
slug: pixel-perfect-on-top-menu
url: /blog/pixel-perfect-on-top-menu/
original_url: https://www.codenameone.com/blog/pixel-perfect-on-top-menu.html
aliases:
- /blog/pixel-perfect-on-top-menu.html
date: '2017-09-12'
author: Shai Almog
---

![Header Image](/blog/pixel-perfect-on-top-menu/pixel-perfect.jpg)

I already have half a post on text components but I’ve put that on hold for now as I’ve been working on the [on-top side menu](/blog/sidemenu-on-top.html) to supersede the existing side menu bar implementation. I’ve made some fixes for it over the week, I wanted to make it the default for Codename One apps but it still isn’t “perfect”. We will make it the default within the next couple of weeks so please test it after this weeks update and let us know ASAP if you spot any issues!

After the latest round of work the side menu now resides on top of the entire form and not just over the content pane. I was able to address some issues in the form layer implementation and it should work properly now. I also fixed some pretty hairy issues with events and even a rather insane issue with `invokeAndBlock`.

To give you a sense of how this looks this is the old side menu in the kitchen sink demo:

![The old side menu in the kitchen sink](/blog/pixel-perfect-on-top-menu/old-side-menu.png)

Figure 1. The old side menu in the kitchen sink

If we add the call `Toolbar.setOnTopSideMenu(true)` to the `init(Object)` callback after this weekend update we should see this:

![The new on-top side menu in the kitchen sink](/blog/pixel-perfect-on-top-menu/on-top-side-menu.png)

Figure 2. The new on-top side menu in the kitchen sink

There is a lot of nuance involved in this new mode, e.g. the underlying form grows gradually darker as you drag on top of it etc.

### Flipping the Switch

We’ll flip this to be the default mode within a couple of weeks unless something big happens. At that point if you have dependencies on the old behavior and would like to restore that you would need to explicitly invoke `Toolbar.setOnTopSideMenu(false)` in the `init(Object)` method.

As we discussed in the past we want to avoid the “legacy pitfall” many frameworks fall into. In this pitfall frameworks try to be “overly compatible” with nuanced behaviors which means users don’t adopt the newer/better functionality that’s available to them.

With this we hope to resolve some of the open issues in the existing side menu implementation.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
