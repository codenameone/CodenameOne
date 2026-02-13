---
title: No More iOS Screenshots
slug: no-more-ios-screenshots
url: /blog/no-more-ios-screenshots/
original_url: https://www.codenameone.com/blog/no-more-ios-screenshots.html
aliases:
- /blog/no-more-ios-screenshots.html
date: '2018-08-28'
author: Shai Almog
---

![Header Image](/blog/no-more-ios-screenshots/xcode-migration.jpg)

In February I wrote about a new/improved way to [build for iOS without the screenshot process](/blog/xcode-9-on-by-default.html). That was a bit ahead of its time as the xib build didn’t disable the screenshot process yet. This is now fixed and it’s turned on by default now. That means that if you send an iOS build it won’t go through the screenshot generation process.

This means that apps with native peers in the first screen such as maps, browser etc. will start working with this coming update. It also means the splash screen of the application on iOS will be relatively simple by default. You can customize it using xcode to make it more appealing. The upside is things such as multi-tasking etc. will work correctly.

A side benefit of this is a slightly faster build. It would also make the resulting binaries even smaller than they already are. In some cases the screenshots increased the size of the binaries significantly.

If you want to revert to the old behavior you can do so by setting the build hint `ios.generateSplashScreens=true`. It now defaults to false.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 29, 2018 at 11:23 am ([permalink](https://www.codenameone.com/blog/no-more-ios-screenshots.html#comment-21579))

> Francesco Galgani says:
>
> Is the multitasking enabled by default? Is the generated splash screen equals to the icon.png of the app?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fno-more-ios-screenshots.html)


### **Francesco Galgani** — August 29, 2018 at 12:04 pm ([permalink](https://www.codenameone.com/blog/no-more-ios-screenshots.html#comment-23733))

> Francesco Galgani says:
>
> What about the compatibility of the new builds with the various versions of iPhone?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fno-more-ios-screenshots.html)


### **Shai Almog** — August 30, 2018 at 5:42 am ([permalink](https://www.codenameone.com/blog/no-more-ios-screenshots.html#comment-23892))

> Shai Almog says:
>
> It isn’t but I think it should, let me check.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fno-more-ios-screenshots.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
