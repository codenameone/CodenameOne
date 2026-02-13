---
title: Using Icon Fonts Such as Fontello
slug: using-icon-fonts-such-as-fontello
url: /blog/using-icon-fonts-such-as-fontello/
original_url: https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html
aliases:
- /blog/using-icon-fonts-such-as-fontello.html
date: '2016-08-22'
author: Shai Almog
---

![Header Image](/blog/using-icon-fonts-such-as-fontello/font-awesome.jpg)

A lot of our focus in the past couple of releases has been around the material design icon fonts, they changed the way we build UIs. We also support arbitrary font icons, this features isn’t limited to material icons [and we blogged about it a while back](/blog/icon-fonts-oldvm-swan-song.html).

After going back and forth with developers we got the sense that using an icon font such as fontello wasn’t clear. In this short tutorial we’ll try to explain the process and benefits.

__ |  On an unrelated note, if you haven’t upvoted our [stackoverflow ad](http://meta.stackoverflow.com/a/332873/756809) please [do this now](http://meta.stackoverflow.com/a/332873/756809) & help us promote Codename One!  
Click on the up arrow next to the ad if you have a stackoverflow account.   
---|---  
  
### Why Icon Font & Not MultiImage?

Both have their place. When in doubt I would pick an icon font as it allows us to adapt the colors dynamically. It can even change the color based on state (e.g. different icon color for pressed/released state).

Another big advantage for icon fonts is application size which can be small. Some designs aren’t achievable with icon fonts in which case there is no other option.

### Getting Started

Go to the icon font website e.g. [fontello.com](http://fontello.com/) and select the icons you want.

__ |  Notice the license terms for each font as fontello might mix different font licenses!   
---|---  
  
Once you picked the desired icons click the download button and save/extract the font zip file.

![Fontello website with icon selection](/blog/using-icon-fonts-such-as-fontello/fontello-website1.png)

Figure 1. Fontello website with icon selection

Once you unzip the file copy the `fontello.ttf` file to the `src` directory in your project. Then open the `demo.html` file in the browser. Click the show codes checkbox on the top right side and you should see something like this:

![Fontello demo html file](/blog/using-icon-fonts-such-as-fontello/fontello-website2.png)

Figure 2. Fontello demo html file

If you look at the symbols you will notice they are hex values, e.g. notice the heart symbol in my font matches `0xe809`. This would be the value for the `FontImage` when we create it, you can convert it to Java syntax with the `u` notation as `ue809`;

E.g. I can create a 4mm red heart icon using:
    
    
    Font fnt = Font.createTrueTypeFont("fontello", "fontello.ttf");
    int size = Display.getInstance().convertToPixels(4);
    FontImage fm = FontImage.createFixed("ue800", fnt, 0xff0000, size, size);

A better approach would use styles. We can define UIID’s in the designer and pick `fontello.ttf` from the combobox of fonts as such:

![Using styles for icon fonts is the best way to define icon fonts](/blog/using-icon-fonts-such-as-fontello/fontello-website3.png)

Figure 3. Using styles for icon fonts is the best way to define icon fonts

Once you define a style in the theme it’s easy to keep it consistent with everything else. E.g. I can change the code above to create a red heart like this:
    
    
    Style s = UIManager.getInstance().getComponentStyle("RedIcon");
    FontImage fm = FontImage.createFixed("ue800", s);

We can also create a selected variation using `getComponentSelectedStyle`:
    
    
    Style s = UIManager.getInstance().getComponentSelectedStyle("RedIcon");
    FontImage fm = FontImage.createFixed("ue800", s);

And the pressed version using `getComponentCustomStyle`:
    
    
    Style s = UIManager.getInstance().getComponentCustomStyle("RedIcon", "press");
    FontImage fm = FontImage.createFixed("ue800", s);

### Final Word

We love flat design, it made font icons pervasive and those are easy to work with. Once you pick them up you will find it hard to stop using them.

If you have any thoughts on how to make the font image API’s easier to use let us know…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **3lix** — August 23, 2016 at 4:39 pm ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-22962))

> Hi, I noticed with the material design support , the back button (more so the back command) now has a ripple effect when clicked, but not the hamburger menu (at least testing on android).  
> Can I add this ripple effect to any button in general , or icon ? If so, I would like to know how.  
> Thanks!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **Shai Almog** — August 24, 2016 at 3:54 am ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-22944))

> This isn’t ripple, it’s a pressed color. Did you define the theme constant sideMenuImage?  
> If so try removing it and see if the pressed color for the item changes.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **3lix** — August 24, 2016 at 3:23 pm ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-22742))

> I believe that what I am seeing on my device is a ripple effect. The background button has a circular background The background color for the back button when clicked / long pressed changes gradually starting from the center down to the circle’s edges over about a half a second time period.
>
> If it was just a background color that is set when clicked then it would not change “gradually” it would instead get updated all at once.
>
> I will try this week to recreate this behavior from a bare bone app and share it. I see the ripple effect only on an android device.
>
> The ripple effect is not a necessity to me at this point. But I do see the effect supported somehow already. I will test more this afternoon.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **Shai Almog** — August 25, 2016 at 5:23 am ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-22655))

> Shai Almog says:
>
> If I understand you correctly you are using an older project with a native theme. This means you aren’t using the Toolbar and are using the native ActionBar which is an approach we moved away from.
>
> Since that is a native component it will have the native ripple effect.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **3lix** — August 25, 2016 at 2:38 pm ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-22896))

> 3lix says:
>
> I see that makes sense now. I was not using the toolbar on that specific form. Also you are right I don’t see the effect if I use the Toolbar.
>
> I actually used “Form”.setBackCommand(backCommand Also I am not using a native theme but I think probably due to the way I am using the theme it is indeed using the native component.
>
> Thank you for the insights and for your time! I understand what is going on now.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **Ch Hjelm** — April 13, 2019 at 9:51 am ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-24113))

> Ch Hjelm says:
>
> Hi, I would like to start using external Icon fonts in addition to the CN1 Material font. Is there some way to use external fonts as easily as the built-in Material icons, e.g. use them in `Label.setMaterialIcon()`? A method like setMaterialIcon() is really practical because it automatically sets size and color of the icon as defined by the Label UIID style, but it currently seems to have hard-coded support for the predefined CN1 Material icons only?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **Shai Almog** — April 14, 2019 at 3:57 am ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-24106))

> Shai Almog says:
>
> That would be great but I’m not sure how we can enable something like that in terms of the API?  
> One of the ideas that floated around when we introduced FontImage was to create it in an “auto-adapt” mode so it will use component styles all the time. This makes the code a bit awkward as you’d need to create multiple FontImage instances and won’t be able to reuse them.  
> The material code is easy to do because we just pass the character and possibly the size. Here we’d need the additional information of the font which changes the balance a bit. If you have a suggestion of how the API should look I’d be interested in hearing that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)


### **Ch Hjelm** — April 14, 2019 at 9:07 am ([permalink](https://www.codenameone.com/blog/using-icon-fonts-such-as-fontello.html#comment-24092))

> Ch Hjelm says:
>
> I’m not much into the inner workings of CN1 but the external api could maybe be something like `Label.setFontIcon(Font font, char icon)`.
>
> I can see that `FontImage.setMaterialIcon((Label l, char icon, float size)` calls getMaterialDesignFont() to get the Material font, so if that font could somehow be passed as a parameter instead, maybe that would do trick.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fusing-icon-fonts-such-as-fontello.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
