---
title: Improving The iOS Port – Moving To Full Java 5 Support
slug: improving-the-ios-port-moving-to-full-java-5-support
url: /blog/improving-the-ios-port-moving-to-full-java-5-support/
original_url: https://www.codenameone.com/blog/improving-the-ios-port-moving-to-full-java-5-support.html
aliases:
- /blog/improving-the-ios-port-moving-to-full-java-5-support.html
date: '2013-06-19'
author: Shai Almog
---

![Header Image](/blog/improving-the-ios-port-moving-to-full-java-5-support/improving-the-ios-port-moving-to-full-java-5-support-1.png)

  
  
  
  
![Duke](/blog/improving-the-ios-port-moving-to-full-java-5-support/improving-the-ios-port-moving-to-full-java-5-support-1.png)  
  
  
  

We constantly try to improve the performance, speed and build speed of the various ports most importantly the iOS port. 

  
  
  
[  
Steve  
](http://sjhannah.com/blog/?p=229)  
found out a while back during his investigations that the synchronized keyword is especially slow in our iOS port, this is something that isn’t trivial to fix in the port itself. I made some work the past couple of days of mitigating the problem by adding usages of a StringBuilder like class and removing synchronization (which never worked anyway) from TextArea. This provided very dramatic and noticeable performance improvements on iOS.  

  
The next thing to do would be to remove the usage of Vector/Hashtable for MUCH faster performance by moving to ArrayList/HashMap both of which are unsynchronized and should boost performance on all devices.  

  
Why didn’t we just “do it”?  

  
The main issue is compilability,  
  
our open source code is compileable on all the platforms we support. But for J2ME/Blackberry this is a bit difficult since our server does quite a bit of heavy lifting. So our goal is to try and place the code that allows translating J2ME/Blackberry code to Java5 into the open source and then start moving forward with proper Java 5 support in the core project.  
  
  
I hope we can start doing this soon.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
