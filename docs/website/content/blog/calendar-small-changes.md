---
title: Calendar & Small Changes
slug: calendar-small-changes
url: /blog/calendar-small-changes/
original_url: https://www.codenameone.com/blog/calendar-small-changes.html
aliases:
- /blog/calendar-small-changes.html
date: '2014-02-11'
author: Shai Almog
---

![Header Image](/blog/calendar-small-changes/calendar-small-changes-1.png)

  
  
  
  
![Picture](/blog/calendar-small-changes/calendar-small-changes-1.png)  
  
  
  

I’ve been working a bit with Kapila and Andreas on a  
[  
Calendar project for Codename One  
](http://code.google.com/p/codenameone-calendar/)  
to be used as a  
[  
cn1lib  
](http://codenameone.com/cn1libs.html)  
(working is a strong word I’ve mostly just bossed them around and didn’t really do much, they did all the work). Its a pretty ambitious project since Calendar API’s are so fragmented and problematic, in fact no cross platform tool I’m familiar with has anything remotely close to a working Calendar API.  
  
  
If this is something important/dear to your heart feel free to pitch in and try to improve the implementation, file issues etc.  
  
  
  
  
  
  
In other news we’ve Improved facebook support on iOS to use the device native login where possible, this was actually implemented incorrectly and was brought to our attention.  
  
  
  
  
  
Last week we had one of the most annoying bugs I think we ever had to deal with. It seems that in some iPhones (only iPhones never iPads) camera capture crashes when saving the photo. We were never able to reproduce this on any of our devices but a customer was getting this consistently on his, eventually he lent us his personal iPhone and we spent half a day debugging this, turns out this is a known issue and the solution is: reboot the device… Ugh.  
  
  
  
  
We no longer have a device that reproduces this issue but we’ve incorporated one of the suggested workarounds for this issue into the code so we hope it works around that iOS bug. If it doesn’t and you are seeing crashes on devices when taking a picture with the current builds let us know!  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — February 26, 2014 at 2:49 pm ([permalink](https://www.codenameone.com/blog/calendar-small-changes.html#comment-21704))

> Anonymous says:
>
> Is this library finished or there is still some works going on? 
>
> Thanks
>



### **Anonymous** — February 26, 2014 at 3:54 pm ([permalink](https://www.codenameone.com/blog/calendar-small-changes.html#comment-21923))

> Anonymous says:
>
> It seems development has slowed down a little. As far as I understand the basic functionality Andreas and Kapilla need is working for them. I suggest you ask this on the forum where they can see it and respond with more information.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
