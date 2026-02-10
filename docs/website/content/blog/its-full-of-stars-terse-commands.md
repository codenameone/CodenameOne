---
title: It's Full Of Stars & Terse Commands
slug: its-full-of-stars-terse-commands
url: /blog/its-full-of-stars-terse-commands/
original_url: https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html
aliases:
- /blog/its-full-of-stars-terse-commands.html
date: '2016-02-23'
author: Shai Almog
---

![Header Image](/blog/its-full-of-stars-terse-commands/components-slider.png)

A very common UI pattern is the 5 star ranking system. Up until recently we always had the same answer  
when developers asked us how to implement it: “Use toggle buttons  
([CheckBox](https://www.codenameone.com/javadoc/com/codename1/ui/CheckBox.html#createToggle-com.codename1.ui.Image-))”.

This is still not a bad answer but we think there is a “better” simpler way to do this thru the  
[Slider](https://www.codenameone.com/javadoc/com/codename1/ui/Slider.html) which was  
effectively designed with this usage in mind.

The best way to do that is to just create two images with all 5 stars full and with all 5 stars empty and assign  
this to the `Slider`/`SliderFull` UIID’s. Keep in mind that you need to apply both to the selected and unselected  
states of the UIID’s.

__ |  You can change the UIID of slider itself e.g. to something like “Stars” at which point the UIID’s will be  
`Stars` & `StarsFull`.   
---|---  
  
This will allow your users to click/drag to select the number of stars. The code below uses the star material icon  
to generate something like this on the fly without any resources.

__ |  We enclose the slider in a `FlowLayout` to prevent it from growing. This is because I chose the stars to be tiled  
instead of aligned (like we could if we used an image). So if the component will grow it won’t have the right feel.  
Enclosing the component in a `FlowLayout` is an old trick to prevent components from growing beyond their preferred  
size.   
---|---  
      
    
    private void initStarRankStyle(Style s, Image star) {
        s.setBackgroundType(Style.BACKGROUND_IMAGE_TILE_BOTH);
        s.setBorder(Border.createEmpty());
        s.setBgImage(star);
        s.setBgTransparency(0);
    }
    
    private Slider createStarRankSlider() {
        Slider starRank = new Slider();
        starRank.setEditable(true);
        starRank.setMinValue(0);
        starRank.setMaxValue(10);
        Font fnt = Font.createTrueTypeFont("native:MainLight", "native:MainLight").
                derive(Display.getInstance().convertToPixels(5, true), Font.STYLE_PLAIN);
        Style s = new Style(0xffff33, 0, fnt, (byte)0);
        Image fullStar = FontImage.createMaterial(FontImage.MATERIAL_STAR, s).toImage();
        s.setOpacity(100);
        s.setFgColor(0);
        Image emptyStar = FontImage.createMaterial(FontImage.MATERIAL_STAR, s).toImage();
        initStarRankStyle(starRank.getSliderEmptySelectedStyle(), emptyStar);
        initStarRankStyle(starRank.getSliderEmptyUnselectedStyle(), emptyStar);
        initStarRankStyle(starRank.getSliderFullSelectedStyle(), fullStar);
        initStarRankStyle(starRank.getSliderFullUnselectedStyle(), fullStar);
        starRank.setPreferredSize(new Dimension(fullStar.getWidth() * 5, fullStar.getHeight()));
        return starRank;
    }
    private void showStarPickingForm() {
        Form hi = new Form("Star Slider", new BoxLayout(BoxLayout.Y_AXIS));
        hi.add(FlowLayout.encloseCenter(createStarRankSlider()));
        hi.show();
    }

In this code you will notice we allow selecting a value between 0 & 10 where 10 is really 5 stars. This allows us  
to pick values like 4.5 stars and just divide the actual value. However, most ranking systems don’t allow a value  
below 1 star. To solve this you can just use a `Label` to represent the first star and use a the `Slider` for the remaining  
4 stars. In which case the values would be between 0 – 8.

### Terse commands

I love lambdas. I wasn’t a fan before they were introduced but they grew on me and made me a convert.

One of the annoyances I had with Codename One was with using `Command` syntax which forced me to fallback  
to pre-lambda code for practically everything as `Command` is a class and not a single method interface. This  
bothered me enough to do something about it so now we have `ActionListener` versions of many `Command` API’s.

These all redirect to the  
[Command.create(String,Image,ActionListener)](https://www.codenameone.com/javadoc/com/codename1/ui/Command.html#create-java.lang.String-com.codename1.ui.Image-com.codename1.ui.events.ActionListener-)  
method which effectively creates a `Command` with the given details for the given action listener. So instead  
of writing code like this:
    
    
    form.getToolbar().addToSideMenu(new Command("My Command") {
         public void actionPerformed(ActionEvent ev) {
             myCodeHere();
         }
    });

I can write this:
    
    
    form.getToolbar().addToSideMenu(Command.create("My Command", null, (ev) -> {
        myCodeHere();
    }));

And to make things even simpler I created helper methods that do that implicitly in `Toolbar` and `Form`:
    
    
    form.getToolbar().addToSideMenu("My Command", null, (ev) -> {
        myCodeHere();
    });

Notice that the version of this method that accepts a the action listener also returns the created `Command` instance  
which might be useful if you want to do something with the command later on (e.g. remove it). So this should work:
    
    
    Command cmd = form.getToolbar().addToSideMenu("My Command", null, (ev) -> {
        myCodeHere();
    });
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — February 24, 2016 at 2:49 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22227))

> Diamond says:
>
> Hi Shai,
>
> Please check this page, it’s not aligned.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Shai Almog** — February 24, 2016 at 2:52 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22727))

> Shai Almog says:
>
> Hi Diamond,  
> I just played a bit with the UI for asciidoc conversion of blog posts. What browser/OS combination are you using? Can you provide a screenshot so I can see if we are seeing the same thing?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Diamond** — February 24, 2016 at 5:50 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22465))

> Diamond says:
>
> Chrome on Windows 10 machine.
>
> Here is the screenshot.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Shai Almog** — February 24, 2016 at 6:39 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22673))

> Shai Almog says:
>
> Gotcha, that’s a really wide screen. Looking into it.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Shai Almog** — February 25, 2016 at 7:25 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22408))

> Shai Almog says:
>
> Just did an update, is it better?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Diamond** — February 25, 2016 at 7:26 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22500))

> Diamond says:
>
> Yes, it’s fixed now.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Shai Almog** — December 20, 2016 at 4:43 am ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-22867))

> Shai Almog says:
>
> Thanks!  
> Just use getProgress() on the Slider component: [http://codenameone.com/java…](<http://codenameone.com/javadoc/com/codename1/ui/Slider.html>)  
> Not the most intuitive method name for this case but we initially designed it as a progress indicator…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **safa** — April 29, 2017 at 10:53 pm ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-23331))

> safa says:
>
> hello , how can i get and set the values of rating stars?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)


### **Shai Almog** — April 30, 2017 at 4:14 am ([permalink](https://www.codenameone.com/blog/its-full-of-stars-terse-commands.html#comment-23482))

> Shai Almog says:
>
> The guy who asked this 4 months ago deleted his question but my answer is the same:  
> Just use getProgress() on the Slider component: [http://codenameone.com/java](<http://codenameone.com/java>)…  
> Not the most intuitive method name for this case but we initially designed it as a progress indicator…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fits-full-of-stars-terse-commands.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
