---
title: Asynchronous Media
slug: asynchronous-media
url: /blog/asynchronous-media/
original_url: https://www.codenameone.com/blog/asynchronous-media.html
aliases:
- /blog/asynchronous-media.html
date: '2019-05-22'
author: Shai Almog
---

![Header Image](/blog/asynchronous-media/new-features-3.jpg)

There are a lot of fixes and new features that I don’t get to cover enough as I’ve been busy on several fronts. One of the new features is support for asynchronous media API’s. These let us create a media object without waiting for it to complete. This is very useful if you have a complex UI and want to play a media file while doing other things.

E.g. if you’re scrolling in a social network feed and want to play a media preview. You might create a media object but don’t want it to block the current call. You can do this using code such as:
    
    
    AsyncResource<Media> async = Display.getInstance().createMediaAsync(URL_TO_MEDIA, isVideo, null);
    async.ready(mediaInstance -> playMedia(mediaInstance));

You will notice the usage of `AsyncResource` which is similar to a future or a promise in other platforms. It lets you monitor the status of an asynchronous approach. This block would execute quickly but the `playMedia` call would happen when loading is completed.

### Rendering Hints

One of the API’s I dislike in JavaSE is the `Graphics2D` rendering hints. It’s a bit opaque in the choices it exposes. I want fast and good looking graphics but the tradeoff isn’t always clear. How much would I “pay” for good looking in this case in terms of speed and visa versa.

Now we also have one rendering hint in our graphics:
    
    
    graphics.setRenderingHints(Graphics.RENDERING_HINT_FAST);

I’m not too crazy about the name as it’s a bit misleading. I’m sure developers would just turn it on to make everything “go fast” then complain when it has no impact…​ It doesn’t do that.

Only iOS uses this and even then only when rendering images. Since copying images to textures is expensive, we keep the last generated texture cached. This works well if we are always rendering the image at the same size. If we are constantly rendering the same image at different sizes, then we’ll constantly be invalidating the cache, this results in artifacts. This affected pinch zoom in the image viewer [causing it to be choppy](https://github.com/codenameone/CodenameOne/issues/2786) as it had to regenerate a texture for every change in size. When Fast rendering is enabled, we now only invalidate the texture cache if the image is larger or smaller than the existing texture by more than a factor of 2.

This change also fixed a bug that caused images to be rendered as black if they are larger than the max OGL texture size. Now it will cap the size of the texture at the max OGL size, and it will use the GPU to scale the texture to the desired size.

### Uppercase

`TextArea` now supports a new `UPPERCASE` constraint which lets you request uppercase input. Generally it will just popup the keyboard with the capslock on. You can use it as such:
    
    
    textFieldOrArea.setConstrainer(TextArea.UPPERCASE);

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
