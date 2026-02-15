---
title: Flamingo SVG Transcoder Revisited
slug: flamingo-svg-transcoder-revisited
url: /blog/flamingo-svg-transcoder-revisited/
original_url: https://www.codenameone.com/blog/flamingo-svg-transcoder-revisited.html
aliases:
- /blog/flamingo-svg-transcoder-revisited.html
date: '2019-06-11'
author: Shai Almog
---

![Header Image](/blog/flamingo-svg-transcoder-revisited/uidesign.jpg)

A couple of years ago I wrote about our support for [Flamingo](/blog/flamingo-svg-transcoder.html) which translates static SVG files to Java source files that you can treat as images within Codename One while getting pixel perfect resolution independent results. There were a few minor changes since until a month ago when Steve committed some work to address this [RFE](https://github.com/codenameone/CodenameOne/issues/2797).

What’s SVG and Why Should I Care?

If you’re new to SVG here’s a quick primer. There are two types of images: Vector and Raster. Raster is what most of us work with every day. They are our JPEG, GIF, BMP and PNG images. They store the pixels of the image. There are many nuances here but I’ll leave it at that…​

Vector images are something completely different. Instead of storing the pixels they store the primitive drawing operations. If we have an image of a triangle, a raster image will store a lot of pixel values representing the triangle whereas a vector image would just store the fact that a triangle is drawn at a specific size in a given coordinate.

Vector images work great for images that were designed to be vector images e.g. logos, drawings etc. They are useless for things such as photos. They have a couple of big advantages:

  * They are smaller than raster images both on disk and more importantly in RAM

  * They can scale to any size with no degradation in image quality, this is very helpful for mobile apps

But surprisingly they might not perform as well as a raster image for all cases. This might be offset by the RAM savings so I’d still recommend SVG for most cases.

Flamingo works by translating SVG’s to Java source code that you can compile in Codename One. This means more elaborate features such as SVG animations are discarded as part of the process. It also means some SVG’s just won’t work. With the new update we added support for a lot of SVG’s and also updated the [flamingo binary distribution](https://github.com/codenameone/flamingo-svg-transcoder/blob/master/flamingo-svg-transcoder-core-1.2-jar-with-dependencies.jar) which we didn’t have in the original project.

The recent changes include both improvement to the translation process and to the Codename One API. Our updated API now supports features such as paint and gradient drawing which is commonly used in SVG. Specifically Steve added a `Paint` interface, with concrete subclass `LinearGradientPaint` that can be used to fill a shape with a gradient via `Graphics.setColor(Paint)` then `Graphics.fillShape(shape)`. There are quite a few other related changes such as `MultipleGradientPaint` and `AffineTransform`…​

With these in place you can convert a lot of typical SVG’s and use them in your app. The big question is whether we’ll integrate this deeper into Codname One by automating this process. Right now we’re not sure about that or how such integration will look. Maybe something similar to the CSS integration.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Durank** — September 16, 2020 at 2:48 pm ([permalink](/blog/flamingo-svg-transcoder-revisited/#comment-24339))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> how can I use svg image in code using flamingo? I have found some examples but without success
>



### **Shai Almog** — September 17, 2020 at 4:10 am ([permalink](/blog/flamingo-svg-transcoder-revisited/#comment-24337))

> Shai Almog says:
>
> They are just images. Instead of loading it you create a class instance and use it as an image.  
> “`  
> Image img = new SvgImage();  
> “`
>



### **Durank** — October 20, 2020 at 3:06 pm ([permalink](/blog/flamingo-svg-transcoder-revisited/#comment-24357))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> how can I to reference my image.svg?
>



### **Shai Almog** — October 21, 2020 at 2:09 am ([permalink](/blog/flamingo-svg-transcoder-revisited/#comment-24358))

> Shai Almog says:
>
> You just create a new instance of the generated Java source image. It’s an Image. There’s no SVG anymore.
>



### **Durank** — October 21, 2020 at 3:23 pm ([permalink](/blog/flamingo-svg-transcoder-revisited/#comment-24359))

> [Durank](https://avatars0.githubusercontent.com/u/16245755?v=4) says:
>
> please provide an example. I never have worked with svg in codenameone.
>



### **Shai Almog** — October 22, 2020 at 10:47 am ([permalink](/blog/flamingo-svg-transcoder-revisited/#comment-24361))

> Shai Almog says:
>
> There’s no example. You just create an instance of the generated class. It’s a subclass of Image so acts as an image. 
>
> You can see sample usage here: <https://github.com/codenameone/flamingo-svg-transcoder/>
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
