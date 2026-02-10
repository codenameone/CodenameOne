---
title: Corporate Guide
slug: corporate-guide
url: /blog/corporate-guide/
original_url: https://www.codenameone.com/blog/corporate-guide.html
aliases:
- /blog/corporate-guide.html
date: '2014-03-04'
author: Shai Almog
---

![Header Image](/blog/corporate-guide/corporate-guide-1.png)

  
  
  
  
![Picture](/blog/corporate-guide/corporate-guide-1.png)  
  
  
  

We recently introduced the corporate server option which is seeing initial deployments right now. As part of that we are now publishing the install guide to the general public, if you are considering the option of purchasing a corporate server but aren’t sure about the process of install you can follow the instructions  
[  
here  
](/corporate-server.html)  
.  
  
  
  
In other news you might recall in a recent blog post I mentioned  
  
[  
we added isDragRegion  
](http://www.codenameone.com/3/post/2014/02/wheel-drag.html)  
, well its now deprecated in favor of a more ambitious getDragRegionStatus which can return multiple constants to help us fine tune the drag behavior to all the various edge cases and make the UI feel more responsive.  
  
  
  
  
Up until now desktop applications in Codename One shared the same .cn1 storage as the simulator, this was a problem with multiple apps installed. Starting with current builds desktop apps should store their data under .AppMainClassName within the home directory. We also fixed the icon on the windows builds to appear correctly within the frame and the running apps menu (alt-tab etc.).  
  
  
  
  
  
  
The Google Play ads were causing some issues in Gingerbread devices where clicks would have no effect, unfortunately we aren’t clear on why exactly this happens but its related to obfuscation probably removing some compatibility code needed by Google. The only workaround we found for this to work on older devices is to disable obfuscation on Android by using the build argument: android.enableProguard=false  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
