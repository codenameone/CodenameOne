---
title: Edit Styles in Simulator
slug: edit-styles-simulator
url: /blog/edit-styles-simulator/
original_url: https://www.codenameone.com/blog/edit-styles-simulator.html
aliases:
- /blog/edit-styles-simulator.html
date: '2017-04-11'
author: Shai Almog
---

![Header Image](/blog/edit-styles-simulator/uidesign.jpg)

One of the biggest pain points in Codename One is theming, there are several things we did to alleviate the problem but it’s an inherently complex problem. One difficulty people have is in the disconnect between what we see in the UI and the styling in the designer. This creates a disconnect that is often hard to bridge.

In the past we added the Component Inspector tool to the simulator that allows you to discover the UIID’s of the various components and change the UIID’s to see the effect. This is very helpful for inspecting a running application and understanding what we see on the screen. Unfortunately, this is a half measure as we need to see something in the Component Inspector then open it in the designer restart the app rinse/repeat…​

That is until now!

With the update this week we added this:

If you don’t have the patience for the video or don’t understand what you just saw this tool allows you to edit any style within a running app (that you built) and instantly preview the changes!

It works with multiple res files/themes but might be “flaky” in such cases as it needs to re-apply the theme. You can open the Component Inspector by clicking Simulator → Component Inspector. You might want to use refresh when reviewing the UI.

__ |  This changes the `.res` file so if you have it open in a designer you might overwrite changes made by this tool!   
---|---  
  
### How does it Work?

A while back Chen wanted to add the ability to edit a style into the new GUI builder so he created a [command line argument to the designer](/blog/using-designer-command-line-options.html). He did that after I wrote that article so it isn’t documented there but you can effectively open the style editor for of the designer using something like:
    
    
    java -jar ~/.codenameone/designer_1.jar -style path-to-res-file.res UIID NameOfTheme

So to edit the `Button` in `theme.res` in `MyProject` I would probably do:
    
    
    java -jar ~/.codenameone/designer_1.jar -style ~/MyProject/src/theme.res Button Theme

Notice that unlike the dialog that opens in the designer tool this one allows you to select the style type you wish to edit using radio buttons on the bottom.

The edit function launches that command but it also has another function of refreshing the UI. So when editing completes we update the theme in RAM and refresh the current Form. This works well for some cases but might cause some odd conflicts. E.g. if you navigate back to a form created before it might still have the old theme.

There might also be issues with layered themes, e.g. if you have 2 themes layered one on top of the other this might trigger a conflict.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Jérémy MARQUER** — April 12, 2017 at 2:02 pm ([permalink](/blog/edit-styles-simulator/#comment-23122))

> Jérémy MARQUER says:
>
> Great enhancement ! 🙂
>



### **Avelblood** — April 15, 2017 at 12:13 pm ([permalink](/blog/edit-styles-simulator/#comment-21576))

> Avelblood says:
>
> Awesome. The dream comes true =)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
