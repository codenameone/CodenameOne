---
title: 64 bit & OSS VM
slug: 64-bit-oss-vm
url: /blog/64-bit-oss-vm/
original_url: https://www.codenameone.com/blog/64-bit-oss-vm.html
aliases:
- /blog/64-bit-oss-vm.html
date: '2014-10-19'
author: Shai Almog
---

![Header Image](/blog/64-bit-oss-vm/64-bit-oss-vm-1.png)

  
  
  
  
![64bit](/blog/64-bit-oss-vm/64-bit-oss-vm-1.png)  
  
  
  

  
Apple Just announced they will mandate 64 bit for all new submissions starting February 2015, luckily we are very prepared for that and we already support 64 bit builds in our new VM. 

This is a huge triumph for the architecture of our new VM which is very resilient to changes Apple might make in the future thanks to its pure C architecture. Our old XMLVM based approach is limited in that regard since it relies on the Boehm garbage collector which in turn relies on low level assembler and memory layouts heavily. Our new VM is both faster and more portable; now its also open source!  
  
In fact, the only portion that posed a slight challenge was porting the native libzbar which we use for QR/barcode reading. Everything related to the VM itself ported easily and works with the new high resolution devices as well as arm64.

If you want to try your app with the new VM just submit a build with the build argument ios.newVM=true. We are now actively seeking bug reports, crashes and compilation errors with the new VM in order to be able to transition it into the default VM by January. If you get an error please reproduce it as a standalone project we can compile and attach it to an issue in the issue tracker describing the problem.

We just committed the current beta of the new VM into SVN, its still not production grade but its now open source under the GPL + Class Path Exception. You can see the source under the VM directory of our SVN tree where you should see two projects. One is the bytecode translator and the other is the Java API implementation.

Basically the architecture is very simple, it statically parses the bytecode using ASM and generates C source/header files that pretty much represent the bytecode as it is in Java. It has a rudimentary optimizer but nothing too shabby. 

The main focus in the future would be removing some of the stack operations in the bytecode by possibly converting the bytecode on the fly to something that would be more efficient on the device. We will probably stay with the stack based approach rather than go completely register based like DEX has for two major reasons:  
  
1\. Similarity to the source bytecode which makes it relatively simple to locate the relevant section.  
  
2\. The stack based approach is very powerful when combined with our GC so we will need it for the GC to work correctly.

On a slightly unrelated note, IAP version 3.0 is now up on the build servers. We made a few corrections to the post from last week containing a discussion of the changes needed so if you use it please  
[  
check out the revised post  
](http://www.codenameone.com/blog/migrating-to-androids-in-app-purchase-30)  
.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — November 10, 2014 at 8:13 am ([permalink](/blog/64-bit-oss-vm/#comment-21907))

> Anonymous says:
>
> I got an error when I tried to build with the new vm
>



### **Anonymous** — November 26, 2014 at 8:14 am ([permalink](/blog/64-bit-oss-vm/#comment-22175))

> Anonymous says:
>
> Can you post on the forum with more details e.g. build error log?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
