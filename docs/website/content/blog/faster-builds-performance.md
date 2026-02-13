---
title: Faster Builds & Performance
slug: faster-builds-performance
url: /blog/faster-builds-performance/
original_url: https://www.codenameone.com/blog/faster-builds-performance.html
aliases:
- /blog/faster-builds-performance.html
date: '2014-08-05'
author: Shai Almog
---

![Header Image](/blog/faster-builds-performance/faster-builds-performance-1.png)

  
  
  
  
![Picture](/blog/faster-builds-performance/faster-builds-performance-1.png)  
  
  
  

Despite quite a few regressions with the new VM we were finally able to make some major improvements to its performance and bring it on-par with native. Its still not as fast as it could be but coupled with the far improved GC and far superior synchronization its probably a better choice in terms of performance than the old VM. We are still heavily improving it to make it even better.  
  
  
On that note, one of the major goals of this VM was in reducing build times. This took a turn to the worse this week with builds in excess of 16 minutes which effectively ground our servers to a halt. Turns out that the LLVM compiler used by Apple is pretty fast for most state of the art C/Objective-C but is downright awful for a simple #define statement. We used a lot of macros to simplify some of the generated code, but apparently the compiler chocked on them so badly it just took up all the CPU.  
  
  
  
  
This was remarkably hard to track down since we have hundreds of thousands of lines of generated code in a typical application and benchmarking the compiler is pretty much a process of trial and error.  
  
  
  
  
The servers are now up to date with a faster VM and we are also bringing in additional servers to handle the excess load and reduce the time spent in queued mode during build. This should make build times noticeably shorter regardless of whether you use the new VM or not.  
  
  
  
  
  
  
Notice that we are now rolling out our first Mac servers based on Mavericks (new Mac OS version) which broke some things in our build scripts. We made some fixes but this might trigger some regressions in the next few days as we adjust our scripts.  
  
  
  
  
On the code side of things Chen made some improvements to the image viewer and Tabs component which include improved animations and additional image sizing options.  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
