---
title: Moving to Xcode 12
slug: moving-to-xcode-12
url: /blog/moving-to-xcode-12/
original_url: https://www.codenameone.com/blog/moving-to-xcode-12.html
aliases:
- /blog/moving-to-xcode-12.html
date: '2021-04-27'
author: Shai Almog
description: Apple updated their requirements for App Store submissions so new apps
  must be built with Xcode 12. As a result we’ve updated our build servers to the
  latest Xcode 12.4 release.
---

Apple updated their requirements for App Store submissions so new apps must be built with Xcode 12. As a result we’ve updated our build servers to the latest Xcode 12.4 release.

![](https://www.codenameone.com/wp-content/uploads/2021/04/Moving-to-Xcode-12.jpg)

This isn’t on by default yet so if you need it this week you’ll need to use the build hint: ios.xcode\_version=12.4

After the weekend this will be the default so if you don’t need to submit a build to Apple this week you can just ignore this.

If this will trigger a regression in your code (possible if you have a native dependency) you can temporarily workaround it by using the build hint: ios.xcode\_version=11.3

## Note

> Older versions are no longer supported and were removed from our servers.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Julio Valeriron Ochoa** — October 7, 2021 at 1:28 pm ([permalink](/blog/moving-to-xcode-12/#comment-24499))

> Julio Valeriron Ochoa says:
>
> I thing that you must start to migrate to xcode 13 to compile app to IOS 15.<https://github.com/codenameone/CodenameOne/issues/3510>
>



### **Shai Almog** — October 7, 2021 at 1:35 pm ([permalink](/blog/moving-to-xcode-12/#comment-24500))

> Shai Almog says:
>
> The build hint in the issue resolves that problem
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
