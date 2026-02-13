---
title: Code Freeze for 3.3 & Performance
slug: code-freeze-for-3-3-performance
url: /blog/code-freeze-for-3-3-performance/
original_url: https://www.codenameone.com/blog/code-freeze-for-3-3-performance.html
aliases:
- /blog/code-freeze-for-3-3-performance.html
date: '2016-01-19'
author: Shai Almog
---

![Header Image](/blog/code-freeze-for-3-3-performance/3.3-coming-soon.jpg)

We’ve been working feverishly to get Codename One 3.3 out of the door next week. Tomorrow morning we  
will finally have the codefreeze branch for 3.3 and we’ll be able to focus on getting the docs/release in order.  
The release should be on the 27th of the month and we should ideally get the plugins out of the door within the  
next couple of days. 

#### Performance Update

We’ve worked a lot on getting Android to perform nicely in the newer phones. When we launched the Android  
graphics architecture was quite different than it is today so we had to make quite a few changes over the years.  
Unfortunately, because of the ridiculously wide variety of devices something that performs well (or even functions)  
across devices is just not technically feasible on Android.  
We made a lot of improvements but they carry a heavy risk and we won’t turn them on by default on 3.3, we did  
however add an option to try them dynamically. You can just invoke the call: 
    
    
    Display.getInstance().setProperty("platformHint.legacyPaint", "false");

This will toggle on the new Android optimizations (but have no effect elsewhere), you can flip the flag back and  
forth dynamically to see the difference. Although notice that you should probably recreate the form when doing this  
as some content might end up blank as a result.   
Notice that while this improves our Android performance significantly on newish devices (ironically we perform  
well on older devices and very new devices), this isn’t the “end all” for performance tuning on Android. We  
are still working on improving performance further both on Android & iOS. This will also include revisited  
tools & guides for performance optimization/tuning of your apps.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
