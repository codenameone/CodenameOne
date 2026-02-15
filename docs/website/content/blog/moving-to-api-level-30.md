---
title: Moving to API Level 30
slug: moving-to-api-level-30
url: /blog/moving-to-api-level-30/
original_url: https://www.codenameone.com/blog/moving-to-api-level-30.html
aliases:
- /blog/moving-to-api-level-30.html
date: '2021-07-06'
author: Shai Almog
description: We will update the build servers to target Android API level 30 this
  Friday.
---

We will update the build servers to target Android API level 30 this Friday.

The API level change should be seamless to most of you but might impact some edge case functionality and cause some native/cn1libs to fail in odd ways.

So be sure to do extra checks on Android when building a new release.

This change is mandated by Google who require that you target the latest Android version by August of this year.

You can set the build hint android.buildToolsVersion=29 as a temporary workaround if things break for your app. Please let us know in that case so we can help resolve those issues.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **plumberg** — July 13, 2021 at 8:18 pm ([permalink](/blog/moving-to-api-level-30/#comment-24468))

> plumberg says:
>
> The geofence is not firing (or firing sometimes, in unpredictable way) unless I add `android.buildToolsVersion=29` build hint. (Tested on Android 10 API 29 and Android 8.1 API 27).
>



### **Shai Almog** — July 14, 2021 at 1:15 am ([permalink](/blog/moving-to-api-level-30/#comment-24469))

> Shai Almog says:
>
> Thanks for the headsup. We’ll look into it
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
