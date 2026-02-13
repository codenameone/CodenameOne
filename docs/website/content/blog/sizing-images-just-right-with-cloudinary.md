---
title: Sizing Images Just Right with Cloudinary
slug: sizing-images-just-right-with-cloudinary
url: /blog/sizing-images-just-right-with-cloudinary/
original_url: https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html
aliases:
- /blog/sizing-images-just-right-with-cloudinary.html
date: '2015-11-24'
author: Steve Hannah
---

![Header Image](/blog/sizing-images-just-right-with-cloudinary/cloud-based-image-management.jpg)

As I discussed in a [previous article](https://www.codenameone.com/blog/i-am-your-density.html), creating a beautiful user interface that behaves appropriately across multiple devices can be a challenge, what with the vast array of screen sizes that your app may be deployed to. Codename One provides you with some power tools on this front. E.g.:

  1. Multi-images

  2. Pixel to Millimetre unit conversion (approximate)

  3. On-device image scaling

In this article, I would like to share with you another powerful tool to add to your kit, the [Cloudinary library](https://github.com/shannah/cloudinary-codenameone).

## The Motivation

Let’s take a look at a screen mock-up from a a [nice UI kit](http://www.invisionapp.com/now) that I’ve been working with lately:

![4a91f952 9295 11e5 953b 850dc84c0fb0](/blog/sizing-images-just-right-with-cloudinary/4a91f952-9295-11e5-953b-850dc84c0fb0.png)

Replicating this UI in Codename One is pretty straight forward:

  1. Use the [ToolBar](http://www.codenameone.com/blog/toolbar.html) for the top bar, and a BoxLayout Y_AXIS for the contents.

  2. Use [multi-images](http://www.codenameone.com/how-do-i---fetch-an-image-from-the-resource-file---add-a-multiimage.html) for the icons so that they come out to the correct “real” size on every device.

The main photo is a bit more challenging though. The design calls for us to span the entire width of the device. If we just use a multi-image we can get it to size approximately to this space. But not exactly. We could set a multi-image as a background image for a container, and specify that it “scale to fill” the space. In fact this solution will work perfectly, if we are able to store the image in our resource file.

Unfortunately, it looks like this app is some sort of news app, where articles are loaded from a server, and each article comes with its own photo. Hence, there could be thousands or millions of photos that need to be sized this way in this app. Using the “multi-image” solution won’t scale well, since multi-images need to be stored inside the resource file, and you can’t realistically store thousands or millions of images in the resource file.

Loading images over the network isn’t a problem. You can just use the URLImage class with a “scale to fill” filter. The PropertyCross demo actually uses a hybrid solution with URLImage to load images of housing properties, multi-images serving as the placeholder image so that the image will scale to the right size. This approach, while workable still has fairly major problem:

How do we resize the image to be the correct size on all devices, **and** maintain quality. E.g. On the iPhone 3G, we need an image to be 320 px wide to span the width of the screen, whereas on the iPhone 6+, the image needs to be 1080 px. If we download a 320px image from the network, and resize it on device, it will look great on the 3G but horrible on the 6+. If we instead download a 1080px image, it will look great on the 6+ but quite poor on the 3G. In addition, this will waste a lot of unnecessary bandwidth for 3G users, downloading hi-res images that they don’t need.

We could try to split the difference by downloading say 720px image, but this just leads to sub-par results on both devices. The ideal solution would be to download a 320px image on the 3G and a 1080px image on the 6+. That way, quality is optimal on both devices, and we don’t waste any bandwidth.

One possible solution is to produce multiple versions of the image on your server, and have the device automatically select the correct one. This wouldn’t be too difficult to do, but before you go off and create a server-side script for resizing your images, I recommend you check out [Cloudinary](http://cloudinary.com/). Because they already do this. And they do it very well.

## The Solution

Before you begin, you’ll need to sign up for a free [cloudinary account](http://cloudinary.com/). The free account gives you up to 75,000 image transformations per month, and up to 5 gigs of bandwidth.

Then, you’ll need to add the [cloudinary cn1lib](https://github.com/shannah/cloudinary-codenameone#installation) to your project.

When you sign up for cloudinary, you’ll receive an API key, and associated credentials for accessing their API. You just need to initialize the API in your app’s init() method, as follows:
    
    
    import com.cloudinary.Cloudinary;
    
    ...
    Cloudinary cloudinary;
    
    ...
    
    public void init() {
    
        ...
    
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
          "cloud_name", "my_cloud_name",
          "api_key", "my_api_key",
          "api_secret", "my_api_secret"));
    
    
        // Disable private CDN URLs as this doesn't seem to work with free accounts
        cloudinary.config.privateCdn = false;
    
        ...
    }

### Loading and Displaying an Image

Once Cloudinary is initialized you can use the API to fetch images, upload images, and much more. The following example loads an image from the internet, and resizes it to a square image that spans the width of the device screen:
    
    
            Form f = new Form("Test");
            f.getAllStyles().setPadding(0,0,0,0);
            f.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
    
            int deviceWidth = Display.getInstance().getDisplayWidth();
    
            // We create a placeholder image to set the size for the image to fit
            // to.
            Image placeholder = Image.createImage(deviceWidth, deviceWidth);
            EncodedImage encImage = EncodedImage.createFromImage(placeholder, false);
    
            // Load an image from cloudinary
            Image img2 = cloudinary.url()
                    .type("fetch")  // Says we are fetching an image
                    .format("jpg")  // We want it to be a jpg
                    .transformation(
                        new Transformation().crop("fill")  //  We crop the image to fill the given width/height
                            .width(deviceWidth)
                            .height(deviceWidth)
                    )
                    .image(encImage, "http://upload.wikimedia.org/wikipedia/commons/4/46/Jennifer_Lawrence_at_the_83rd_Academy_Awards.jpg");
    
            // Add the image to a label and place it on the form.
            Label l = new Label(img2);
    
            // Get rid of margin and padding so that the image spans the full width of the device.
            l.getAllStyles().setMargin(0, 0, 0, 0);
            l.getAllStyles().setPadding(0, 0, 0, 0);
            f.addComponent(l);
            f.show();

The result is:

![251ad556 929d 11e5 8f07 d4d2e2b9ec0e](/blog/sizing-images-just-right-with-cloudinary/251ad556-929d-11e5-8f07-d4d2e2b9ec0e.png)

You’ll notice that if you try to load that image directly from the [URL](http://upload.wikimedia.org/wikipedia/commons/4/46/Jennifer_Lawrence_at_the_83rd_Academy_Awards.jpg), it is massive! The Cloudinary service automatically resizes the image before sending it to the device in the requested size. Performing the resize on the server produces stellar results. Much better than you could achieve with on-device resizing. In addition, you save a lots of bandwidth for your users, which also improves app performance. It’s a win-win all ’round.

## But That’s Not All

This is the most basic example, I could come up with, but Cloudinary can do much more. See their [features page](http://cloudinary.com/features) for a full list of capabilities. Here is a few nice features that caught my eye:

  1. Resizing & Cropping

  2. Face detection, and appropriate cropping

  3. Watermark overlays

  4. Circular thumbnails, rounded corners

  5. Text overlays, shadows, and borders

  6. Photo effects, like Sepia, Blur, Black & White, etc..

  7. PDF and Office document processing. E.g. you can extract pages from PDFs or Word documents as images.

  8. Convert image format / modify quality.

## Learn More

The cn1lib is a fairly straight port of Cloudinary’s official Java API, and should support most of its features (including uploading). Check out the [README](https://github.com/shannah/cloudinary-codenameone) for more information.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — November 25, 2015 at 5:40 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-22398))

> Hi Steve,
>
> I tried out Cloudinary and it works really great.
>
> There is challenge though, if my MultiButtons or List reference the same image, cloudinary sent multiple requests until 1 is cached. And according to this article [http://support.cloudinary.c…](<http://support.cloudinary.com/hc/en-us/articles/206077291-Why-did-my-Monthly-Transformation-quota-inflate-so-quickly->), it in turns increment the Transformation Quota.
>
> Is there a way to detect if an image URL is already in queue and instead of sending another request, it waits for that to finish and use it?
>
> As a note to anyone using this library: Cloudinary changes spaces in filenames to underscores automatically.  
> To avoid 404 error, Do “.image(encImage, StringUtil.replaceAll(yourImageName, ” “, “_”));”.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **shannah78** — November 26, 2015 at 12:06 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-22405))

> Thanks for the tip on image names and underscores.
>
> The issue with loading the same URL might be something we want to do at a lower level — like NetworkRequest. I ran into this same issue when doing the Flickr concentration demo for a webinar a couple months ago. I worked around it by keeping a map of the URLs that I loaded, and if a URL request was pending, it just returned the URLImage for the previous image. Here is the code I used:
>
> [https://github.com/shannah/…](<https://github.com/shannah/cn1-flickr-concentration-demo/blob/master/src/com/codename1/demos/flickrconcentration/ClassicFlickrConcentration.java#L54-L61>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **bryan** — November 26, 2015 at 6:03 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-22497))

> bryan says:
>
> Slightly off topic, but I looked at the link to the UI toolkit you mentioned, and I’m wondering what does it do for me as a CN1 developer ?
>
> BTW – like your posts, very helpful and informative.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **shannah78** — November 26, 2015 at 4:45 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-22439))

> shannah78 says:
>
> UI toolkits like that are very helpful for making nice UIs in Codename One. I’m currently working on porting that particular one to a theme and UI library for codename one. The process I use is described here: [http://www.codenameone.com/…](<http://www.codenameone.com/blog/psd-to-app-converting-a-beautiful-design-into-a-native-mobile-app.html>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Carl-Erik Kopseng** — August 30, 2016 at 8:10 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23005))

> Carl-Erik Kopseng says:
>
> Any tips on generating image sitemaps for cloudinary? Since there could be any number of versions of the same image, I was wondering if we should list them all or just one canonical representation.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Shai Almog** — August 31, 2016 at 4:31 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-22713))

> Shai Almog says:
>
> Is this related to Codename One?  
> I don’t think this is the best venue for cloudinary tips.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Gert** — March 24, 2017 at 3:12 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23153))

> Gert says:
>
> I’ve never seen such a wonder example..  
> However, you’d like to change  
> Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(…  
> to  
> cloudinary = new Cloudinary(ObjectUtils.asMap(…
>
> Well..a question. Can’t I use local image to transform?
>
> Thanks.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Shai Almog** — March 25, 2017 at 5:48 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23047))

> Shai Almog says:
>
> You can transform local images with many of the API’s we have such as masking etc. for that you don’t need cloudinary though.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Gert** — March 25, 2017 at 5:59 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23446))

> Gert says:
>
> Thanks, Shai.  
> I found the Image.fill(width, height) method at last. It is very useful one.  
> Another question,  
> First, is there any possibility to drive iOS and Android fingerprint authentication?  
> Second, I would like to make the Windows and Mac forms to be “alwaysOnTop” and “Non-focusable” ? It is possible in Java swing, but I couldn’t find the way in this codename1. if no way, any other way to use native libraries?  
> Best regards.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Shai Almog** — March 26, 2017 at 5:08 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23389))

> Shai Almog says:
>
> You will need native interfaces for that, I don’t think it will be hard but haven’t tried it myself.
>
> When you generate native interfaces it also generates one for Java SE which will execute in the desktop port and the simulator. You can implement “always on top” there. I would check the isDesktop method from Desktop to prevent that code from executing in the simulator.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Gert** — March 28, 2017 at 12:26 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23226))

> Gert says:
>
> Hi, Shai, I was very impressed to your rapid reply.  
> Just hope your business grow the best.
>
> Still I have question. I studied native interface, but I couldn’t find the way to make the Desktop form to “AlwaysOnTop”.  
> First of all, I couldn’t pass the Form value as parameter for native methods, even I don;t know this way is right for always on top.  
> Please guide me how to implement alwaysontop feature using native interface.  
> All the best.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **shannah78** — March 28, 2017 at 4:39 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23311))

> shannah78 says:
>
> You don’t need to pass the Form to the native interface. You can get the JFrame in native (swing) code using Frame.getFrames() or Window.getWindows. [http://stackoverflow.com/a/…](<http://stackoverflow.com/a/7364729/2935174>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Gert** — March 30, 2017 at 2:27 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23463))

> Gert says:
>
> Thanks so much, it works very well.  
> Just a question also.  
> I use cd1’s Button or JButton.  
> However I couldn’t get it in Native Interfaces, like Frame.getFrames()..  
> Even it seems it will manage only swing components, not cd1 component in JavaSE Native methods.  
> How can I manage it?
>
> I prefer I will be able to get cd1 components in JavaSE native interface’s methods.  
> Since we know we can’t pass button components as argument to native methods, i can’t know how to get it in native methods.
>
> Thanks. shannah78, and shai again.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Gert** — March 30, 2017 at 10:15 am ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23330))

> Gert says:
>
> Hi, Shai, First of all, thanks to everything of codename one.  
> Well, I would like to customize camera view, simply overlay PNG image in the full camera view screen.  
> Any way how to do it?  
> Best regards.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **shannah78** — March 30, 2017 at 6:06 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23271))

> shannah78 says:
>
> I don’t know what you’re trying to do. You can still interact with CN1 from the native side. Just make sure that you only interact with Swing on Swing’s EDT, and only interact with CN1’s UI on the CN1 EDT.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)


### **Gert** — April 1, 2017 at 2:24 pm ([permalink](https://www.codenameone.com/blog/sizing-images-just-right-with-cloudinary.html#comment-23010))

> Gert says:
>
> Yes, you are right. I was thinking wrongly. There was never needed to get any Buttons in native methods.  
> Thanks for your everything.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fsizing-images-just-right-with-cloudinary.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
