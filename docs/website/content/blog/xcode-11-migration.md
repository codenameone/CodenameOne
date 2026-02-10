---
title: Xcode 11 Migration
slug: xcode-11-migration
url: /blog/xcode-11-migration/
original_url: https://www.codenameone.com/blog/xcode-11-migration.html
aliases:
- /blog/xcode-11-migration.html
date: '2020-03-05'
author: Shai Almog
---

![Header Image](/blog/xcode-11-migration/xcode-migration.jpg)

Apple keeps moving the goal posts of xcode requirements for developers. This is good as it keeps the technology fresh but it means support for older devices becomes untenable. Unfortunately there isn’t much we can do and we need to move with the times as Apple will no longer accept apps built with older versions of xcode.

The main problem with this is another pain point for iOS developers. Newer versions of code require newer versions of Mac OS. That means we need to update the version of Mac OS on all of our servers. That’s a HUGE pain not just because of the drudge of upgrading every server…​

It’s a pain because Catalina (the new Mac OS) isn’t compatible with xcode 9.2. As a result we killed support for xcode 9.2 and if you explicitly request it in your build hints your build will fail starting today. Next week we’ll upgrade the OS’s and install the new version of xcode. At that point you should be able to send a build with xcode set to 11.3 as an option. To do that you can use the build hint: `ios.xcode_version=11.3` or `ios.xcode_version=10.1`. By April we hope to make 11.3 the default value.

__ |  We suggest removing this build hint once your done so you can use the default target recommended by us   
---|---
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — March 8, 2020 at 9:03 pm ([permalink](https://www.codenameone.com/blog/xcode-11-migration.html#comment-21423))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Thanks for all your hard work guys, much appreciated
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-11-migration.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
