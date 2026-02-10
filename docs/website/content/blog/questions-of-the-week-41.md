---
title: Questions of the Week 41
slug: questions-of-the-week-41
url: /blog/questions-of-the-week-41/
original_url: https://www.codenameone.com/blog/questions-of-the-week-41.html
aliases:
- /blog/questions-of-the-week-41.html
date: '2017-02-02'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-41/qanda-friday2.jpg)

We are releasing a small Eclipse update today, it’s small because it isn’t a full release that includes the latest features/libraries but rather a minor bug fix to the previous release which was missing the UWP build target. One of the biggest changes this week is the fix for the build performance issue mentioned below. It might also speed up general app performance for some use cases!

The biggest change this week is the support for peer component Z-ordering on practically all platforms, this completely changes the use cases for Codename One and we intend to take full advantage of these new capabilities moving forward.

[Klaus](http://stackoverflow.com/users/7191374/klausheywinkel) asked about that annoying [iOS 10+ warning](http://stackoverflow.com/questions/41943982/warning-when-starting-the-codenameone-app-on-ios-app-slows-down-the-device) when building 32bit apps. It seems Apple is [dropping 32bit support](https://arstechnica.com/apple/2017/01/future-ios-release-will-soon-end-support-for-unmaintained-32-bit-apps/) moving forward so we might flip the default to be 64 bit on this flag in the near future.

This [issue](http://stackoverflow.com/questions/41930957/what-could-cause-long-build-times) raised by [Stefan](http://stackoverflow.com/users/5695429/stefan-eder) is really problematic and I hope it leads us to faster build times which are crucial!   
What I really liked about this is the good and small test case he was able to come up with for such a challenging issue…​

[Dee](http://stackoverflow.com/users/6921936/dee-bo) ran into some issues with signature component and JPEG export, since the generated image is transparent this [caused some issues with the simulator codec](http://stackoverflow.com/questions/41852966/codename-one-signatuecomponent-image-upload).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
