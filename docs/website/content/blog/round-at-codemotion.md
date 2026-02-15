---
title: Round At Codemotion
slug: round-at-codemotion
url: /blog/round-at-codemotion/
original_url: https://www.codenameone.com/blog/round-at-codemotion.html
aliases:
- /blog/round-at-codemotion.html
date: '2014-11-30'
author: Shai Almog
---

![Header Image](/blog/round-at-codemotion/round-at-codemotion-1.png)

  
  
  
  
![Picture](/blog/round-at-codemotion/round-at-codemotion-1.png)  
  
  
  

  
  
  
  
  
  
  
  
  
I’ve had a lovely time giving a demo of Codename One at  
[  
Codemotion Tel Aviv  
](http://telaviv.codemotionworld.com/)  
, one of the things that surprised me about the conference is that the sessions are so short (40 minutes) which gives very little time to actually get into code. So to fit both details about Codename One, Demo etc. and an app I had to narrow this down to the most barebone Codename One demo I could think of that could still be valuable.  
  
Enter Rounder, its a trivial demo of Codename One that just uses the camera to grab a photo and make it into a round photo. Not exactly useful but it shows the usage of graphics, masking and image capture in a trivial app.  
  
The full code is posted below and can help you if you are looking into image masking.  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 2, 2014 at 7:38 am ([permalink](/blog/round-at-codemotion/#comment-22146))

> Anonymous says:
>
> It would be even super if there is a standard image cropping CodenameOne component where we can crop an image by drawing a square/circle frame over the area of the image to crop.
>



### **Anonymous** — December 2, 2014 at 3:10 pm ([permalink](/blog/round-at-codemotion/#comment-21476))

> Anonymous says:
>
> You can use subimage for standard crop and code similar to the one above for shaping. I don’t follow the issue here?
>



### **Anonymous** — December 3, 2014 at 4:58 am ([permalink](/blog/round-at-codemotion/#comment-22318))

> Anonymous says:
>
> Hi Shai, its just a suggestion. What I mean is it would be great if there is a standard CodenameOne component utility class where we can use it to crop the image by selecting the area of the photo visually (see: [http://www.ajaxshake.com/en…](<http://www.ajaxshake.com/en/JS/22701/mootools-image-cropping-utility-moocrop.html>)). If its easy for your team to add such a feature in, that would be very useful, just not sure if this feature crosses the boundaries of what CodenameoOne is suppose to offer, but then again, it wouldn’t hurt to exceed those boundaries a little bit more ;).
>



### **Anonymous** — December 4, 2014 at 6:07 am ([permalink](/blog/round-at-codemotion/#comment-21596))

> Anonymous says:
>
> I think that’s a bit beyond our scope right now. But you can do it as a cn1lib which is the exact reason why we have them: [http://www.codenameone.com/…](<http://www.codenameone.com/cn1libs.html>)
>



### **Anonymous** — December 4, 2014 at 1:13 pm ([permalink](/blog/round-at-codemotion/#comment-22326))

> Anonymous says:
>
> Thanks Shai, I think I’ll have a go at it a bit later and perhaps contribute the component to the cn1lib when its done. Right now the first thing comes to mind is having 4 corners implemented as transparent circle buttons that serve as anchor points for the clipping square that can be drawn accordingly when being pinched and expanded. What do you think? Or do you think there is an easier approach? I am not sure if this will be efficient to do in CodenameOne. It would be great if you can point me to classes and methods that will be useful to achieve this outcome. Thanks!
>



### **Anonymous** — December 5, 2014 at 3:24 am ([permalink](/blog/round-at-codemotion/#comment-21996))

> Anonymous says:
>
> You can check out the code of ImageViewer which does most of this and also implements pinching/panning etc. 
>
> Personally I’d just have an image viewer in a layered layout and place a styled overlay for the cropped area with a “Crop” button below. When the user presses the crop just do it based on the current state of the ImageViewer. The main challenge might be some changes you might need from the image viewer to get its current state.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
