---
title: Pisces & String Picker
slug: pisces-string-picker
url: /blog/pisces-string-picker/
original_url: https://www.codenameone.com/blog/pisces-string-picker.html
aliases:
- /blog/pisces-string-picker.html
date: '2013-12-31'
author: Shai Almog
---

![Header Image](/blog/pisces-string-picker/pisces-string-picker-1.jpg)

  
  
  
  
![Picture](/blog/pisces-string-picker/pisces-string-picker-1.jpg)  
  
  
  

[  
Steve  
](http://sjhannah.com/)  
just released a very important  
[  
cn1lib  
](/cn1libs.html)  
that effectively provides developers with elaborate low level graphics primitives to draw pretty much everything on all platforms. The main use case for this library is for charts and graphs which up until now we had to do with the relatively limited graphics capabilities of Codename One (which are currently optimized for speed/portability).  
  
  
  
  
Pisces is a rendering engine developed at Sun that was designed to be very portable, it performs all the rendering in Java and can thus do relatively advanced graphics even on J2ME devices that don’t necessarily have such graphics capabilities. The downside is that it can’t be fast since it effectively can’t use the GPU.  
  
  
  
  
We are working on adding “native” support for advanced graphics capabilities that will use hardware acceleration to bring this level of graphics to all use cases. This is obviously a tall order and would only work on smartphones so the utility of the Pisces port will remain even when we provide such an API.  
  
  
  
  
Since the Pisces API outputs an image, the drawing of the image itself should be pretty fast so for the use case of static chart creation this tool should be more than enough.  
  
  
  
  
Other than that we will be introducing support for a String picker in the upcoming update of Codename One. Essentially the Picker class will also allow you to set a string array to pick from just like it allows you to pick a date/time etc.  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 3, 2014 at 8:36 am ([permalink](/blog/pisces-string-picker/#comment-22011))

> Anonymous says:
>
> Hello, when are you releasing the upcoming update?
>



### **Anonymous** — January 4, 2014 at 6:07 am ([permalink](/blog/pisces-string-picker/#comment-24255))

> Anonymous says:
>
> Yesterday.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
