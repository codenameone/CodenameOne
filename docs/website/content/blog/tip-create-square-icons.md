---
title: 'TIP: Create Square Icons'
slug: tip-create-square-icons
url: /blog/tip-create-square-icons/
original_url: https://www.codenameone.com/blog/tip-create-square-icons.html
aliases:
- /blog/tip-create-square-icons.html
date: '2016-11-06'
author: Shai Almog
---

![Header Image](/blog/tip-create-square-icons/just-the-tip.jpg)

This is inspired by [this post](/blog/different-icons.html) that covers the process of creating different icons for different  
OS builds. But here I’d like to discuss the lowest common denominator and how we got it wrong!

On Android an icon can have any shape and often does, on iOS we usually expect a rounded corner look and on  
Windows we expect square icons. The thing is that iOS automatically crops the icon with round corners, so if you  
provide a completely square icon it should work just fine.

As part of that we are tuning slightly the default icon that ships with Codename One to look like this:

![Square default icon](/blog/tip-create-square-icons/icon-square.png)

Figure 1. Square default icon

The main value is that this looks good on Windows, iOS and reasonable on Android. The alternative of Android  
style icons looks horrible on iOS and Windows.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
