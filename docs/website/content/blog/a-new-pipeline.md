---
title: A New Pipeline
slug: a-new-pipeline
url: /blog/a-new-pipeline/
original_url: https://www.codenameone.com/blog/a-new-pipeline.html
aliases:
- /blog/a-new-pipeline.html
date: '2014-01-26'
author: Shai Almog
---

![Header Image](/blog/a-new-pipeline/a-new-pipeline-1.png)

  
  
  
  
![Picture](/blog/a-new-pipeline/a-new-pipeline-1.png)  
  
  
  

One of our enterprise developers started complaining about the performance of our Android port, which forced us to take a closer look at our rendering pipeline on Android. It seems that Google’s hardware acceleration broke pretty much all the best practices of the Android 2.x era and what we had wasn’t taking full advantage of “project butter” the codename for Google’s new Android rendering layer.  
  
  
  
  
  
  
This took a lot of effort to adapt and the effort is still ongoing but we wrote a brand new rendering layer for Android, at the moment you would need to switch it on explicitly and it will only work for Android 3.x or newer. When we will feel that its stable we will flip the default switch and make this the default for all future Android builds.  
  
  
  
  
  
  
If you want to play with it just use the build argument android.asyncPaint=true  
  
  
  
  
  
  
In this mode all paint graphics operations are added to a “task” pipeline and rendered asynchronously which allows better utilization of the device GPU for some use cases. Because of that this mode returns false from Display.areMutableImagesFast() this allows us to optimize rendering to avoid double buffering paradigms where possible (e.g. within the MapComponent).  
  
  
  
  
  
  
One of the nice unintended side effects of this rendering mode is that color reproduction on the device is more accurate in subtle ways. The rasterization process on Android devices is slightly different especially on  
[  
PenTile  
](http://en.wikipedia.org/wiki/PenTile_matrix_family)  
devices (very common on Android) and when we use this approach such devices produce a more accurate result.  
  
  
  
  
  
  
Another unexpected bonus we got as a result of this change was that Android related GPU debugging tools started working for Codename One applications. Unfortunately, they indicated extreme overdraw issues for some cases. Overdraw indicates that we paint the same pixel multiple times to draw a single form which means the UI will effectively be slower. Androids tools are really good at pointing out a problem but they are awful at pointing us to the location of the problem. So we had to extend our performance monitor tool and add a special mode to it that shows the exact graphics operations performed by every component painting itself. This can be illuminating when trying to see why a specific UI is so slow, it also provides stack traces for every graphics operation.  
  
  
  
  
  
  
To try it just open the performance monitor from the simulator and select the second tab then refresh within the form you want to inspect. You can then traverse the tree of components and see the exact rendering calls that were used to draw every single component within the hierarchy.  
  
  
  
  
  
  
Using this tool we noticed that the parent form drew itself twice for every rendering cycle, it turns out that a special case needed only for transitions was active with every repaint. This was easy to fix in the core and should provide a small performance boost to Codename One applications on all platforms. 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 28, 2014 at 5:22 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline.html#comment-21912))

> Anonymous says:
>
> Yes! Great Job! 
>
> This is something I’ve really been waiting for.
>



### **Anonymous** — February 5, 2014 at 5:46 am ([permalink](https://www.codenameone.com/blog/a-new-pipeline.html#comment-24236))

> Anonymous says:
>
> It’s funny because just yesterday I thought I noticed my app was running smoother, sometimes its easy to forget you eager beavers are improving our lives secretly and remotely, I appreciate this so much. As for ever draw, Im assuming you can detect if something wont be scene and not render it, a little like a 2d game tile engine. I THINK though that this new build hint is making the native browsers I have embedded in my app flash.
>



### **Anonymous** — March 12, 2014 at 1:13 pm ([permalink](https://www.codenameone.com/blog/a-new-pipeline.html#comment-21796))

> Anonymous says:
>
> Excellent! I think the android port has been in need of a serious performance review for quite some time now. Android being the mobile platform with the most market share, it would be quite expedient if performance on this platform is focused on more intently. Looking forward to hearing more about this. Thanks!
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
