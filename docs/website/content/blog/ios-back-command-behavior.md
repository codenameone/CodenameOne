---
title: iOS Back Command Behavior and Facebook Clone Update
slug: ios-back-command-behavior
url: /blog/ios-back-command-behavior/
original_url: https://www.codenameone.com/blog/ios-back-command-behavior.html
aliases:
- /blog/ios-back-command-behavior.html
date: '2018-04-16'
author: Shai Almog
---

![Header Image](/blog/ios-back-command-behavior/pixel-perfect.jpg)

I’ve been working on the new Facebook clone app, I have a lot to say about that but I’ll defer that for now. One of the things that Facebook did is provide a different experience in iOS & Android. I wanted to replicate that by using a more iOS style back behavior in my clone.

![Back Command iOS/Android & Facebooks native on Android](/blog/ios-back-command-behavior/facebook-clone-back-command.png)

Figure 1. Back Command iOS/Android & Facebooks native on Android

On Android we use an arrow to indicate back but on iOS we usually show the title of the previous form with a `<` icon to indicate the back action. Up until recently this was represented in the `BackCommand` UIID with a an image aligned to the left of the command. This was hard to style and made very little sense.

With the next update we’ll use the builtin material icon for this when you set a back command. In this form I used the following code for back:
    
    
    getToolbar().setBackCommand(backLabel,
            Toolbar.BackCommandPolicy.WHEN_USES_TITLE_OTHERWISE_ARROW,
            e -> previous.showBack());

The `backLabel` variable represents the title of the previous form. The toolbar policy indicates an arrow will be used on Android and a Title will appear on iOS.

This will work with the new material font code in the next update (you need to update the skins too). You can disable or force the font image behavior with the new theme constant `iosStyleBackArrowBool=false`.

### Material Icon Commands

As part of that work we added three new methods to `Command`:
    
    
    public void setMaterialIcon(char materialIcon);
    public void setMaterialIconSize(float size);
    public void setIconGapMM(float iconGapMM);

These allow control over the gap between the icon and the label for the command element as well as set the icon as a material icon.

We updated a lot of the `Toolbar` code to use these internally so UI will adapt better to changes in UIID’s.

### Landscape UIID’s

`setUIID` is a core API that hasn’t changed in ages. We just added a new version of this API that accepts two strings. One represents the UIID in portrait and the other (optional one) can represent a different UIID for landscape.

__ |  If they are the same the other string should be `null`  
---|---  
  
This allows us to easily implement minor UI changes that make sense when switching orientation e.g. smaller font/padding in the title area. Since that is the chief use case we also added a theme constant which is currently `false` by default: `landscapeTitleUiidBool=true`.

When you set this to true the UIID’s: `ToolbarLandscape`, `TitleCommandLandscape`, `BackCommandLandscape` &`TitleLandscape` will be used for landscape mode. They all derive their non-landscape versions so just setting the flag to `true` should have no effect. You will need to edit these styles to support this behavior.

### Container UIID & Font Api’s

While we are on the subject of UI enhancements we also made a few enhancements to the general API.

`Container` now accepts a UIID in the constructor with the layout. It’s one less line of code when you create a container.

We moved the constants from `Font` into the `CN` class and added a couple of helper methods to create fonts more easily. Specifically:
    
    
    public static Font createTrueTypeFont(String fontName);
    public static Font createTrueTypeFont(String fontName, float sizeMm);

So instead of doing:
    
    
    Font n = Font.createTrueTypeFont("native:MainLight", "native:MainLight").
        derive(convertToPixels(3));

You can do:
    
    
    Font n = Font.createTrueTypeFont("native:MainLight", 3);

### Summary

I hope we won’t break anything with these changes but experience tells me there is always some nuance. Hopefully these will resolve themselves as we move forward.

I have a lot more to say about the Facebook app, I hope I’ll complete it in time but frankly with my current status it’s a bit doubtful as I’m seriously delayed and back logged. On the plus side what I have so far looks great!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — May 18, 2018 at 7:43 am ([permalink](https://www.codenameone.com/blog/ios-back-command-behavior.html#comment-23915))

> Thank you very much for this information, this post helped me on setting correctly the back buttons for iOS and Android. Thank you for all the other news.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
