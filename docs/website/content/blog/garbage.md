---
title: Garbage Collection
slug: garbage
url: /blog/garbage/
original_url: https://www.codenameone.com/blog/garbage.html
aliases:
- /blog/garbage.html
date: '2014-06-25'
author: Shai Almog
---

![Header Image](/blog/garbage/garbage-1.png)

  
  
  
  
![Picture](/blog/garbage/garbage-1.png)  
  
  
  

We’ve been spending an excessive amount of time tuning our garbage collection and memory management of the new iOS VM based on profiler output. This is one of the cool features in the approach we took (translating Java to C rather than going directly to machine code), we can use Apple’s nifty profiling tools to see where the CPU/GPU spend their time. 

After quite a few profiling sessions it became apparent that the GC is the main reason for the performance overhead. Further digging showed that mutexes were the real issue here.  
  
Our GC is non-blocking and concurrent which means it runs in a separate thread and never stops the world during collection. However, in order to track all the memory we use a wrapper around malloc which has to be threadsafe and stores all the objects allocated. This allows our memory sweep algorithm to remove all objects that are no longer used, the problem is that access to this global pool must be synchronized…

To solve this problem we rethought the basic concept behind this, now the pool is managed by a single thread that stores/removes elements. Every thread has a set of objects that it allocated since the last GC and every time a GC runs it copies the newly created objects to the global sweep pool and wipes the slate. This effectively removes all synchronization from the very expensive allocation process which is pretty cool.

On a separate subject our Codename One workshop for  
[  
JavaZone  
](http://2014.javazone.no/)  
just got accepted, that is pretty cool!  
  
I really  
[  
enjoyed JavaZone last year  
](http://www.codenameone.com/3/post/2013/09/javazone-trip-report.html)  
and felt it exceeded JavaOne in many aspects. This would actually be a workshop session where you can learn Codename One programming from me in person, it would be great seeing Codename One developers there.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
