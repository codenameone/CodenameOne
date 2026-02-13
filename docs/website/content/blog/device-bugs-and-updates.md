---
title: Device Bugs And Updates
slug: device-bugs-and-updates
url: /blog/device-bugs-and-updates/
original_url: https://www.codenameone.com/blog/device-bugs-and-updates.html
aliases:
- /blog/device-bugs-and-updates.html
date: '2014-02-17'
author: Shai Almog
---

![Header Image](/blog/device-bugs-and-updates/device-bugs-and-updates-1.png)

  
  
  
  
![Picture](/blog/device-bugs-and-updates/device-bugs-and-updates-1.png)  
  
  
  

We’ve been even more busy than usual with our first  
[  
corporate deployment  
](/corporate-server.html)  
kicking off, this is currently a pretty rough process that requires a lot of hands on help from us but we hope to make it less painful for our customers. Either way, this being a completely new offering with a great deal of complexity involved its an uphill effort which is part of why we are experiencing a slowdown in new features.  
  
  
  
  
Another aspect that hit us this week is the camera bug on iOS which apparently still wasn’t resolved, it got solved thanks to the combined stubbornness of Chen and some clues from Steve. We also had some rendering artifacts on Android KitKat due to regressions Android has with its TextureView, we ended up disabling the TextureView which we introduced to workaround a relatively rare Android bug (there are a lot of those around). You can still explicitly enable the TextureView approach by using the build flag android.textureView=true.  
  
  
  
  
These issues are highlighting the importance of migrating to the Android async mode, unfortunately it isn’t complete at this time and it too suffers from some significant issues.  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
