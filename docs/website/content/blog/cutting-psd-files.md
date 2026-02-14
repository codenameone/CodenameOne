---
title: Cutting PSD Files
slug: cutting-psd-files
url: /blog/cutting-psd-files/
original_url: https://www.codenameone.com/blog/cutting-psd-files.html
aliases:
- /blog/cutting-psd-files.html
date: '2013-04-07'
author: Shai Almog
---

![Header Image](/blog/cutting-psd-files/cutting-psd-files-1.png)

This post is inspired by a great post written by  
[  
Tope  
](http://www.appdesignvault.com/photoshop-crop)  
, covering the slicing of a PSD image to produce small PNG images which you can later on use as image borders, backgrounds, icons etc. Tope’s technique is pretty simple and works rather well but I’d like to offer another technique as well as a better way to detect the proper layer you with to cut.

  
  
  
[  
![Picture](/blog/cutting-psd-files/cutting-psd-files-1.png)  
](/img/blog/old_posts/cutting-psd-files-large-4.png)

This post assumes you have a recent version of Photoshop installed and assumes you don’t know anything about Photoshop. So we start by opening the PSD file in Photoshop, this file is composed of layers. A single “component” is usually composed of multiple layers which you can show/hide by pressing the “eye” button in the layer view. 

  
  
  
You can see the layer view by selecting Windows->Layers from the photoshop menu, on the right you can see the layers of a simple iPhone design that I have here. You will notice that every entry is collapsible.  
  
A common paradigm designers use is to create multiple screens/forms in a singled PSD  
  
and thus represent a “screen” (Form) of its own within the design.  
  
  
So in order to see the other forms for those cases you can just hide/show each layer, in order to show individual components you can  
  
use the eye icon to get a specific component.

  
Now our goal is to find a specific set of layers relevant to us and “hide” everything else so we can get the particular component we need in isolation.  
  
  
This is pretty easy when the design is small, but just locating the right layer becomes a HUGE hassle as the design gets complicated and deeply nested.  
  

* * *

  
  
  
[  
![Picture](/blog/cutting-psd-files/cutting-psd-files-2.png)  
](/img/blog/old_posts/cutting-psd-files-large-5.png)

To find the layer matching a specific component we select the “Move Tool” from the toolbar and check the auto select option in the toolbar above, we then pick the “Layer” entry instead of group.  
  
  
Now when we click an area on the screen the layer corresponding to this specific entry will be selected in the layer view and we could manipulate it. Notice that a component is often composed of multiple layers… We usually would want to hide things such as the text layers etc. for cases such as buttons where we would want to get the button alone so we can cut it into a 9-piece border, but we would want other layers.  
  

* * *

  
  
  
[  
![Picture](/blog/cutting-psd-files/cutting-psd-files-3.png)  
](/img/blog/old_posts/cutting-psd-files-large-6.png)

Now say I want to extract an image that is comprised of the following 3 layers, I can select all 3 layers then right click on them (important! Notice that you need to click on the area where the text appears NOT on the icon of the layer, you will get a different context menu otherwise!). You will get an option to convert the layers to a smart object.  
  
  
After converting to a smart object double click the layer icon (you will get a dialog with a message that is relevant only if you are interested in really changing the file), the standalone image will open in a separate tab and you will be able to use the Save As option and select PNG as the format.  
  

* * *

I hope this tutorial together with the very detailed tutorial from Tope will help you cut images from PSD’s more effectively and help you create better looking apps.  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — September 30, 2013 at 3:28 am ([permalink](https://www.codenameone.com/blog/cutting-psd-files.html#comment-21890))

> Anonymous says:
>
> there is a nice alternative solution to do this process automatically, 
>
> free little extension called Breeezy it adds to Photoshop the ability to export multiple layers in one click 
>
> you can take a look 
>
> [breeezyplugin.com](<http://breeezyplugin.com>)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
