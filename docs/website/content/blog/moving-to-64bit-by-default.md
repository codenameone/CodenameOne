---
title: Moving to 64bit by Default
slug: moving-to-64bit-by-default
url: /blog/moving-to-64bit-by-default/
original_url: https://www.codenameone.com/blog/moving-to-64bit-by-default.html
aliases:
- /blog/moving-to-64bit-by-default.html
date: '2017-02-08'
author: Shai Almog
---

![Header Image](/blog/moving-to-64bit-by-default/generic-java-2.jpg)

When building an iOS debug version of the app we only build a 32bit version to save build time. Otherwise the build will take almost twice as long as every file would be compiled twice. This worked well and was also fast enough. However, Apple started sending warnings to old 32bit apps and mistakes our apps as such.

The crux of the issue is [Apple forcing developers to support 64 bit](https://arstechnica.com/apple/2017/01/future-ios-release-will-soon-end-support-for-unmaintained-32-bit-apps/), we already support it and have for years. But now Apple is showing a warning on apps built without 64 bit support even if they are only meant for debug.

Unfortunately, even if this remains only as a warning this doesn’t look good for to developers trying Codename One for the first time and seeing a message that the app might be “slow”. So from now on we will build 64 bit versions of the apps by default and if you have an old 32 bit device you would need to explicitly disable that using the build hint:
    
    
    ios.debug.archs=armv7

This will produce a 32bit build like before.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **James van Kessel** — February 13, 2017 at 11:48 am ([permalink](https://www.codenameone.com/blog/moving-to-64bit-by-default.html#comment-23336))

> James van Kessel says:
>
> Hi Shai, I’m trying to track down the cause of my app development builds failing to install on a Gen5 iPod touch and a 3-year-old ipad mini with a message “could not install <app> at this time”. I suspect this issue you describe is the cause (will check this evening when I have my test devices), but I’m wondering, is this a typical symptom that other users would expect to see? are there other ways this could manifest that users may not understand from the errors apple gives?
>
> I didn’t find this answer by searching for my problem, but Lianna pointed it out on chat for me (Thank you!) suspecting it may be related. To make your helpful articles easier to find via Google, can you add more context to notes like this: for example, making a short list of the common problems users will experience due to this issue would have shortened my search by a day. I know you’re addressing a frustration point where users may think an app is running slow or seeing a warning, but in this case, I can’t install and test my app, and the apple error messages aren’t instructive to the cause. A google search of the error related to CodenameOne finds nothing but certificate issue posts, so in my opinion that will be more detrimental to users experience with CN1 than a warning about running slow which I think would be less intrusive. thanks for all that you do, and Hopefully this can make it easier to get CN1 devs going.
>



### **Shai Almog** — February 14, 2017 at 8:04 am ([permalink](https://www.codenameone.com/blog/moving-to-64bit-by-default.html#comment-23105))

> Shai Almog says:
>
> Hi,  
> thanks for the feedback. I wrote this post before we published the change and didn’t think about phrasing it like that but that is indeed a good point.
>



### **Keith Valdez Hasselstrom** — March 6, 2017 at 6:01 pm ([permalink](https://www.codenameone.com/blog/moving-to-64bit-by-default.html#comment-23419))

> Keith Valdez Hasselstrom says:
>
> I just got snagged by this. Trying to install on older IPad mini that previously worked. Maybe a little note on the dashboard would be useful. Thanks again for a great product.
>



### **Shai Almog** — March 7, 2017 at 6:33 am ([permalink](https://www.codenameone.com/blog/moving-to-64bit-by-default.html#comment-23343))

> Shai Almog says:
>
> I was going to answer that we looked into that and couldn’t find the right place in the UX and just as I was typing that I had that forehead slap of “why didn’t I think about THAT”.
>
> There is a place to add that: the page with the green “Install” button we show on the iOS device. It’s not perfect but at least it will get noticed more.
>



### **Keith Valdez Hasselstrom** — March 7, 2017 at 2:20 pm ([permalink](https://www.codenameone.com/blog/moving-to-64bit-by-default.html#comment-23240))

> Keith Valdez Hasselstrom says:
>
> Perfect !! At a minimum it would be a quick reminder. Thanks again for the level of support you personally provide; for a project this size, it’s extremely impressive !!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
