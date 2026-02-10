---
title: Xcode 11 is now the Default
slug: xcode-11-migration-now-default
url: /blog/xcode-11-migration-now-default/
original_url: https://www.codenameone.com/blog/xcode-11-migration-now-default.html
aliases:
- /blog/xcode-11-migration-now-default.html
date: '2020-03-19'
author: Shai Almog
---

![Header Image](/blog/xcode-11-migration-now-default/xcode-migration.jpg)

We hope you’re all keeping safe!  
We announced a couple of weeks ago that we’re moving our build servers to use xcode 11.3 by default. As a recap, Apple requires a new version of xcode/iOS SDK for apps submitted to the appstore. As a result we had to update the version of xcode on our build servers.

This has been in the cloud servers for a while and is now the default when sending new builds. For most of you this should be seamless…​

Everything should “just work”. But for some edge cases things might fail or behave differently. This is especially true if you rely on native libraries but also if you rely on some functionality that we missed in our testing.

If you run into such a problem first verify it by testing against xcode 10.1 using the build hint: `ios.xcode_version=10.1`.

__ |  We suggest removing this build hint once your done so you can use the default target recommended by us   
---|---

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
