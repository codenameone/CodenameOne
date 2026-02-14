---
title: New Graphics Pipeline
slug: new-graphics-pipeline
url: /blog/new-graphics-pipeline/
original_url: https://www.codenameone.com/blog/new-graphics-pipeline.html
aliases:
- /blog/new-graphics-pipeline.html
date: '2014-06-10'
author: Shai Almog
---

![Header Image](/blog/new-graphics-pipeline/new-graphics-pipeline-1.png)

  
  
  
  
![Picture](/blog/new-graphics-pipeline/new-graphics-pipeline-1.png)  
  
  
  

Finally the new graphics pipeline is starting to trickle into the JavaSE port and the iOS port & once we iron those two out it should make its way into the Android port.  
  
With this in place we are finally at a point of functionality similar to JavaFX but without the overhead and performance implications that FX carries with it. 

The new pipeline includes new API’s for shapes, affine/perspective transforms and more. Notice that unless you are a graphics geek these things won’t mean much to you, but these things allow us to expose complex features (e.g. high performance charts, special effects) thru the high level API’s. Since not all devices will support these capabilities we they each include an is*Supported method in Graphics and if your painting code needs access to that capability it should test that availability and provide a reasonable fallback. Right now most of the capabilities should be supported for iOS and Java SE with Android support arriving later.

Notice that even when supported some features can’t be exposed on all platforms, e.g. perspective transform isn’t available on our current JavaSE port and might not be available in the future since Java2D doesn’t support that. JavaFX introduced some form of perspective transform but this effectively means rewriting a great deal of code to use the JavaFX API’s which is a non-trivial task. Hopefully this will be easier to integrate on the Android front.

To enable the new pipeline on iOS you will need to use the build argument ios.newPipeline=true  
  
Its currently off by default to maximize compatibility and prevent regressions, however we recommend that you test your apps with this flag even if you don’t currently need this feature so we can be ready for flipping the default state for this switch.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 12, 2014 at 9:14 pm ([permalink](https://www.codenameone.com/blog/new-graphics-pipeline.html#comment-21805))

> Anonymous says:
>
> Does the Android version use Opengl for drawing or draws in Canvas? How suitable is this for intense animations or games?
>



### **Anonymous** — June 13, 2014 at 2:44 am ([permalink](https://www.codenameone.com/blog/new-graphics-pipeline.html#comment-24214))

> Anonymous says:
>
> No. We use different things based on the version of Android since performance changed completely between 2.x and 4.x. 
>
> We generally draw on canvas since we need integration with native widgets that might be problematic if we use OpenGL. We might provide an OpenGL API in the future for gaming purposes. 
>
> Our current focus is on apps and to a lesser degree on lightweight games. If you have a need for intense animations or high framerate animations you would probably need to use native code at this time.
>



### **Anonymous** — June 16, 2014 at 9:00 am ([permalink](https://www.codenameone.com/blog/new-graphics-pipeline.html#comment-22039))

> Anonymous says:
>
> Sounds great, thanks! 
>
> Is there some discussion (javadoc or methods list) regarding the API released with this new graphics pipeline ? 
>
> Thank you !
>



### **Anonymous** — June 16, 2014 at 1:08 pm ([permalink](https://www.codenameone.com/blog/new-graphics-pipeline.html#comment-22067))

> Anonymous says:
>
> Not much, but the Graphics class and the geom package should contain all the relevant methods/classes.
>



### **Anonymous** — June 17, 2014 at 11:46 am ([permalink](https://www.codenameone.com/blog/new-graphics-pipeline.html#comment-22050))

> Anonymous says:
>
> Thanks, I will have a look. 
>
> Does this new graphic pipeline has any impact regarding the need to change the gradiant definition of a background by a picture (for permance issues) ? 
>
> thanks
>



### **Anonymous** — June 17, 2014 at 4:49 pm ([permalink](https://www.codenameone.com/blog/new-graphics-pipeline.html#comment-21977))

> Anonymous says:
>
> Not yet. However, since we use shaders many special effects can be moved into the pipeline such as gradients, shaped clipping, etc. We are reserving these to future incremental enhancements based on user feedback.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
