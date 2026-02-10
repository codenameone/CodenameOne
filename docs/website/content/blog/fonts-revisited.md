---
title: Fonts Revisited
slug: fonts-revisited
url: /blog/fonts-revisited/
original_url: https://www.codenameone.com/blog/fonts-revisited.html
aliases:
- /blog/fonts-revisited.html
date: '2012-11-26'
author: Shai Almog
---

![Header Image](/blog/fonts-revisited/fonts-revisited-1.png)

  
  
  
[  
![Picture](/blog/fonts-revisited/fonts-revisited-1.png)  
](/img/blog/old_posts/fonts-revisited-large-3.png)

Fonts are were a painful subject in Codename One, historically devices supported a very limited set of fonts and we were bound by said limitations. However, devices moved forward and finally we too can move forward to more reasonable font support. 

The new font API is limited to Android & iOS, we were considering Blackberry support too but it seems that the font support on Blackberry is too limited for our needs (feel free to correct me if I’m wrong here), on the other platforms a standard system font will be used where fonts aren’t supported.

In order to use a font just add the ttf file into the src directory, notice that the file must have the “.ttf” extension otherwise the build server won’t be able to recognize the file as a font and set it up accordingly (devices need fonts to be defined in very specific ways). Once you do that you can use the font from code or from the theme.

In the theme section of the Codename One designer you now have the option to define the font like this: 

* * *

  
  
  
[  
![Picture](/blog/fonts-revisited/fonts-revisited-2.png)  
](/img/blog/old_posts/fonts-revisited-large-4.png)

The system font will be used where True Type fonts aren’t supported, the size of the font can be one of 5 options.  
  
Small, medium & large correspond to the 3 system font sizes and occupy the exact same sizes of the system fonts.  
  
Millimeters & pixels will size the fonts appropriately using the numeric field. 

To use fonts from code just use:  
  
if(Font.isTrueTypeFileSupported()) {  
  
Font myFont = Font.createTrueTypeFont(fontName, fontFileName);  
  
myFont = myFont.derive(sizeInPixels, Font.STYLE_PLAIN);  
  
// do something with the font  
  
}  
  
Notice that in code only pixel sizes are supported so its up to you to decide how to convert that. You also need to derive the font with the proper size unless you want a 0 sized font which probably isn’t very useful.  
  
The font name is the difficult bit, iOS requires the name of the font which doesn’t always correlate to the file name in order to load the font, its sometimes viewable within a font viewer but isn’t always intuitive so be sure to test that on the device to make sure you got it right. 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
