---
title: Lollipop Take 2 & iOS VM Stability
slug: lollipop-take-2-ios-vm-stability
url: /blog/lollipop-take-2-ios-vm-stability/
original_url: https://www.codenameone.com/blog/lollipop-take-2-ios-vm-stability.html
aliases:
- /blog/lollipop-take-2-ios-vm-stability.html
date: '2014-12-14'
author: Shai Almog
---

![Header Image](/blog/lollipop-take-2-ios-vm-stability/lollipop-take-2-ios-vm-stability-1.jpg)

  
  
  
  
![Picture](/blog/lollipop-take-2-ios-vm-stability/lollipop-take-2-ios-vm-stability-1.jpg)  
  
  
  

  
  
  
  
Our  
[  
previous attempt at getting the new Lollipop behavior  
](http://www.codenameone.com/blog/static-garbage-lollipop)  
on Android OS 5 didn’t go as well as we had hoped. While we can’t find any device that failed we got a lot of community reports of various 4.x devices that just stopped working right after we made the changes recommended by Google. We tried to resolve them using several different tricks all of which proved futile. Eventually we just reverted the whole thing and went back to the drawing board, it seems Android fragmentation is worse than we feared. 

We are now ready for another shot at this. This Tueday we will deploy a second attempt at the Lollipop action bar which will hopefully not break everything. This time the new action bar will only work for newer Android devices and older devices will fallback to the original action bar or old title bar. Make sure to test your apps as much as possible, if you are one of the people who ran into issues with our previous release take extra time to check that we didn’t break things again this Tuesday.

We also spent a lot of time working on the new iOS VM and fixed a lot of bugs there, we are rapidly closing the gap with the old VM and seem to be on schedule for the January release. We are still seeing projects that don’t compile and various issues, if you are one of the guys who experienced a problem with the new VM be sure to  
[  
file an issue right away  
](https://code.google.com/p/codenameone/issues)  
!  
  

  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
