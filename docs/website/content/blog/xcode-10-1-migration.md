---
title: Xcode 10.1 Migration
slug: xcode-10-1-migration
url: /blog/xcode-10-1-migration/
original_url: https://www.codenameone.com/blog/xcode-10-1-migration.html
aliases:
- /blog/xcode-10-1-migration.html
date: '2019-01-08'
author: Shai Almog
---

![Header Image](/blog/xcode-10-1-migration/xcode-migration.jpg)

Over the past month Apple started sending out warnings that they will no longer accept apps built with older SDK’s starting this March. To preempt that we will update our servers to use xcode 10.1 over the next couple of weeks. This change should be seamless for the most part but some odd behaviors or bugs usually rise as a result of such migrations.

To test if a sudden regression is caused by this migration you can explicitly force xcode 9.2 for compatibility by using the build hint: `ios.xcode_version=9.2`.

We also considered updating the Android target SDK from 27 to 28 but since that would be a requirement only around August it might make better sense to postpone it to the 7.0 release cycle.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — January 10, 2019 at 11:29 am ([permalink](https://www.codenameone.com/blog/xcode-10-1-migration.html#comment-23911))

> Francesco Galgani says:
>
> Will do you still use Gradle 4.6? This information can be useful when testing native interfaces.
>



### **Shai Almog** — January 10, 2019 at 11:52 am ([permalink](https://www.codenameone.com/blog/xcode-10-1-migration.html#comment-23902))

> Shai Almog says:
>
> Right now we won’t touch the Android side. When we do the migration to 28 we might need newer version of gradle though. We’ll post about it before that migration takes place as we’ll have specific version numbers and setting information.
>



### **Francesco Galgani** — January 10, 2019 at 11:53 am ([permalink](https://www.codenameone.com/blog/xcode-10-1-migration.html#comment-23973))

> Francesco Galgani says:
>
> Ok, thank you
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
