---
title: New Codename One iOS VM Is Now The Default
slug: new-codename-one-ios-vm-is-now-the-default
url: /blog/new-codename-one-ios-vm-is-now-the-default/
original_url: https://www.codenameone.com/blog/new-codename-one-ios-vm-is-now-the-default.html
aliases:
- /blog/new-codename-one-ios-vm-is-now-the-default.html
date: '2014-12-24'
author: Shai Almog
---

![Header Image](/blog/new-codename-one-ios-vm-is-now-the-default/new-codename-one-ios-vm-is-now-the-default-1.png)

  
  
  
  
![Picture](/blog/new-codename-one-ios-vm-is-now-the-default/new-codename-one-ios-vm-is-now-the-default-1.png)  
  
  
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
New builds sent for iOS will now use the new iOS VM by default, this will deliver a lot of new features: stack traces, 64 bit, xcode 6+ support, iPhone 6/6+ native resolution, no stall GC etc.  
  
We spent a great deal of time stabalizing the new VM but obviously it can’t be as mature as our existing XMLVM backend and so it is quite possible that you would run into issues that occur purely in the new VM. You can test that this is a new VM issue by building against the old VM using the build argument ios.newVM=false, assuming this is indeed a new VM issue please  
[  
file a bug  
](http://code.google.com/p/codenameone/issues/)  
with a test case immediately. 

There is currently one known issue with the new VM  
[  
Issue  
](http://code.google.com/p/codenameone/issues/detail?id=1151)  

  
  
  
  
  
  
  
  
  
  
[  
1151  
](http://code.google.com/p/codenameone/issues/detail?id=1151)  
. 

Here is a mini FAQ on how this might affect your app:

**  
Q: What does the new VM include? What might be affected by the migration?  
**  
  
**  
A:  
**  
The VM includes the portion that translates bytecode to C instructions, the garbage collector and the java.* packages of the virtual machine. All of those were rewritten from scratch to create the new VM. The old VM had quite a few bugs and misconceptions related to the threading model so threads would behave differently and more in line with the Java specification.  
  
The build server, native API’s etc. weren’t significantly modified for the new VM so for most use cases the transition to the new VM should be seamless.  
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
**  
Q: What are the technical differences between the old and new VM?  
**  
  
**  
A:  
**  
The old VM was based on XMLVM which is a very generic tool that can translate any language/VM to any other language/VM. While this is pretty powerful it had quite a few drawbacks.  
  
When Apple migrated to xcode 5.1 in preparation for 64 bit support they deprecated some assembly relied on by the XMLVM garbage collector. We also had several inherent bugs that were very hard to fix in XMLVM due to its generic architecture and we decided to make a clean break.  
  
The XMLVM iOS implementation translates Java bytecode to dex and then translates that into C code, the new VM goes directly to C from bytecode. This has an advantage for the  
[  
GC implementation  
](https://www.voxxed.com/blog/2014/10/beating-the-arc/)  
, but since the ARM CPU is register based performance tuning is a bit trickier with the new VM.  
  
The old VM supported a very large block of the Java API but did so without proper testing resulting in many pieces that just didn’t work. The new VM uses a very lean implementation of the subset supported by Codename One alone and should be far more efficient for that reason.  
  
In most things the performance of the new VM should be superior, however due to the advantages of DEX over the bytecode approach in some cases it might. 

  
  
  
  
  
  
  
**  
Q: Will native code/libraries be affected?  
**  
  
**  
A:  
**  
Not by default. Native interfaces will work exactly like they always worked and you should be able to use existing code without changes.  
  
However, if you relied on existing behaviors of XMLVM such as by calling back from native code into the Java code you should read  
[  
this guide  
](http://www.codenameone.com/blog/native-ios-code-callbacks)  
. 

**  
Q: Why was the new VM developed? Why not maintain the old XMLVM port? Why not use another 3rd party VM?  
  
A:  
**  
We studied all the alternatives and came to the conclusion that any one of them would be the equivalent of moving back rather than forward.  
  
They suffer from the problem of translating directly to machine code which is problematic with the frequent changes made by Apple. They also try to target the full Java language specification which is just too big to maintain/test in a reliable way across platforms.  
  

  
  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
