---
title: Icon Fonts & Old VM Swan Song
slug: icon-fonts-oldvm-swan-song
url: /blog/icon-fonts-oldvm-swan-song/
original_url: https://www.codenameone.com/blog/icon-fonts-oldvm-swan-song.html
aliases:
- /blog/icon-fonts-oldvm-swan-song.html
date: '2015-06-23'
author: Shai Almog
---

![Header Image](/blog/icon-fonts-oldvm-swan-song/font-awesome.jpg)

While multi-image’s go a long way in making your app scalable to various devices, scalable images can be even  
more convenient. The SVG standard never took off though, its too complex to support in an efficient way on  
the devices and so its relegated to web applications and even those don’t make heavy use of it due to its complexity.  
However, icon fonts have gained a lot of popularity in recent years and we used them in the past in Codename One  
e.g. in the [photo demo](http://udemy.com/build-mobile-ios-apps-in-java-using-codename-one/). 

That usage was awkward, we had to define a special font for the tab area and use letters in a way that didn’t communicate  
what would actually appear on the screen. It worked because we only used an icon without text, had we tried to use  
both it wouldn’t have worked…  
So we now have support for icon fonts via the new `FontImage` class. This class encapsulates a character  
or string from the icon font and presents it as an image. You can even scale and rotate such as image and you can convert  
it to an `EncodedImage` or regular `Image` if necessary. 

This allows very smooth graphics that adapt based on the platform e.g. here we use the fontello font to show a rotating  
progress wheel, thanks to the smoothness of the font the progress looks far more refined: 
    
    
    InfiniteProgress ip = new InfiniteProgress();
    int size = Display.getInstance().convertToPixels(20, true);
    Font fnt = Font.createTrueTypeFont("fontello", "fontello.ttf");
    FontImage fm = FontImage.createFixed("ue800", fnt, 0xffffff, size, size);
    ip.setAnimation(fm);
    ip.showInifiniteBlocking();

We can create such an image using two methods, a fixed size/color image which you can use to assign a pixel  
size for such an image and a more “generic” method that will accept a style and adapt the icon for that style. 

As we move forward we would like to integrate icon fonts far deeper into the Codename One stack, this would  
include integration with the designer tools etc. 

#### Retiring The Old VM

Apple is pretty quick with with moving forward, they pushed everyone to 64 bit very fast and it seems they are  
poised to do the same with some new iOS 9 changes. This usually takes the form of requiring an xcode upgrade  
which is pretty easy for us to do since we just need to login to all the servers and update xcode on those machines. 

This does pose a problem though, the newer versions of xcode require a newer version of Mac OS (Yosemite or even newer),  
unfortunately the older versions of xcode that still run the old VM don’t work on Yosemite anymore and aren’t  
maintained by Apple. We will try to wait until the last minute to upgrade in order to allow those of you who  
are still building with the old VM (e.g. in the enterprise setting) some time to migrate. However, you must migrate…  
Even versioned builds won’t work with the old VM once the OS is updated so you can no longer rely on the old  
XMLVM builds to still work in the future! 

Unfortunately some of our enterprise users just don’t read the blog, social media posts or our repeated mailings on the subject…  
Which is why we will be retiring the `ios.newVM` build hint so those guys who just added it and forgot  
about it will be **forced** to re-evaluate its necessity. 

We will expose a new flag: `ios.deprecatedDontUseThisFlagSeriouslyOldVM` which you can set to true  
to force the old vm. But seriously…  
**Don’t use it**.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
