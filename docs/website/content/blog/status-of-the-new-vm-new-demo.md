---
title: Status Of The New VM & New Demo
slug: status-of-the-new-vm-new-demo
url: /blog/status-of-the-new-vm-new-demo/
original_url: https://www.codenameone.com/blog/status-of-the-new-vm-new-demo.html
aliases:
- /blog/status-of-the-new-vm-new-demo.html
date: '2015-02-08'
author: Shai Almog
---

![Header Image](/blog/status-of-the-new-vm-new-demo/status-of-the-new-vm-new-demo-1.png)

  
  
  
  
![Picture](/blog/status-of-the-new-vm-new-demo/status-of-the-new-vm-new-demo-1.png)  
  
  
  

  
  
  
  
  
  
  
  
Now that we are officially in the 64bit era of the iOS app store and our new VM is the default target it has gotten much stabler and helped us track issues that have been a part of Codename One since its inception. As part of improving the VM and fixing bugs we made a lot of changes to the VM including some core conceptual changes. 

One of the core changes we made is removing the  
[  
reference counting  
](http://www.codenameone.com/blog/beating-the-arc)  
. In our original architecture we created a reference counting/GC hybrid which seemed like a good idea in theory. In practice reference counters are surprisingly slow (despite their inaccurate reputation) this is mainly due to multi-threaded access which makes the reference counting code problematic. We tried many approaches but eventually after many experiments we just removed the reference counter completely and saw a big speed boost. Since our GC is concurrent and never stops the world (it can stop the allocating thread if its out of RAM) you shouldn’t see any stalls assuming your thread yields properly.  
  
Removing the reference counting logic reduced code size by 20% which helps speed up compilation and the overall app performance so this is a pretty significant leap. Doing that helped us leapfrog past the performance of the previous VM in most (although not all) benchmarks. With the new VM compilation time is often under 2 minutes as opposed to 5 minute compilation time on the old VM, this alone justifies the effort in migration.

We also came across several “gotchas” in the new VM that you should be aware of:  

  1. Default encoding – the old VM defaulted to US-ASCII as its encoding where the new VM defaults to UTF-8. UTF-8 is slightly slower so you might want to explicitly use US-ASCII.  

  2. Exception performance – Exceptions in the new VM are really slow by comparison. This is mostly because we create the stack trace data before throwing which is somewhat inefficient. This shouldn’t be a big deal as long as you don’t use code that relies on exceptions for better performance e.g. looping an array until array out of bounds exception is thrown. This would be slower than just using a standard if.  

  3. Thread overhead – The new VM take up a bit more RAM per object but this is very noticeable in threads. Every thread can take half a megabyte in RAM to get started! This is pretty expensive but there is a reason. The new VM allocates stack objects for all the data within the thread stack frame. Furthermore, every object allocated within a thread is stored by the thread and only placed into the global pool when the thread and GC thread sync. This allows allocation code to be lock free and very fast as a result! 

On a completely different note we now have a new Demo in SVN titled Property Cross that demonstrates property search in the UK. It allows you to search a UK database for houses for sale and then scroll an infinite list of properties. Mark your favorites and get additional details on them. We’ll try to post a detailed tutorial on it in the coming weeks.  
  
  
  
  

  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 10, 2015 at 7:44 am ([permalink](/blog/status-of-the-new-vm-new-demo/#comment-21609))

> Anonymous says:
>
> I wish you can also improve the Windows port so that it can catchup to the Android and iOS.
>



### **Anonymous** — February 10, 2015 at 1:18 pm ([permalink](/blog/status-of-the-new-vm-new-demo/#comment-22158))

> Anonymous says:
>
> That request isn’t echoed by our enterprise customers. Due to the major effort required in rewriting that port we will only undertake it if we have several enterprise seats demanding it.
>



### **Anonymous** — February 10, 2015 at 8:09 pm ([permalink](/blog/status-of-the-new-vm-new-demo/#comment-24195))

> Anonymous says:
>
> Interesting demo – some nice techniques to be aware of.
>



### **Anonymous** — February 11, 2015 at 7:48 am ([permalink](/blog/status-of-the-new-vm-new-demo/#comment-22167))

> Anonymous says:
>
> Pity. So even if I advise my Co + manager to subscribe as an enterprise client it wouldn’t be enough as you need several…what a shame. Thanks for the feedback though.
>



### **Anonymous** — February 11, 2015 at 4:26 pm ([permalink](/blog/status-of-the-new-vm-new-demo/#comment-22118))

> Anonymous says:
>
> You can be one company that has several enterprise developer seats but yes we do see the expense. 
>
> To do Windows Phone right we need to start over and that’s up to 12 man months of work so picking that up without concrete “real” paying customer demand that would eventually recoup the cost doesn’t make much sense. We already did 3 separate efforts on the port only to be thwarted by MS’s changing strategies and limited market penetration. Its hard to convince ourselves to go at it again when the platform is so uncertain. 
>
> There are also constant rumors about APK support in Windows Phone which will really make this redundant.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
