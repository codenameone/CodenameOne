---
title: Screenshots and Graphics Update
slug: screenshots-and-graphics-update
url: /blog/screenshots-and-graphics-update/
original_url: https://www.codenameone.com/blog/screenshots-and-graphics-update.html
aliases:
- /blog/screenshots-and-graphics-update.html
date: '2014-04-27'
author: Shai Almog
---

![Header Image](/blog/screenshots-and-graphics-update/screenshots-and-graphics-update-1.jpg)

  
  
  
  
![Picture](/blog/screenshots-and-graphics-update/screenshots-and-graphics-update-1.jpg)  
  
  
  

If you read the article about the  
[  
7 screenshots of iOS  
](http://www.codenameone.com/3/post/2014/03/the-7-screenshots-of-ios.html)  
you might have wondered whether you can just supply these screenshots yourself? 

Well, now you can. Our build server will now generate the screenshots only if they don’t already exist in the jar so you will need to create the right png images in the exact resolutions mentioned below:  
  
Default.png – 320×480  
  
[[email protected]](/cdn-cgi/l/email-protection) – 640×960  
  
[[email protected]](/cdn-cgi/l/email-protection) – 630×1136  
  
Default-Portrait.png – 768×1024  
  
Default-Landscape.png – 1024×768  
  
[[email protected]](/cdn-cgi/l/email-protection) – 1536×2048  
  
[[email protected]](/cdn-cgi/l/email-protection) – 2048×1536

Notice that the names are case sensitive and the resolutions must match the numbers above.

We’ve been working for a while on a new Graphics pipeline and Shapes API which should include some pretty nifty features, the main code is mostly done for iOS. This includes a new Shape API that will allow you to define any arbitrary shape and stroke/fill it using common graphics idioms. This will be fully hardware accelerated which will allow us to offer rather elaborate effects such as good looking/fast charts.

The API will also expose a proper Affine Transform and potentially a perspective transform allowing for high performance rotation effects and 3D effects. This is all slated for 2.1 which we hope to release before Java One.

We will have a booth at this years Java One hopefully some of the sessions we submitted will also go thru.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — April 28, 2014 at 3:46 pm ([permalink](https://www.codenameone.com/blog/screenshots-and-graphics-update.html#comment-21830))

> Anonymous says:
>
> Hello, 
>
> Will the rotation and 3D effects be applied to Android and Windows ports?
>



### **Anonymous** — April 30, 2014 at 2:45 pm ([permalink](https://www.codenameone.com/blog/screenshots-and-graphics-update.html#comment-21772))

> Anonymous says:
>
> We are targeting Android and ideally desktop. 3D might be tricky but we would like them on both of these. 
>
> Windows Phone probably not in the near future.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
