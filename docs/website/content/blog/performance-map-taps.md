---
title: Performance & Map Taps
slug: performance-map-taps
url: /blog/performance-map-taps/
original_url: https://www.codenameone.com/blog/performance-map-taps.html
aliases:
- /blog/performance-map-taps.html
date: '2014-06-15'
author: Shai Almog
---

![Header Image](/blog/performance-map-taps/performance-map-taps-1.jpg)

  
  
  
  
![Picture](/blog/performance-map-taps/performance-map-taps-1.jpg)  
  
  
  

We neglected to mention in our last post about the new graphics pipeline that it was authored by Steve Hannah who did a splendid job there! He just updated the shaders for the implementation to be far more efficient bringing performance back to the current levels. The true value of this architecture is that now we will be able to manipulate shaders for very complex effects at relatively low cost.  
  
  
  
  
We are also integrating some improvements into the Android drawing pipeline which should incorporate some of these features in the new Android pipeline architecture. Notice that the new pipeline is only in effect for Android 3.x or higher and seamlessly uses a legacy pipeline for 2.x devices.  
  
  
  
  
The  
[  
native map implementation  
](http://www.codenameone.com/3/post/2014/03/mapping-natively.html)  
had an issue with detecting pointer events, since its a  
[  
peer component  
](http://www.codenameone.com/3/post/2014/05/understanding-peer-native-components-why-codename-one-is-so-portable.html)  
you couldn’t necessarily monitor the standard pointer events. So we added a new API allowing you to addTapListener to a map that would broadcast an event from a native map with an x/y location.  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
