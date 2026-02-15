---
title: Java 8 & API 23 defaults
slug: java-8-api-23-defaults
url: /blog/java-8-api-23-defaults/
original_url: https://www.codenameone.com/blog/java-8-api-23-defaults.html
aliases:
- /blog/java-8-api-23-defaults.html
date: '2016-08-29'
author: Shai Almog
---

![Header Image](/blog/java-8-api-23-defaults/marshmallow.png)

One of the biggest changes we made in the past couple of years was the introduction of Java 8 language support features and making it the default target. We are now ready for the next step: removing compatibility for Java 5 targeted builds.

Notice that this won’t break existing projects…​ They will compile with the retrolambda pipeline even if you set the build hint `java.version=5`.

We are doing this so we can integrate Java 8 features into the core of the Codename One itself and make the implementation more efficient/easy.

This change will go in this Friday, if you have old projects that you haven’t compiled in a while we would suggest testing them.

### Android API 23 Default

We mentioned [Android permissions](/blog/switching-on-android-marshmallow-permission-prompts.html) a while back. We are now switching to API level 23 by default as this makes more sense moving forward.

Unlike the language change you could still revert this change manually by defining `android.targetSDKVersion=21`. Notice that this is a short term solution as our goal is to keep up with the current Android release cycle.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Simone Peirani** — September 7, 2016 at 7:16 am ([permalink](/blog/java-8-api-23-defaults/#comment-23032))

> Simone Peirani says:
>
> Hi,  
> I noticed an issue with BTDemo on Android 6.0.1 device (with API 23 in build hints). To get works the App the user should go under settings –> App – – > BTDemo –> Authorizations, and manually enable the position authorization.  
> By setting API 21 under build hints, BTDemo perfectly works instead.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
