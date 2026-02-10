---
title: Cool Text Effects for Your Mobile App on iPhone (iOS), Android etc.
slug: cool-text-effects-mobile-app-phone-ios-android
url: /blog/cool-text-effects-mobile-app-phone-ios-android/
original_url: https://www.codenameone.com/blog/cool-text-effects-mobile-app-phone-ios-android.html
aliases:
- /blog/cool-text-effects-mobile-app-phone-ios-android.html
date: '2016-11-09'
author: Steve Hannah
---

![Header Image](/blog/cool-text-effects-mobile-app-phone-ios-android/cool-text-transformations.jpg)

FontBox is a mature java library for loading, manipulating, and rendering fonts in Java. It gives you direct access to the font glyphs so that you can perform effects or transformations on them. A couple of years ago, I ported FontBox to Codename One, but since CN1 didn’t yet include support for drawing shapes, I made it dependent upon the [CN1Pisces library](https://github.com/shannah/CN1Pisces), which did support drawing shapes. This was cool but it had some major limitations; the main one being that FontBox fonts could only be used to draw strings on Pisces graphics contexts, which can only be rendered to an image – not directly to the screen. This meant that you couldn’t **just** use a FontBox font in your app (e.g. in a label or a button). You could only use it to write on an image.

Fast forward to the present day, when Codename One supports [drawing shapes](https://www.codenameone.com/javadoc/com/codename1/ui/geom/GeneralPath.html) “natively” on all major platforms, I felt is was time to revamp this library so that it integrates better with Codename One. My goal was to be able to use a FontBox font interchangeably with “normal” fonts. E.g. for a label, button, text field, graphics context, etc…​ The motivation for this update came as I was developing the “Meme Maker” demo. I needed to draw some text that was filled white but had a black outline. Built-in fonts don’t have this ability, but since FontBox provides direct access to the font glyphs as paths (i.e. lines and curves) I could fairly easily create a font that had both fill and stroke. That’s just what I did, with the new `TTFFont` class.

### What can `TTFFont` do that `Font` cannot?

`TTFFont` is a subclass of `CustomFont` which is itself a subclass of `com.codename1.ui.Font`. Therefore it can be used in any place that a standard font is used. Why would you do this?

TTFFont supports stroking, filling, or both, with different colors. E.g. You can create a font that is stroked with white, but filled with black. And you can specify the width of the stroke just as you would a normal shape.

![Stroked and filled text](/blog/cool-text-effects-mobile-app-phone-ios-android/fontbox-text-normal.png)

Figure 1. Stroked and filled text

TTFFont supports both horizontal and vertical scaling, so you can make your text really skinny, or really wide, depending on what your requirements are. This also enables you to size text to fit a space **exactly**.

![Stretched text](/blog/cool-text-effects-mobile-app-phone-ios-android/fontbox-text-stretched.png)

Figure 2. Stretched text

![Compressed text](/blog/cool-text-effects-mobile-app-phone-ios-android/fontbox-text-compressed.png)

Figure 3. Compressed text

`TTFFont` can load a TTF file from storage, file system, resources, or just from a plain old InputStream. This means that you can load fonts dynamically over a network if you want.

### Drawbacks

Since `TTFFonts` are actually rendered in CN1 as shapes, they will be slower than built-in fonts, which are rendered natively by the platform. Therefore, you should only use TTFFont in cases where you require its extra capabilities. That said, there was no noticeable “lag” introduced in the [Meme Maker app](https://github.com/shannah/mememaker) by my use of TTFFont.

### Usage Examples

#### Loading TTF File

**From InputStream:**
    
    
    TTFFont font = TTFFont.createFont("MyFont", inputStream);

**From Resources:**
    
    
    TTFFont font = TTFFont.createFont("MyFont", "/MyFont.ttf");

**From Storage/URL:**
    
    
    TTFFont font = TTFFont.createFontToStorage("MyFont",
        "font_MyFont.ttf",
        "http://example.com/MyFont.ttf"
    );

**From File System/URL:**
    
    
    TTFFont font = TTFFont.createFontToFileSystem("MyFont",
        FileSystemStorage.getInstance().getAppHomePath()+"/fonts/MyFont.ttf",
        "http://example.com/MyFont.ttf"
    );

**From Cache:**
    
    
    TTFFont font = TTFFont.getFont("MyFont", 12);

#### Setting Font for Style
    
    
    myLabel.getAllStyles().setFont(font);

#### Getting Particular Size Font
    
    
    font = font.deriveFont(24); // get size 24 font.

#### Horizontal and Vertical Scaling
    
    
    font = font.deriveScaled(0.5f, 1.5f);
        // scaled 50% horizontal, and 150% vertical
    
    font = font.deriveHorizontalScaled(0.5f); // scaled 50% horizontally
    
    font = font.deriveVerticalScaled(0.5f); // scaled 50% vertically

#### Stroking and Filling
    
    
    font = font.deriveStroked(Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f), #ff0000);
        // Stroke with red 1px outline
    
    font = font.deriveStroked(Stroke(1f, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1f), null);
        // Stroked - stroke color determined by graphics context's current color.. e.g. defers to Style's foreground color
    
    font = font.deriveStroked(null, 0x0);
        // Not stroked
    
    font = font.deriveFilled(true, null);
        // Filled - fill color determined by graphics context's current color.. e.g. defers to Style's foreground color
    
    font = font.deriveFilled(true, 0x00ff00);
        // Filled with green
    
    font = font.deriveFilled(false, null);
        // Not filled

#### Antialias
    
    
    font = font.deriveAntialias(true);
        // Should be rendered antialiased
    
    font = font.deriveAntialias(false);
        // should be rendered without antialias.

#### Drawing Directly on Graphics Context
    
    
    font.drawString(g, "Hello world", x, y);
    
    // Or ...
    g.setFont(font);
    g.drawString("Hello world", x, y);

#### Appending to existing GeneralPath
    
    
    font.draw(path, "Hello world", x, y, 1f /*opacity*/);

### More About CN1FontBox

For more information about CN1FontBox, check out the [CN1FontBox github repository](https://github.com/shannah/CN1FontBox). The best way to install it is through the “Extensions” section of the Codename One Settings for your project.

### Get the Meme Maker App

  1. [On the Play Store](https://play.google.com/store/apps/details?id=com.codename1.demos.mememaker)

  2. [In the Windows Store](https://www.microsoft.com/en-us/store/p/codename-one-meme-maker/9nblggh441nf)

  3. [In the iTunes Store](https://itunes.apple.com/us/app/codename-one-meme-maker/id1171538632)

  4. [On GitHub](https://github.com/shannah/mememaker)
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — November 10, 2016 at 7:41 pm ([permalink](https://www.codenameone.com/blog/cool-text-effects-mobile-app-phone-ios-android.html#comment-22931))

> That’s very cool. Thanks once again for all your great work on CN1.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcool-text-effects-mobile-app-phone-ios-android.html)


### **Chibuike Mba** — November 11, 2016 at 9:09 am ([permalink](https://www.codenameone.com/blog/cool-text-effects-mobile-app-phone-ios-android.html#comment-21507))

> Nice one Steve. Thanks.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcool-text-effects-mobile-app-phone-ios-android.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
