---
title: Navigate & Faster Builds
slug: navigate-faster-builds
url: /blog/navigate-faster-builds/
original_url: https://www.codenameone.com/blog/navigate-faster-builds.html
aliases:
- /blog/navigate-faster-builds.html
date: '2014-06-08'
author: Shai Almog
---

![Header Image](/blog/navigate-faster-builds/navigate-faster-builds-1.png)

  
  
  
  
![Picture](/blog/navigate-faster-builds/navigate-faster-builds-1.png)  
  
  
  

One of the things we’ve been missing is a simple “navigate” feature that allows you to launch the devices native navigation software with a fixed destination. This was relatively simple to hack together using Display.execute but that’s not the same as an official API. Chen just added two methods to Display that should really help in this process: isOpenNativeNavigationAppSupported & openNativeNavigationApp(lat, lon). This should launch the device navigation software (e.g. Google Maps) with the given destination.  
  
  
  
  
  
As you might recall building an iOS native app requires 7 screenshots, this slows the build a bit (depends on your apps functionality though), to slightly speed up your build you can use the build argument ios.fastbuild=true which will use hardcoded splash screen images (notice that this will only work for debug builds not for appstore builds). For the kitchen sink this shaves roughly 15 seconds from the startup time, but it might shave minutes off your build for a complex app.  
  
  
  
  
We have some pretty big changes that landed last week, but we’ll write about them later this week.  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 20, 2014 at 4:17 am ([permalink](/blog/navigate-faster-builds/#comment-21948))

> Anonymous says:
>
> Thanks for including this feature. Tested on Android and iOS and it worked perfectly. I will like to know if this functionality will work on windows phone by opening Here Drive or similar navigator?
>



### **Anonymous** — June 20, 2014 at 1:31 pm ([permalink](/blog/navigate-faster-builds/#comment-22004))

> Anonymous says:
>
> At the moment this isn’t supported on Windows Phone.
>



### **Anonymous** — June 21, 2014 at 8:58 am ([permalink](/blog/navigate-faster-builds/#comment-21821))

> Anonymous says:
>
> Tried it on Nokia X device running Android OS, it works perfectly by opening Nokia Here Drive. Hopefully it will work on Windows Phone. Thanks once again for this feature…I have been waiting for it
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
