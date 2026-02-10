---
title: VM Stats
slug: vm-stats
url: /blog/vm-stats/
original_url: https://www.codenameone.com/blog/vm-stats.html
aliases:
- /blog/vm-stats.html
date: '2014-04-29'
author: Shai Almog
---

![Header Image](/blog/vm-stats/vm-stats-1.png)

  
  
  
  
![Picture](/blog/vm-stats/vm-stats-1.png)  
  
  
  

If you haven’t yet filled out the  
[  
developer economics survey  
](http://www.vmob.me/DE3Q14CodenameOne)  
please do so now! We are still short of 30 entries in order to get better logo placement in the released survey. This is important since the people who read these surveys are of a demographic that’s much harder for us to reach normally.  
  
It would help us greatly if you would convince your friends to fill this out as well. Thanks. 

Finishing the work on the new VM is taking longer than we originally estimated (as all engineering tasks do) but its getting along well. To give you a sense of scale, converting the Kitchen Sink to C with the old VM took 1,851,249 lines of code where is with the new VM its a “svelte” 1,001,317 with the current implementation. The old XMVM implementation produced 4,220 source files to do that whereis the new implementation produces “only” 1,474 files.  
  
Ideally we’d like this to shrink significantly since one of the main motivations here is smaller size and faster builds.

However, the new VM will be naturally very verbose since bytecode by definition is more verbose than Dalvik which the existing XMLVM implementation relies on. We also embed the GC logic directly into the C code using an ARC like GC architecture (emphasis on the word “like” ARC isn’t a GC) which would also enlarge the VM a bit. 

One of the hard goals with the new VM is to be mostly source level compatible with XMLVM but as we are moving forward this is something we are starting to reconsider. We already broke one major compatibility aspect with method signatures. E.g. with XMLVM a class such as this:  
  
package com.mycompany;  
  
class MyClass {  
  
public int myMethod(int arg1, int arg2) ….  
  
}

With be translated to this in C:  
  
JAVA_INT com_mycompany_myMethod___int_int(JAVA_OBJECT thisObject, JAVA_INT arg1, JAVA_INT arg2);

We already changed this to support covariant return type in Java where the method return value must be a part of the signature, so if your return type is void this will work like XMLVM but for any other return type you will get:  
  
JAVA_INT com_mycompany_myMethod___int_int_  
**  
R_int(  
**  
JAVA_OBJECT thisObject, JAVA_INT arg1, JAVA_INT arg2);

This allows us to have two methods with different return values which the VM spec allows even though the Java language disallows it! The javac compiler makes use of that discrepancy to generate things such as covariant return type support.

We are considering a more radical change though, we would like to add to every method an additional argument:  
  
JAVA_INT com_mycompany_myMethod___int_int_R_int(  
**  
CODENAME_ONE_THREAD_STATE  
**  
JAVA_OBJECT thisObject, JAVA_INT arg1, JAVA_INT arg2);

This will allow us to define this as nothing on the current XMLVM code but when building the new VM we will be able to pass the thread context thru the method stack rather than look it up constantly which is an expensive operation. In the new VM this will translate to something like:  
  
#define CODENAME_ONE_THREAD_STATE struct ThreadData* threadStateData, 

Which will effectively compile to this:  
  
JAVA_INT com_mycompany_myMethod___int_int_R_int(  
**  
struct ThreadData* threadStateData,  
**  
JAVA_OBJECT thisObject, JAVA_INT arg1, JAVA_INT arg2);

This will allow us to avoid expensive locking and critical sections and easily keep track of the stack.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
