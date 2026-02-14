---
title: Animated Gif Support
slug: animated-gif-support
url: /blog/animated-gif-support/
original_url: https://www.codenameone.com/blog/animated-gif-support.html
aliases:
- /blog/animated-gif-support.html
date: '2017-08-07'
author: Shai Almog
---

![Header Image](/blog/animated-gif-support/vacation.jpg)

So you know how you write a blog post just before you go on vacation, press publish and never check that it actually got published…​ Funny thing, that’s exactly what I did and the blog post mentioning that I was on “vacation” for a couple of weeks never got published. Anyway, other people have been busy while I was “away” but I got a couple of things done too including animated gif support.

Before we get to that Steve did a lot of work on Mac retina display support. This is a HUGE leap in usability if you use a retina Mac. It makes the iPhone 3gs skin tiny but you can now use the iPhone 5 skin without scaling…​ It looks great and uses the pixels on these Macs really well.

I also released a new [cn1lib that implements animated GIF support](https://github.com/codenameone/AnimatedGifSupport/) in Codename One without the resource file hack. It’s still not something I would recommend as animated gifs can be pretty expensive in terms of resources but you can still use it to get a pretty decent animation.

One of the cool things is that this works as a plug in image and you should be able to use it in most places where image works. There are caveats though. E.g. you can’t use it as a native map marker as that image is passed to native. But other than such API’s it should work in labels and even in background image styles, although I would suggest avoiding the latter as it would be a memory/battery drain.

The library is in the extensions section and you can use it like this:
    
    
    Form hi = new Form("Gif", new BorderLayout());
    
    try {
        hi.add(CENTER, new ScaleImageLabel(GifImage.decode(getResourceAsStream("/giphy-downsized.gif"), 1177720)));
    } catch(IOException err) {
        log(err);
    }
    hi.show();

Notice the following:

  * `GifImage.decode` can throw an `IOException`

  * It accepts an `InputStream` and the length of the input stream so you need to know the size in advance

  * It returns a `GifImage` which is a subclass of `Image`
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **salah Alhaddabi** — August 10, 2017 at 1:01 pm ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-23745))

> salah Alhaddabi says:
>
> Thanks a lot Shai.
>
> So does this mean that the image will be animated continously??
>



### **Shai Almog** — August 11, 2017 at 7:22 am ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-23653))

> Shai Almog says:
>
> It will loop based on the loop settings in the GIF itself. GIF’s contain a loop count. If it’s 0 it means looping forever.
>



### **Francesco Galgani** — August 16, 2017 at 5:19 pm ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-23444))

> Francesco Galgani says:
>
> Thank you 🙂  
> How are the various densities managed by animated GIFs? Is there any multi-image equivalent for GIF?
>



### **Shai Almog** — August 17, 2017 at 4:39 am ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-24151))

> Shai Almog says:
>
> We don’t. GIF has no density support so it can only be scaled. Using an approach like multi-image with GIF would be prohibitive as the file size will balloon. GIF’s are huge enough as it is.
>



### **Francesco Galgani** — August 17, 2017 at 9:39 am ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-23637))

> Francesco Galgani says:
>
> Mmm… is there any way to get the right animated GIF size using an external service such as Cloudinary? I’ve never used it, so I don’t know if it supports animated GIFs.
>



### **Shai Almog** — August 18, 2017 at 5:56 am ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-23418))

> Shai Almog says:
>
> I don’t know. I’m not familiar with that.
>



### **Rainer** — August 23, 2017 at 7:52 pm ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-24219))

> Rainer says:
>
> Hello! I tried the sample code with an animated gif, but nothing appears with the simulator
>



### **Shai Almog** — August 24, 2017 at 9:04 am ([permalink](https://www.codenameone.com/blog/animated-gif-support.html#comment-23695))

> Shai Almog says:
>
> Do you see any error in the console?  
> Have you tried with a different gif file?
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
