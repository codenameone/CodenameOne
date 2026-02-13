---
title: Static Garbage & Lollipop
slug: static-garbage-lollipop
url: /blog/static-garbage-lollipop/
original_url: https://www.codenameone.com/blog/static-garbage-lollipop.html
aliases:
- /blog/static-garbage-lollipop.html
date: '2014-11-23'
author: Shai Almog
---

![Header Image](/blog/static-garbage-lollipop/static-garbage-lollipop-1.jpg)

  
  
  
  
![Picture](/blog/static-garbage-lollipop/static-garbage-lollipop-1.jpg)  
  
  
  

  
  
  
  
  
  
  
As  
[  
we wrote before  
](https://www.voxxed.com/blog/2014/10/beating-the-arc/)  
our new Garbage Collector is designed for amazing speed an never locks, this worked really well for most cases but we started running into weird crashes that took us deep into the seemingly simple GC code and exposed flaw in our “no locking” approach. It seems that our assumption that we can just mark all the static objects was flawed since a thread might mutate the static (global) object while the GC is running.  
  
So we ended up creating a rather elaborate patch that marks the statics over again when cycling over all the threads. This slows the GC thread slightly but should have no impact on app performance. 

We also introduced new support for the Lollipop action bar and some other cool new features. As part of that change we will soon start building Android apps with target set to API level 21. One of the reasons we held back on that change was that some code relied on the older version and we didn’t want to break that code, if you notice things acting differently in your builds please let us know. Notice that this means that the target SDK not the minimum SDK is 21 hence everything should still work all the way back to cupcake.

To customize the colors of the ActionBar on Lollipop define a colors.xml file in the native/android directory of your project. It should look like this:  

  
  
  
  
  
  
  

* * *

  
We considered adding this as a build argument but there are a few more customizations Google allows and this might just become painful to maintain as a build argument.  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
