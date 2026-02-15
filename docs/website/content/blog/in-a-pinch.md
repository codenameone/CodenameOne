---
title: In A Pinch
slug: in-a-pinch
url: /blog/in-a-pinch/
original_url: https://www.codenameone.com/blog/in-a-pinch.html
aliases:
- /blog/in-a-pinch.html
date: '2013-08-06'
author: Shai Almog
---

![Header Image](/blog/in-a-pinch/in-a-pinch-1.jpg)

  
  
  
  
![Picture](/blog/in-a-pinch/in-a-pinch-1.jpg)  
  
  
  

Codename One has supported multi-touch and effectively pinch to zoom events from the very first release. However, it wasn’t intuitive to write code that would handle pinch to zoom. We just committed new pinch callback events to component which effectively allows you to zoom in/out with gestures.  
  
  
  
  
  
Effectively if you are interested in handling pinch you would just override:  
  
protected boolean pinch(float scale) and return true after handling the pinch operation. We did this to support our new  
[  
ImageViewer class  
](http://code.google.com/p/codenameone/source/browse/trunk/CodenameOne/src/com/codename1/components/ImageViewer.java)  
(where you can see a working sample of pinch), the image viewer allows you to inspect, zoom and pan into an image. It also allows swiping between images if you have a set of images (using an image list model). 

  
Now to simulate the pinching we needed a simpler solution than just building for the device, which is why we added right click dragging. Now when you drag using the right mouse button (or by pressing 2 fingers on a Mac laptop and dragging together) you will get a pinch effect. We do this by hardcoding the second finger at position 0,0. So effectively if you drag towards the top left corner you will be zooming out (fingers closer together) and by dragging away you will be zooming in.  

Here is a simple example of using the ImageViewer:  

* * *

Notice that we use a list to allow swiping between images (unnecessary if you have only one image), we also create a placeholder image to show while the image is still loading. Notice that encoded images aren’t always fully loaded and so when you swipe if the images are really large you might see delays!  
  
  
  
  
This leads me to one of the more important aspects here: image locking. 

**  
  
Image Locking  
  
**  
  
  
  
  
Once of the big performance improvements we got in swiping was through image locking, unfortunately this is so unclear in our current docs that when Nokia forked LWUIT they actually removed the usage of EncodedImage instead of fixing a minor locking bug.  
  
  
  
To understand locking we first need to understand EncodedImage. EncodedImage stores the data of the image in RAM (png or JPEG data) which is normally pretty small, unlike the full decoded image which can take up to width X height X 4. When a user tries to draw an encoded image we check a WeakReference cache and if the image is cached then we show it otherwise w  
  
e load the image, cache it then draw.

  
Naturally loading the image is more expensive so we want the images that are showing to stay in cache (otherwise GC will thrash a lot).  
  
That’s where lock() kicks in, we automatically invoke image loading when necessary but when lock() is active we keep a hard reference to the actual native image so it won’t get GC’d.  
  
This REALLY  
  
improves performance!  
  
  
Internally we invoke this automatically for bg images, icons etc. which results in a huge performance boost. However, if you use a complex renderer or custom drawing UI you should lock() your images where possible!  
  
  
  
  
  
**  
  
  
Generics Update  
**  
  
You might have noticed from the code above that the list model is now generified, the same is also true now for the list renderer and list. We are working on modernizing as much of our code as we can while maintaining backwards compatibility.  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — August 12, 2013 at 8:28 am ([permalink](/blog/in-a-pinch/#comment-21826))

> Anonymous says:
>
> This is one of missing APIs,what I am waiting for. Great!!
>



### **Mahmoud** — April 3, 2016 at 7:35 am ([permalink](/blog/in-a-pinch/#comment-22797))

> Mahmoud says:
>
> dear shai  
> i have dynamic list in ImageViewer( images from url)  
> i want to swipe images not on touch only but in pressing button too (swiping left and right)  
> how can i do it
>
> thanks,
>



### **Shai Almog** — April 4, 2016 at 2:30 am ([permalink](/blog/in-a-pinch/#comment-22808))

> Shai Almog says:
>
> Hi,  
> there is a code sample within the JavaDocs that covers just that use case: [https://www.codenameone.com…](</javadoc/com/codename1/components/ImageViewer/>)  
> It’s also included in the developer guide section of the ImageViewer:  
> [https://www.codenameone.com…](</manual/components/#_imageviewer>)
>



### **Mahmoud** — April 5, 2016 at 8:11 am ([permalink](/blog/in-a-pinch/#comment-22492))

> Mahmoud says:
>
> hi Shai,  
> thank you …  
> i use setSelectedIndex method in ImageList  
> int myNewIndex=myImageViewer.getImageList().getSelectedIndex()(+1 or -1); to swiping left and right  
> and  
> myImageViewer.getImageList().setSelectedIndex(myNewIndex);
>
> BR,
>



### **Mahmoud** — June 25, 2016 at 1:24 am ([permalink](/blog/in-a-pinch/#comment-22758))

> Mahmoud says:
>
> dear shai,  
> i have ImageViewer the image size 800X400
>
> when i run my app on small device the image looks like attached image (there’s a space on top and bottom of image )  
> any idea please
>



### **Shai Almog** — June 25, 2016 at 5:56 am ([permalink](/blog/in-a-pinch/#comment-22624))

> Shai Almog says:
>
> Images default to “scale to fit” in the image viewer.
>



### **Mahmoud** — June 25, 2016 at 11:05 pm ([permalink](/blog/in-a-pinch/#comment-21519))

> Mahmoud says:
>
> ok how i can change it
>



### **Shai Almog** — June 26, 2016 at 4:48 am ([permalink](/blog/in-a-pinch/#comment-22800))

> Shai Almog says:
>
> You can change the zoom level, but are you trying to show a static image or allow the user to change that? If the former then image viewer is the wrong component to use.
>



### **Mahmoud** — June 26, 2016 at 7:13 am ([permalink](/blog/in-a-pinch/#comment-22851))

> Mahmoud says:
>
> dynamic list in ImageViewer( images from url) i show it as slider
>



### **Shai Almog** — June 27, 2016 at 3:06 am ([permalink](/blog/in-a-pinch/#comment-22860))

> Shai Almog says:
>
> So if you have multiple images in the viewer and flip between them zooming in will be a mistake.  
> This can create an image whose edges are wider than the screen so when you try to swipe you will really pan within the image…  
> You can replicate that behavior by zooming in with pinch and trying to swipe to see what I mean.
>



### **Mahmoud** — July 1, 2016 at 4:09 pm ([permalink](/blog/in-a-pinch/#comment-22901))

> Mahmoud says:
>
> i have list of images not multi-images and i have Image Viewer  
> if you understand what i mean , is any way to create slider images without space on top and bottom of image in small device  
> thanks a lot
>



### **Shai Almog** — July 2, 2016 at 4:29 am ([permalink](/blog/in-a-pinch/#comment-22959))

> Shai Almog says:
>
> If I understand correctly you want to do something similar to the phone picture gallery where you swipe between the photos you took.
>
> If you look at that native OS UI you will notice space above or on the sides to keep the aspect ratio of the image in place as you swipe. If you are speaking of a different UI/UX I’ll need a reference.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
