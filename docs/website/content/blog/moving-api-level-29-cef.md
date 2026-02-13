---
title: Moving to API Level 29 and CEF Update
slug: moving-api-level-29-cef
url: /blog/moving-api-level-29-cef/
original_url: https://www.codenameone.com/blog/moving-api-level-29-cef.html
aliases:
- /blog/moving-api-level-29-cef.html
date: '2020-07-30'
author: Shai Almog
---

![Header Image](/blog/moving-api-level-29-cef/generic-java-1.jpg)

Steve just updated our CEF support which should now work on all platforms. We updated [the original post](/blog/big-changes-jcef.html) with additional information. We’ve also updated the build servers to use API level 29 on Android.

The API level change should be seamless to most of you but might impact some edge case functionality and cause some native/cn1libs to fail in odd ways. So be sure to do extra checks on Android when building a new release.

This change is mandated by Google who require that you target the latest Android version within 6 months of its release.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
