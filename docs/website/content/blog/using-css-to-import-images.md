---
title: 'CSS Tip: Using CSS to Import Images'
slug: using-css-to-import-images
url: /blog/using-css-to-import-images/
original_url: https://www.codenameone.com/blog/using-css-to-import-images.html
aliases:
- /blog/using-css-to-import-images.html
date: '2016-11-14'
author: Steve Hannah
---

![Header Image](/blog/using-css-to-import-images/css-header.jpg)

The Codename One CSS plugin is a handy tool to style your app with CSS. This includes setting borders, padding, margin, fonts, colors, and pretty much everything else that you might otherwise do in the resource editor. However, there are some other cool things that this plugin can do for you, such as importing multi-images into your resource file. Let me demonstrate with a small snippet.

__ |  In order to use CSS with your Codename One project, you will need to install [CSS support](https://github.com/shannah/cn1-css) in your project first.   
---|---  
  
I’ll use the [MemeMaker demo](https://github.com/shannah/mememaker) as an example of how to do this. It has a CSS file located at “css/theme.css” within the project directory, and I’ve loaded this CSS file in my app’s init() method using this code:
    
    
    try {
        css = Resources.openLayered("/theme.css");
        UIManager.getInstance().addThemeProps(css.getTheme(css.getThemeResourceNames()[0]));
    } catch (Exception ex) {
        Log.e(ex);
    }

I created a little icon, to display in the title area. It is located at “css/MemeIcon-white-100×100.png”.

![The MemeMaker icon](/blog/using-css-to-import-images/MemeIcon-white-100x100.png)

I’d like it to be imported as a multi-image so that it will be rendered at an appropriate size on all devices, regardless of their pixel density.

I can import the image in my CSS file by adding a dummy selector with a `background-image`. I call this selector “Images”, but you can call it anything you like:
    
    
    Images {
        background-image: url(MemeIcon-white-100x100.png);
        cn1-source-dpi: 320;
    }

__ |  This url path is relative to the css file itself. If I had placed the Image in a subdirectory (e.g. css/images/), then the url would be `url(images/MemeIcon-white-100x100.png)`. I could also use an http or https URL if I wanted to load the image over the network. In such a case, this image would be downloaded at compile-time and embedded directly into the theme.css.res file.   
---|---  
  
The above CSS snippet should be mostly self-explanatory. It is creating a selector with my image as a background image. This triggers the CSS processor to import the image into the generated resource file at compile time.

The `cn1-source-dpi` directive is a hint to the CSS plugin as to what the “source dpi” of the image is. It uses this as a reference point for generating the different sizes in the resulting multi-image. Generally I only use one of 2 values here: `160` or `320`. As a reference, a value of 160 means that the image is currently sized correctly for an iPhone 3G (i.e. Non-retina display). A value of 320 means that the image is currently sized correctly for an iPhone 4, 5, or 6 (i.e. a Retina) device. It will then generate appropriately sized versions for all other densities based on this reference point.

In my experience, most PSD theme designs I find on the internet are sized for a retina display, so you’ll usually be using a value of “320” here. If you get it wrong, it will be obvious, and you can experiment with different values easily by just changing it and recompiling.

__ |  When changing the cn1-source-dpi, you may need to delete the old generated “theme.css.res” file (which is saved in your app’s “src” directory) to clear out the old resolutions.   
---|---  
  
### Accessing the Images from Code

Using the imported image from code is very easy. If you have a reference to the `Resources` object from loading the “theme.css.res” file, you can simply call `css.getImage("MemeIcon-white-100x100.png")` (i.e. you can load it by its file name).

### Loading Multiple Images in a Single Selector

In the example above, I only imported a single image, but you can import multiple images in the same selector by separating them by a comma within the same `background-image` property. E.g.
    
    
    Images {
        background-image: url(MemeIcon-white-100x100.png),
            url(http://example.com/otherimage.jpg),
            url(images/myotherimage.png);
        cn1-source-dpi: 320;
    }

### Adding Images to UI Constants

Sometimes I find it tedious to keep a reference to my css `Resources` object for the purpose of loading an image. One nice alternative is to add the image to Codename One’s theme constants. The CSS plugin reserves a special selector `#Constants` for defining theme constants (these correspond with the values you see in the “Constants” tab of a theme in the resource editor). Here is an example from the [MemeMaker app](https://github.com/shannah/mememaker):

***theme.css** :
    
    
    Images {
        background-image: url(MemeIcon-white-100x100.png);
        cn1-source-dpi: 320;
    }
    
    #Constants {
        MemeIconImage: "MemeIcon-white-100x100.png";
    }

This creates a theme constant named “MemeIconImage” with my image. I can then access the image from code as follows:
    
    
    UIManager.getInstance().getThemeImageConstant("MemeIconImage")

__ |  Theme constants must follow naming conventions to identify their “type”. Any theme constant that stores an image, must end with “Image”. E.g. “MemeIconImage” is fine. “MemeIconPhoto” is **not**. If you take a look at the constants tab of any theme, you should be able to catch on to the naming convention very quickly.   
---|---  
  
And the result:

![Mememaker Title Area](/blog/using-css-to-import-images/Mememaker-titlearea.png)

### Get the Meme Maker App

  1. [On the Play Store](https://play.google.com/store/apps/details?id=com.codename1.demos.mememaker)

  2. [In the Windows Store](https://www.microsoft.com/en-us/store/p/codename-one-meme-maker/9nblggh441nf)

  3. [In the iTunes Store](https://itunes.apple.com/us/app/codename-one-meme-maker/id1171538632)

  4. [On GitHub](https://github.com/shannah/mememaker)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — August 9, 2017 at 3:44 pm ([permalink](https://www.codenameone.com/blog/using-css-to-import-images.html#comment-23664))

> Is it possible to import several images with different cn1-source-dpi?
>
> In the following example you assume that both images have same dpi, is it right? How to do if the dpi is different?
>
> Images {  
> background-image: url(MemeIcon-white-100×100.png),  
> url([http://example.com/otherima…](<http://example.com/otherimage.jpg>)),  
> url(images/myotherimage.png);  
> cn1-source-dpi: 320;  
> }
>
> Thank you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-css-to-import-images.html)


### **shannah78** — August 10, 2017 at 5:00 am ([permalink](https://www.codenameone.com/blog/using-css-to-import-images.html#comment-23254))

> You would do separate styles.
>
> E.g.
>
> Images {  
> ….  
> cn1-source-dpi: 320  
> }  
> Images160 {  
> …  
> cn1-source-dpi: 160  
> }
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-css-to-import-images.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
