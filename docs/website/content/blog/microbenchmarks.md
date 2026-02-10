---
title: Microbenchmarks
slug: microbenchmarks
url: /blog/microbenchmarks/
original_url: https://www.codenameone.com/blog/microbenchmarks.html
aliases:
- /blog/microbenchmarks.html
date: '2014-07-28'
author: Shai Almog
---

![Header Image](/blog/microbenchmarks/microbenchmarks-1.jpg)

  
  
  
  
![Picture](/blog/microbenchmarks/microbenchmarks-1.jpg)  
  
  
  

Microbenchmarks are often derided in the Java community with some good reason, they show edge cases that either present the JIT in a bad light or show it as ridiculously (unrealistically) fast. However, with static compilers and translation tools microbenchmarks can give us insight into performance problems that normal profilers might not provide insight into.  
  
  
  
We’ve been tracking performance issues with the new iOS VM for the past couple of weeks and had a very hard time pinpointing the issues, finally thanks to microbenchmarks we see some of the performance issues and already eliminated quite a few bottlenecks in the code that bring the optimization level closer to native speed. E.g. our getter/setters are now practically free.  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
