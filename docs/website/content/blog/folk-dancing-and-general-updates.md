---
title: Folk Dancing And General Updates
slug: folk-dancing-and-general-updates
url: /blog/folk-dancing-and-general-updates/
original_url: https://www.codenameone.com/blog/folk-dancing-and-general-updates.html
aliases:
- /blog/folk-dancing-and-general-updates.html
date: '2013-08-25'
author: Shai Almog
---

![Header Image](/blog/folk-dancing-and-general-updates/hqdefault.jpg)

  

Who would have thought Hungarian folk dance would be so entertaining! Can’t. stop. watching….  
  
  
  
Its been a busy week mostly spent on updating the build server code so its iOS 7 ready, during that time we also managed to get some other things done… These are some of the highlights: 

You may recall the  
[  
ImageViewer class that I mentioned a while back  
](http://www.codenameone.com/3/post/2013/08/in-a-pinch.html)  
, it will now be a part of the designer and has some small improvements to its event handling as well as keyboard handling.

We now have a new set of image rotation API’s designed for camera images. These API’s can rotate or flip an image by square angles, which is important for rotating an image that isn’t a perfect square (e.g. taken by camera). These API’s are really simple to use e.g.: image.rotate90Degrees(true);

Where the boolean argument indicates whether the image is opaque (e.g. for the case of a camera image). There are also methods for 180 and 270 degrees as well as vertical and horizontal flip methods.

Up until recently we always told people to avoid the scaled method as much as possible, the main issue was that an image gets decoded and then is very expensive to hold in RAM. This is not necessarily the case anymore. Now if you invoke the scaled() method on an EncodedImage (assuming you are not on a feature phone) the ImageIO class will be used to re-encode the resulting image and allow you the same benefits as any other encoded image.  
  
We still don’t recommend scaled since its still less efficient than drawing and not as sharp as multi image. It will now also be slightly slower but it will have a better memory footprint.

We also added a feature that allows you to create an encoded image from ARGB data, again using the ImageIO class, this isn’t guaranteed to work (since ImageIO doesn’t necessarily exist in all devices) but when it does it should be more efficient for most use cases. 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — August 27, 2013 at 3:46 am ([permalink](https://www.codenameone.com/blog/folk-dancing-and-general-updates.html#comment-24253))

> Anonymous says:
>
> ImageViewer is deleted in new version have any class for instead for it ?
>



### **Anonymous** — August 27, 2013 at 4:51 am ([permalink](https://www.codenameone.com/blog/folk-dancing-and-general-updates.html#comment-21793))

> Anonymous says:
>
> No its not, its under components.
>



### **Anonymous** — August 29, 2013 at 7:49 am ([permalink](https://www.codenameone.com/blog/folk-dancing-and-general-updates.html#comment-21736))

> Anonymous says:
>
> Tell me your scaled() works for j2me.
>



### **Anonymous** — August 29, 2013 at 4:27 pm ([permalink](https://www.codenameone.com/blog/folk-dancing-and-general-updates.html#comment-21707))

> Anonymous says:
>
> Sure, but it doesn’t feature this optimization.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
