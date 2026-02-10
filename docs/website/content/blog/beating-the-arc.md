---
title: Beating The ARC
slug: beating-the-arc
url: /blog/beating-the-arc/
original_url: https://www.codenameone.com/blog/beating-the-arc.html
aliases:
- /blog/beating-the-arc.html
date: '2014-05-06'
author: Shai Almog
---

![Header Image](/blog/beating-the-arc/beating-the-arc-1.png)

  
  
  
  
![Picture](/blog/beating-the-arc/beating-the-arc-1.png)  
  
  
  

For the uninitiated, ARC is Apple’s term for Automatic Reference Counting. Objective-C uses a reference counting scenario to collect objects which is pretty painful to work with. Personally I preferred C/C++’s manual delete/free to the Objective-C semantics. But a couple of years ago Apple introduced ARC in which the compiler implicitly inserts the retain/release reference counting logic. 

While its a big improvement its still a reference counter with many of the implied limitations. It solves 95% of your memory handling logic leaving the hardest 5% for you to deal with manually but it does have one advantage over a GC: determinism. Since memory is deallocated immediately it provides consistent performance with no GC stalls.

We briefly covered the garbage collection approach we took with the new iOS VM, however this time we’ll go more in-depth into the implementation details. Our goal with the garbage collection was not to create the fastest GC possible but the one that stalls the least. Up to now pretty much all open source/iOS VM’s used Boehm GC which is a conservative GC designed for C, it is state of the art and pretty cool but stalls.  
  
Boehm can’t really avoid stalling since it needs to stop all executing threads so it can traverse their stacks and this takes time… 

Unlike C, we can make a lot of assumptions in a Java application thanks to the type safety and clearly defined VM. This makes the process of collecting comparatively easy and makes it possible to collect without stopping the world. We do however need that threads yield the CPU shortly otherwise the GC will be blocked, this is generally a good practice and the EDT makes sure to follow that practice however if you do something like this:

while(true) {  
  
System.out.println(‘WHeee”);  
  
}

It would block our new GC from running unless you add a Thread.yield/sleep or wait() call (besides draining the CPU/battery). This might be considered a flaw but we mitigated that to some degree by incorporating a reference counting collector as well (similar to ARC) which deals with the “low hanging garbage” thus making the actual GC process far less important so our GC sweeps don’t need to be very fast. 

But this post is titled “beating the ARC”… How can we be faster than ARC?  
  
Simple, we don’t de-allocate. All objects that our reference counter deems to be garbage are sent to the garbage heap and finalized/deleted on the GC thread (as is custom in Java) hence we get the benefit of multi-core parallel cleanup logic on top of the fast performance. 

Our GC never actually pauses the world, it uses a simple mark sweep cycle where we iterate the thread stacks and mark all objects in use, we then iterate all the objects in the world and delete the living, unmarked objects. Since deletion of GC’d and reference counted objects is always done in the GC thread this is pretty easy and thread safe on the VM part. The architecture is actually rather simple and conservative.

The benefit of the reference counting approach becomes very clear with the non-pausing GC, since the reference counting system still kicks out objects from RAM the GC serves only for the heavy lifting. So it can be executed less frequently and its OK for it to miss quite a few objects as the reference counting implementation will pick up the slack.

We are still working on getting this into users hands ideally within the next couple of weeks (albeit in alpha state) and eventually open sourcing all of that code.

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
