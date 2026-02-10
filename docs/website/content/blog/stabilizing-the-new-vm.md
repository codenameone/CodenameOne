---
title: Stabilizing The New VM
slug: stabilizing-the-new-vm
url: /blog/stabilizing-the-new-vm/
original_url: https://www.codenameone.com/blog/stabilizing-the-new-vm.html
aliases:
- /blog/stabilizing-the-new-vm.html
date: '2014-12-28'
author: Shai Almog
---

![Header Image](/blog/stabilizing-the-new-vm/stabilizing-the-new-vm-1.png)

  
  
  
  
![Picture](/blog/stabilizing-the-new-vm/stabilizing-the-new-vm-1.png)  
  
  
  

  
  
  
  
  
Its always a challenge to migrate to a new implementation and the new VM is no exception, we ran into several issues and are already hard at work fixing them.  
  
Currently resolved are:  
  
[  
Issue 1149  
](https://code.google.com/p/codenameone/issues/detail?id=1149)  
: ios.newVM causes exception on Calender.getTime()  
  
[  
Issue 1151  
](https://code.google.com/p/codenameone/issues/detail?id=1151)  
: array index out of bounds exception with ios.newVM=true 

We also fixed several other issues causing the build to be slower/larger and potentially less secure. The two fixes above are already in the build servers and the additional fixes will land tomorrow.

Still open is:  
  
[  
Issue 1245  
](https://code.google.com/p/codenameone/issues/detail?id=1245)  
: Strange System.currentTimeMillis() behavior with iOS new VM

We hope to resolve that soon as well. All VM related issues are getting the most attention possible from us and we currently seem to be on track to a very stable release.  
  

  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
