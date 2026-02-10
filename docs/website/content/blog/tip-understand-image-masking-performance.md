---
title: 'TIP: Understand Image Masking Performance'
slug: tip-understand-image-masking-performance
url: /blog/tip-understand-image-masking-performance/
original_url: https://www.codenameone.com/blog/tip-understand-image-masking-performance.html
aliases:
- /blog/tip-understand-image-masking-performance.html
date: '2017-05-28'
author: Shai Almog
---

![Header Image](/blog/tip-understand-image-masking-performance/tip.jpg)

[Image masking](/blog/round-at-codemotion.html) allows us to adapt an image which we acquired from an external source to fit our design e.g. if we want to show an image cropped to a circle we could apply a mask to it in order to get an intelligent crop. This is a very powerful tool as a designer can supply a hardcoded mask image and build some pretty complex shapes that include an alpha channel as well (making it superior to shape clipping).

The most simple type of image masking can be achieved using code like this taken from [this old post](/blog/round-at-codemotion.html):
    
    
    try {
        int width = Display.getInstance().getDisplayWidth();
        Image capturedImage = Image.createImage(Capture.capturePhoto()).fill(width, width);
        Image roundMask = Image.createImage(width, capturedImage.getHeight(), 0xff000000);
        Graphics gr = roundMask.getGraphics();
        gr.setColor(0xffffff);
        gr.setAntiAliased(true);
        gr.fillArc(0, 0, width, width, 0, 360);
        Object mask = roundMask.createMask();
        capturedImage = capturedImage.applyMask(mask);
        result.setIcon(capturedImage);
    } catch(IOException err) {
        Log.e(err);
    }

__ |  I made some minor improvements to the code   
---|---  
  
### Masking Approaches

There are several other ways to implement masking besides the low level approach illustrated above

#### URLImage Masking

`URLImage` can be created with a mask. Since masking replaces the underlying image with the masked image if you would use it with a `URLImage` you would effectively lose the implicit download functionality of the `URLImage`. The builtin masking allows us to workaround that behavior and still apply a mask to an image that is fetched implicitly.

To paraphrase the previous example this can be used like:
    
    
    Image roundMask = Image.createImage(placeholder.getWidth(), placeholder.getHeight(), 0xff000000);
    Graphics gr = roundMask.getGraphics();
    gr.setColor(0xffffff);
    gr.setAntiAliased(true);
    gr.fillArc(0, 0, placeholder.getWidth(), placeholder.getHeight(), 0, 360);
    
    URLImage.ImageAdapter ada = URLImage.createMaskAdapter(roundMask);
    Image i = URLImage.createToStorage(placeholder, "fileNameInStorage", "http://xxx/myurl.jpg", ada);

#### Theme Masking

Label and its subclasses can have a mask associated with an icon thru a theme constant. This means that all images set to the given icon will be implicitly masked within the set method. That can be very convenient as it allows us to decouple the masking behavior into the theme rather than code it into the application. However, as stated above this won’t work with tools such as `URLImage`.

To apply a theme mask we define a theme constant image representing the mask and then invoke `Label.setMaskName(String)` with the name of the theme constant. We can also set the mask object directly into the label which can be very beneficial.

The [SocialBoo](https://www.codenameone.com/blog/social-boo-revisited.html) demo uses this approach to round up the images of the avatars using rounded borders…​

### Performance Tradeoffs

Masking is expensive, it works by literally reviewing every pixel in the mask against every pixel in the image to apply alpha. Applying a mask during rendering is prohibitively expensive.

`URLImage` solves this by applying the mask as the image data is downloaded. It saves an image object with the mask already applied thus removes the cost of masking from future usage. It does make a tradeoff of encoding and saving the image which is more expensive than just saving the image but that benefits future executions. Other mask types don’t have that level of seamless usage. E.g. when we use the mask builtin to label we might run into a performance overhead if we call `setIcon` on more than one occasion.

We might also trigger a performance issue when invoking `setIcon` in a performance critical section of the code. E.g. if we optimize performance by lazily loading images while scrolling this masking might produce an overhead. It’s important to be vigilant when you apply masking to elements so you don’t do it more than once.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Clipping Creations India** — January 11, 2018 at 11:18 am ([permalink](https://www.codenameone.com/blog/tip-understand-image-masking-performance.html#comment-23860))

> Clipping Creations India says:
>
> Hi Shai Almog, you have mentioned important tips about image masking. Great job.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-understand-image-masking-performance.html)


### **AR Khan** — February 9, 2018 at 8:12 am ([permalink](https://www.codenameone.com/blog/tip-understand-image-masking-performance.html#comment-23541))

> AR Khan says:
>
> This is a very good and more necessary idea that you have explained here. thank you for this best resources
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-understand-image-masking-performance.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
