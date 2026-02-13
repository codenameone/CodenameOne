---
title: New Peers on by Default on Android
slug: new-peers-on-by-default-on-android
url: /blog/new-peers-on-by-default-on-android/
original_url: https://www.codenameone.com/blog/new-peers-on-by-default-on-android.html
aliases:
- /blog/new-peers-on-by-default-on-android.html
date: '2016-08-14'
author: Shai Almog
---

![Header Image](/blog/new-peers-on-by-default-on-android/native-peer-revisited.png)

Starting with the next Friday release we will migrate to the [new peer support](/blog/new-android-peer-mode.html).  
This migration will allow us to focus on a single code base and remove the branch where we are maintaining the old peer support.

If you run into issues you can use the `android.newPeer=false` build hint to see if the issue is due to the new peers.

__ |  This is an **undocumented** temporary flag! We will remove this flag soon!   
---|---  
  
Because of that you **MUST** report regressions you encounter and **MUST NOT** set the flag without further action…​

Once this is stabler on Android and things cool down with the xcode migration, UWP port etc. we will hopefully implement this on all the other platforms as well bringing our native integration into a new age.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
