---
title: Rounded Corners, Gradients, and Shadows with CSS
slug: rounded-corners-shadows-and-gradients-with-css
url: /blog/rounded-corners-shadows-and-gradients-with-css/
original_url: https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html
aliases:
- /blog/rounded-corners-shadows-and-gradients-with-css.html
date: '2015-10-18'
author: Steve Hannah
---

![Header Image](/blog/rounded-corners-shadows-and-gradients-with-css/css-header.jpg)

Codename One provides you with the tools to craft a visually stunning and unique user interface via its pluggable look and feel. For simple use cases, you can define styles entirely by setting style properties either in code or the resource editor. However, to achieve best results (for anything more complicated than a solid background color with a rectangular border), you will often want to use image borders and backgrounds. The usual process for styling elements in Codename One is:

  1. Find or create an image that encapsulates the background and border of the element.

  2. Import the image into your theme as a multi-image.

  3. Slice the image into a 9-piece border if you want the element to be able to adapt to different sizes.

This process is described in great detail [in this blog post](http://www.codenameone.com/blog/psd-to-app-converting-a-beautiful-design-into-a-native-mobile-app.html).

This process is extremely flexible, since you can use it to convert pretty much any design into an app. However, for many common use cases such as rounded borders, I would prefer to be able to automatically generate these image borders and backgrounds without having to create an image, import it, and slice it.

The new Codename One CSS library allows you to do just that. The library enables you to create entire themes using only CSS, but in this post, I’m going to focus on its ability to automatically generate image backgrounds and borders for your apps.

## Rounded Corners

The following a CSS definition for a minimal rounded button.
    
    
    Round1 {
        width: 40pt;
        height: 40pt;
        border-radius: 20pt;
        border: 1pt solid black;
        text-align: center;
    }

The result will be a button that looks like this:

![bf41a490 7329 11e5 9fdf 973404d20f65](/blog/rounded-corners-shadows-and-gradients-with-css/bf41a490-7329-11e5-9fdf-973404d20f65.png)

How it works:

At compile-time, the CSS library compiles the CSS file into a codename one resource file. Each element selector (in the example above this is “Round1”) is converted into a UIID/Style that can be referenced from your codename one application. If the CSS styles specified can be expressed completely by Codename One style properties (e.g. padding, margin, font, simple borders, etc…​), then the resulting UIID will be more or less a direct conversion of the CSS properties. If, however, the CSS styles mandate a background or border that Codename One cannot express using its regular styles (e.g. rounded corners, shadows, gradients), then an appropriate image border or background will be generated and saved in the resource file as a multi-image.

In the above example, we specify that the “Round1” UIID should include rounded corners with a radius of 20pt. Since Codename One doesn’t support rounded corners natively, the CSS module will (at compile time) generate a an image with the appropriate rounded border and use this in an image border for the “Round1” UIID.

## Inner Shadows

Let’s spice up our button a bit more by adding shadows into the mix. Add this CSS snippet:
    
    
    Round1InnerShadow {
        cn1-derive: Round1;
        box-shadow: inset 0px 0px 21pt 10pt rgba(61,59,61,0.48);
        border: 1pt solid transparent;
    }

This defines a new UIID named “Round1InnerShadow” that derives from the “Round1” UIID (i.e. inherits all of its properties), but changes to a transparent border, and adds an inner shadow.

And the result is

![f2317810 732b 11e5 82b4 e6ea31fc1e6b](/blog/rounded-corners-shadows-and-gradients-with-css/f2317810-732b-11e5-82b4-e6ea31fc1e6b.png)

## Outer Shadows

Let’s shift gears a bit and make a button that is a little less round, but has a subtle drop-shadow.
    
    
    Round1OuterShadow {
        cn1-derive: Round1;
        box-shadow: 5pt 5pt 10pt 0px rgba(61,59,61,0.48);
        border-radius: 5pt;
        border: 1pt solid #ccc;
        padding-bottom: 2mm;
        padding-top: 1mm;
    }

In this case we reduce the border radius to 5 dips, and we have removed the `inset` marker from the `box-shadow` property so that the shadow will be outside the button. After some experimentation I also found that a little bit of padding helps for the look of the button in this case.

The result:

![e5185556 7333 11e5 9706 c44eb9eea001](/blog/rounded-corners-shadows-and-gradients-with-css/e5185556-7333-11e5-9706-c44eb9eea001.png)

## Gradients

I could go on and on with different cool CSS effects that you can generate, but I’ll stop at this one last one: Gradients.

Codename One has been capable of generating gradient backgrounds at runtime for quite some time, but we are told not to use them because they are “slow”. Instead we are instructed to create an image with the gradient that we want, and then use it as a background image.

Using CSS and the `linear-gradient` or `radial-gradient` property, you can have your cake and eat it too since it will generate the gradients as images at compile-time, then use them as image backgrounds (or image borders) in your theme.
    
    
    Round1Gradient {
        cn1-derive: Round1OuterShadow;
        background: linear-gradient(to bottom,
            rgba(242,246,248,1) 0%,
            rgba(216,225,231,1) 50%,
            rgba(181,198,208,1) 51%,
            rgba(224,239,249,1) 100%
        );
    }

The result:

![d5131086 7335 11e5 8fc5 c8183f1b1ecb](/blog/rounded-corners-shadows-and-gradients-with-css/d5131086-7335-11e5-8fc5-c8183f1b1ecb.png)

## Generating the CSS

For those of you who don’t speak CSS fluently yet, you will be happy to learn that the ‘net is filled with online tools for generating CSS borders, shadows, and gradients for you. One such tool is this [CSSMatic tool](http://www.cssmatic.com/border-radius). You just enter the values that you want for color, border-radius, etc.., and it spits out some shiny CSS for you to just paste into your stylesheet.

**However** You will probably want to modify the CSS that it generates just a little bit:

  1. Change all `px` units to `pt`. This will cause the coordinate to be scaled appropriate for the device density.

  2. Only include the standard CSS properties, not the browser-specific ones. Browser specific property names will be prefixed by one of `-moz-`, `-webkit`, or some other prefix beginning with “-“. E.g. The tool output the following CSS for a rounded border:
         
         border-radius: 10px 10px 10px 10px;
         -moz-border-radius: 10px 10px 10px 10px;
         -webkit-border-radius: 10px 10px 10px 10px;
         border: 0px solid #000000;

We would change this to
         
         border-radius: 10pt 10pt 10pt 10pt;
         border: 0px solid #000000;

## Where Do I Put My CSS File?

“This CSS looks really nice, but where to I put it, and how to I set up the CSS module?”, you say. I’m glad you asked. The CSS compiler will look for .css files inside your project’s “css” directory (which you’ll need to create). It will compiles these CSS files into corresponding .res files which will be placed into your project’s src directory. You can then load this theme in your app just as you would load any other theme file.

![Codename One CSS flow chart](/blog/rounded-corners-shadows-and-gradients-with-css/bf44abe4-73f7-11e5-8ecb-976478e297da.png)

E.g.

Suppose you add a CSS file into your project css/theme.css. When you compile your project, it will generate the file src/theme.css.res. Then you can load this file in your app as follows:
    
    
    Resources css = Resources.openLayered("/theme.css");
    UIManager.getInstance().addThemeProps(css.getTheme(css.getThemeResourceNames()[0]));

You can download the Codename One CSS library and read the installation instructions in the [CN1-CSS Repository](https://github.com/shannah/cn1-css) on GitHub.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — October 19, 2015 at 3:54 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22454))

> Looks like really cool stuff; hope to try this out some time. Thanks!!!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Franck Marchand** — February 13, 2016 at 10:51 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22222))

> Hi, is it possible to do a cercle ? I try with border-radius:50% but I have a border around image, not a circle.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Shai Almog** — February 14, 2016 at 3:36 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22371))

> Hi,  
> that won’t work. You round is inherently a special case since it can’t grow very well.
>
> The closest you can get is use something a MultiImage of a circle as a background image and marking it as SCALE_TO_FIT.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Franck Marchand** — February 14, 2016 at 10:21 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22425))

> Have you an example ?
>
> The closest I do is not really fun :).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Franck Marchand** — February 14, 2016 at 7:41 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22617))

> Ok, I find: [http://www.javacodegeeks.co…](<http://www.javacodegeeks.com/2015/08/building-a-chat-app-with-codename-one-part-4.html>)
>
> And I am inspired me :
>
> EncodedImage img = EncodedImage.createFromImage(_parent.getTheme().getImage(“teams_female.png”), false);
>
> Image mask = _parent.getTheme().getImage(“teams_rounded-mask.png”);
>
> EncodedImage roundPlaceholder = EncodedImage.createFromImage(img.scaled(mask.getWidth(), mask.getHeight()).applyMask(mask.createMask()), false);
>
> Label photo = new Label(roundPlaceholder);  
> photo.setUIID(“UserImage”);
>
> Regards
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Shai Almog** — February 15, 2016 at 3:28 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22481))

> Shai Almog says:
>
> I was thinking more about this: [https://www.codenameone.com…](<https://www.codenameone.com/manual/theme-basics.html#_the_background_behavior_image>)
>
> Notice that the article you linked appeared here first: [http://www.codenameone.com/…](<http://www.codenameone.com/blog/building-a-chat-app-with-codename-one-part-4.html>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Mo** — June 2, 2016 at 4:09 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22840))

> Mo says:
>
> Hi Steve, love the CSS lib, thank you, so far so good, just ran into a few issues when it comes to controlling the position and styling for the Container (Center) when setting direction ( RTL/LTR) with SpanLabel and SpanButton components(Label and Text), which do not inherits the UIID in addition to the Round1OuterShadow above, where I failed to Center, keeping inmind that, my user can switch from Arabic(RTL) to English, do I have to maintain to 2 CSS files??, any thought to the above would be much appreciated.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Shai Almog** — June 3, 2016 at 4:05 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22730))

> Shai Almog says:
>
> CSS isn’t really related to LTR. Things will flip automatically when you turn on RTL
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Akinniranye James** — September 9, 2016 at 8:55 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22881))

> Akinniranye James says:
>
> Css to res ,along with the the New GUI Builder (i could substitute this [https://github.com/shannah/…](<https://github.com/shannah/CN1ML-NetbeansModule>)), gave me the superpower to create my best looking cross mobile app with codenameone.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — September 11, 2016 at 11:32 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22752))

> Hristo Vrigazov says:
>
> There are instructions for Netbeans and Eclipse. Is IntelliJ supported? I tried the steps for Netbeans but it did not work
>
> EDIT: It actually works in InteliJ following Netbeans instructions. But I think that we should load the theme by:
>
> UIManager.getInstance().addThemeProps(css.getTheme(css.getThemeResourceNames()[0]));
>
> instead of:
>
> UIManager.getInstance().addThemeProps(css.getThemeResourceNames()[0]);
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **shannah78** — September 12, 2016 at 3:34 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-23033))

> shannah78 says:
>
> Thanks. I’ve fixed the post… it will be updated on site shortly.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — October 23, 2016 at 6:25 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-23135))

> Hristo Vrigazov says:
>
> I notice that sometimes, in very rare cases, the rounded UIID appears broken in a dialog, it is like the part above it appears under it. I cannot a find a case that does this reliably, but it is somewhat annoying. Any idea what could have caused the problem?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Shai Almog** — October 24, 2016 at 2:17 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22687))

> Shai Almog says:
>
> Is this specific to CSS?  
> What’s the generated style you have in the resource file?  
> Can you provide screenshots?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — October 27, 2016 at 1:22 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-21448))

> Hristo Vrigazov says:
>
> I finally managed to get it on
>
> [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/642215d9f98754c90ceeba25e3a2a86b96465438652a7de0551bf847662f89e2.png>) the simulator.
>
> I have seen this only on CSS and not always.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Shai Almog** — October 28, 2016 at 3:44 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22904))

> Shai Almog says:
>
> That looks like a problematic cut of a 9-piece border
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — November 2, 2016 at 10:15 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-23073))

> Hristo Vrigazov says:
>
> Thanks for the guide! Is there a way to avoid this only using css, without modifiying the resulting theme.css.res file?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **shannah78** — November 3, 2016 at 12:31 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22835))

> shannah78 says:
>
> Can you share the CSS that you used to create this border?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — November 3, 2016 at 5:31 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-23180))

> Hristo Vrigazov says:
>
> Round1 {  
> width: 40pt;  
> height: 40pt;  
> border-radius: 20pt;  
> border: 1pt solid black;  
> text-align: center;  
> background-color: white;  
> }
>
> Round1InnerShadow {  
> cn1-derive: Round1;  
> box-shadow: inset 0px 0px 21pt 10pt rgba(61,59,61,0.48);  
> border: 1pt solid transparent;  
> background-color: white;  
> }
>
> The UIID is Round1InnerShadow
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **shannah78** — November 3, 2016 at 5:41 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22864))

> shannah78 says:
>
> Hmm.. That looks fine. Do you get the same behaviour with the Round1 UIID?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — November 3, 2016 at 7:49 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-22845))

> Hristo Vrigazov says:
>
> Yes, for example see this: [https://uploads.disquscdn.c…](<https://uploads.disquscdn.com/images/92f2cc3dea505cd0988a87c657eceba59bb6bd5427af2b7310ac294e5ccccec3.png>).
>
> I managed to pinpoint exact scenario that breaks it everytime like this. It is when I have a bigger and after it I show smaller. Not sure if this helps.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **shannah78** — November 4, 2016 at 6:48 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-23184))

> shannah78 says:
>
> Can you file an issue in the cn1-css issue tracker, and include a minimal test case (both the java and css)? I can’t seem to reproduce this problem using your CSS and a test case that I put together.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Hristo Vrigazov** — November 9, 2016 at 5:30 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-23206))

> Hristo Vrigazov says:
>
> Sorry, I can only reproduce it in my app for now, I can’t find a simple example that creates it. I will try to find one later
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Artur Hefczyc** — May 4, 2020 at 6:50 pm ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-21400))

> [Artur Hefczyc](https://avatars3.githubusercontent.com/u/1848738?v=4) says:
>
> Hi,  
> I tried linear-gradient examples but they seem to be not working. They give css compiler errors:  
> `  
> background: linear-gradient(to bottom,  
> rgba(242,246,248,1) 0%,  
> rgba(216,225,231,1) 50%,  
> rgba(181,198,208,1) 51%,  
> rgba(224,239,249,1) 100%  
> );  
> `  
> This results in the following CSS error:  
> `  
> CSS> Expecting color for param 3 of linear-gradient  
> CSS> Gradient not valid: null  
> `
>
> Then I simplify it to (removed the stop parameter):  
> `  
> background: linear-gradient(to bottom,  
> rgba(242,246,248,0.5),  
> rgba(216,225,231,0.7),  
> rgba(181,198,208,0.9),  
> rgba(224,239,249,1)  
> );  
> `  
> And I get another error:  
> `  
> CSS> Gradient not valid: alphas of start and end colors don’t match  
> `  
> So it looks like the linear-gradient does not support ‘stop’ parameter and it does not support custom alpha for each color. This is a shame as creating a linear gradient for a single color with different alpha for each stop gives really nice effects.
>
> Or maybe I am doing something wrong?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)


### **Shai Almog** — May 5, 2020 at 5:34 am ([permalink](https://www.codenameone.com/blog/rounded-corners-shadows-and-gradients-with-css.html#comment-21402))

> Shai Almog says:
>
> At this time we don’t support alphas on the gradient. It’s something we might be able to improve but it’s a bit rough to optimize since we sometimes generate image textures and customizing the alpha there is slightly harder.  
> If it’s important to you then this is something we can improve potentially.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Frounded-corners-shadows-and-gradients-with-css.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
