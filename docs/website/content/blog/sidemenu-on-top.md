---
title: Sidemenu On Top
slug: sidemenu-on-top
url: /blog/sidemenu-on-top/
original_url: https://www.codenameone.com/blog/sidemenu-on-top.html
aliases:
- /blog/sidemenu-on-top.html
date: '2017-05-15'
author: Shai Almog
---

![Header Image](/blog/sidemenu-on-top/new-features-3.jpg)

This feature is still undergoing development but I wanted to share the progress here so we can start getting bug reports and suggestions. One of the frequent requests for the side menu UI is to provide a way for it to render on top of the UI instead of shift the UI. The old side menu was written when the facebook app was the chief application that used that UI paradigm so it was built to match that approach.

The chief problem with the old side menu is that it was written prior to the `Toolbar`. As a result it was based on the `Menu` class which was designed in the age of feature phones. Many assumptions that were true back then no longer hold and as a result the side menu has a few elaborate hacks in place to make it feel fluid. When we started looking into the process of rendering the side menu on top we hit a wall.

There are multiple conflicting places in the code that position the side menu and do so in elaborate and unintuitive ways. The current side menu implementation is held back by legacy that makes even a seemingly simple change like this challenging. As a result we decided to take a completely different direction for the “on top” sidemenu.

### A New Direction

When the original side menu was designed we didn’t have a layered pane and no `InteractionDialog` as a result the options for implementing the side menu were limited. When we ran into difficulty with implementing the “on top” mode in the same way as the regular side menu we decided to shift our focus into the Toolbar class. The on-top side menu is implemented entirely within the `Toolbar`.

The on-top side menu is based on the work we did for the [permanent side menu](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more.html) and as a result some incompatibilities and different behaviors will occur when you use that approach. This side menu is placed into an interaction dialog and we use pointer event listeners to track the drag motion to expand and collapse it.

This means a few behaviors of the current side menu will be different:

  * Currently the on-top side menu appears below the `Toolbar` – We have a fix for this but it’s a bit “buggy” see the discussion below

  * On-top Side menu isn’t a separate form – the original side menu was a separate form. That means the existing UI was de-initialized & some subtle behaviors were different. The underlying UI would still be “live”

  * Shadow isn’t supported, however the underlying form is “darkened” gradually as we drag

  * Performance might not be as smooth at this time – we optimized the hell out of the old side menu, this isn’t as refined

  * The menu button is simply added to the left side – the old side menu had a special case we can’t use

  * Many of the theme constants aren’t supported yet, this is work in progress

This is how the new side menu looks in the kitchen sink:

We’d appreciate if you try the new side menu and let us know where you run into issues, to try is just add the following line to your init method:
    
    
    Toolbar.setOnTopSideMenu(true);

In the future we will add a theme constant for this and might flip it to be the default.

### Another Layered Pane

One of the problems with the new side menu is that it doesn’t cover the entire form due to the fact that `InteractionDialog` can’t render on the whole form. That is because the layered pane which the `InteractionDialog` depends on wraps the content pane and not the full `Form`.

That’s not a bad decision as the content pane is where we want most of our UI but there are special cases. Up until now our only option was glass pane but it’s too “low level”. To solve this we added a new layer into Codename One with `getFormLayeredPane` which is semantically identical to `getLayeredPane` but is on top of the entire `Form` not just the content pane.

To prevent potential overhead we only add the form layered pane as needed based on user requests. Internally the form has a hidden border layout that you can’t normally access. It places the title area into its right place as well as the content pane etc. Adding another layer might have impacted compatibility or performance in a way that is too disruptive so we chose to use a rather creative approach…​

We added a new constraint to `BorderLayout` named `OVERLAY` which you can use as:
    
    
    myBorderLayoutContainer.add(BorderLayout.OVERLAY, placeThisContainerOnTop);

This just places the component on top (or below) all of the elements within the `Container` so the overlay element will literally span the full size of the border layout regardless of all the other elements.

__ |  Z-ordering in Codename One is determined by the order of the elements so the last added element is on top by default   
---|---  
  
The `InteractionDialog` now has a special mode of working on the form layer which works for simple cases but has some rendering artifacts probably due to existing assumptions in the code regarding the `Form` which has some special cases. That’s why it isn’t enabled by default for the on-top side menu.

Ideally as we improve this and fix the issue we’ll flip the switch to use the form layered pane.

### Finally

This is a bit of a big change, ideally this side menu should be the default but that probably won’t happen for 3.7 as the stability needs to improve. Once this is more stable we will probably flip the switch so developers can get the benefit of a more modern and simpler architecture facilitated by this new side menu.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Carlos** — May 17, 2017 at 1:28 pm ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23485))

> Carlos says:
>
> Thanks, this is a huge advance. Some problems I’ve seen:
>
> – If you have a button in the form with a action listener and tap over the side panel, the button below will still work, which is a problem. Everything in the form should be disabled when the side panel is open.
>
> – The animation when tapping the icon is fine, but dragging the panel is a bit ugly, because the side commands shrink instead of just moving left.
>
> – And this is me being clumsy: how on earth do you change the hamburger icon color? I can’t see any theme entry, and the side icon constant still doesn’t work.
>



### **Shai Almog** — May 18, 2017 at 5:20 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23543))

> Shai Almog says:
>
> Thanks for the feedback!
>
> – Thanks I’ll fix that. It’s a pretty easy fix just make the side menu content pane focusable which means it will grab events in the hierarchy.
>
> – I’m not sure I can do much about it. There are some deeply held assumptions that prevent me from placing a dialog or interaction dialog “offscreen”. It might be something worth addressing.  
> Alternatively, a hack might be possible here where the Container “thinks” it has the final size during drag. I’m not sure how hard this will be to address.
>
> – It should now use the standard TitleCommand UIID instead of the MenuCommand UIID. Since I used the addMaterialCommandToSideMenu call. I did use the menuImageSize theme constant though which currently defaults to 4.5.
>



### **Klug Gauvain** — May 18, 2017 at 2:37 pm ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23492))

> Klug Gauvain says:
>
> Hello, found another bug: If your sidemenu need to be scrolled, with the on top menu it’s impossible ! The menu expand or retracts itself when trying to scroll.
>



### **Shai Almog** — May 19, 2017 at 4:55 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23282))

> Shai Almog says:
>
> Thanks, I see this. It missed the deadline for this week but I’ll try to do it for next weeks update. Can you file an issue so it doesn’t get lost?
>



### **Klug Gauvain** — May 19, 2017 at 7:19 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23373))

> Klug Gauvain says:
>
> Issue submitted
>



### **beck** — June 24, 2017 at 3:54 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23524))

> beck says:
>
> It seems nice but it’d have been better if the side menu covers whole screen (on top of the toolbar as well) though the problem was discussed above. I hope it’ll cover the whole screen in future.
>



### **Shai Almog** — June 24, 2017 at 4:39 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-24148))

> Shai Almog says:
>
> Notice the content of the section titled “Another Layered Pane”
>



### **Gareth Murfin** — January 9, 2018 at 7:26 pm ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23730))

> Gareth Murfin says:
>
> How do you turn this off? It appears on all my forms after my splash and I dont need it.
>



### **Shai Almog** — January 10, 2018 at 5:26 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23842))

> Shai Almog says:
>
> Toolbar.setOnTopSideMenu(false);
>



### **Gareth Murfin** — January 10, 2018 at 8:00 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23852))

> Gareth Murfin says:
>
> doesnt seem to have any effect here Shai, any other things I need to do? Im using old gui builder project, but its a new project.
>



### **Shai Almog** — January 12, 2018 at 7:06 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23607))

> Shai Almog says:
>
> It disables this feature when used in the init(Object) method. You are probably seeing something else
>



### **Yaakov Gesher** — January 21, 2018 at 8:53 pm ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23764))

> Yaakov Gesher says:
>
> Hi, I’m having trouble styling the side menu. How can I make the commands in the side menu render right-to-left? I tried `getToolbar().getMenuBar().setRTL(true);` but it didn’t seem to have an effect. Also, I wanted to make the side menu wider than just the width of the command texts, but the constants that worked with the older version don’t do anything any more. I tried editing the TitleCommand style, but it didn’t help. Thanks!
>



### **Shai Almog** — January 22, 2018 at 4:23 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23673))

> Shai Almog says:
>
> It should be seamless for an RTL app once we actually implement it and you wouldn’t need to do anything.  
> Unfortunately we didn’t implement RTL yet or the right hand sidemenu in the on-top mode. It’s somewhere on our todo list hopefully we’ll be able to address it before 4.0.
>



### **Yaakov Gesher** — January 22, 2018 at 10:35 pm ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23929))

> Yaakov Gesher says:
>
> Sorry, maybe it wasn’t clear what I wanted to accomplish: I know that the menu currently can’t be made to open from the right, but I at least wanted the text on the commands to be right-aligned. Eventually I accomplished this by adding components rather than Commands to the menu. But is there a way to give the menu a fixed width?
>



### **Shai Almog** — January 23, 2018 at 6:42 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23936))

> Shai Almog says:
>
> I suggest using screenshots to illustrate a question as it would make it easier to answer. Components determine their size based on preferred size. It should “just work” and if you manipulate it this might break.
>
> You can use RTL on a command by using toolbar.findCommandComponent(cmd) and setting the RTL flag on that component explicitly.
>



### **Francesco Galgani** — March 15, 2018 at 5:09 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-23682))

> Francesco Galgani says:
>
> Are the BorderLayout.OVERLAY and the LayeredLayout equivalent? Can I use them in the same cases?
>



### **Shai Almog** — March 16, 2018 at 5:11 am ([permalink](https://www.codenameone.com/blog/sidemenu-on-top.html#comment-21635))

> Shai Almog says:
>
> You always have a layered layout as there is one in the form. Layouts are constrained to their own container and are unaware of the existence of other layouts.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
