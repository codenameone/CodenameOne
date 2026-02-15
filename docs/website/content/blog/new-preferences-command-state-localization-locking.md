---
title: New Preferences, Command State, Localization & Locking
slug: new-preferences-command-state-localization-locking
url: /blog/new-preferences-command-state-localization-locking/
original_url: https://www.codenameone.com/blog/new-preferences-command-state-localization-locking.html
aliases:
- /blog/new-preferences-command-state-localization-locking.html
date: '2016-05-25'
author: Shai Almog
---

![Header Image](/blog/new-preferences-command-state-localization-locking/new-settings.png)

Starting with the new version of the NetBeans plugin we will have the new settings/preferences UI which we  
introduced in the [IntelliJ/IDEA plugin](/blog/a-new-idea.html). Currently this will be in addition to the main  
preferences but as we move forward we will only add features to the new settings UI.

You will be able to open the new project preferences UI by right clicking the project and selecting it in the  
Codename One section:

![Launching the new preferences UI](/blog/new-preferences-command-state-localization-locking/newsettings-ui.png)

Figure 1. Launching the new preferences UI

Notice that the UX while similar has some distinct differences:

  * We use the mobile style of scrolling as it was built with Codename One

  * You can see global settings by clicking the “Globe” button on the top left

  * You can save/cancel using the X or save buttons on the top right.

We will probably make quite a few changes to this UI in the coming months to refine it further based on feedback  
from you guys.

The main motivation for doing this change is the new Windows UWP port which needed changes to the windows  
section of the preferences. Doing this 3 times over is silly, changing one single global preferences tool is always  
the right thing to do.

__ |  This isn’t yet implemented in the Eclipse plugin but it might be done before the next update   
---|---  
  
### Command Icon States

[In a previous post](/blog/pressed-selected-icon-font-utf-8.html) we mentioned the ability to handle states  
in buttons and how that fits well with icon fonts.

A question in the post raised the issue of commands which support such states but the API isn’t there.

To solve this we added a version of `setMaterialIcon` to [FontImage](/javadoc/com/codename1/ui/FontImage/)  
that accepts a [Command](/javadoc/com/codename1/ui/Command/) as  
its argument. This effectively makes commands work with such icon fonts.

To support that further we also added to [Toolbar](/javadoc/com/codename1/ui/Toolbar/)  
the methods: `addMaterialCommandToSideMenu`, `addMaterialCommandToRightBar`, `addMaterialCommandToLeftBar` &  
`addMaterialCommandToOverflowMenu`.

These accept one of the `MATERIAL_*` char constants from the `FontImage` class to create the icon for a command  
relatively easily.

### Suppress Localization

By default strings in Codename One are implicitly localized which is unique. Most frameworks require some  
level of intervention to implement localization but since Codename One was developed by people whose  
native language isn’t English we felt compelled to fix that…​

Localization should be the default and Codename One does the right thing here, however sometimes you want to  
turn it off e.g. if you have a user submitted string that might be identical to an application resource bundle value.

In `Label` we have the  
[setShouldLocalize](/javadoc/com/codename1/ui/Label/#setShouldLocalize-boolean-)  
method which works great for disabling the implicit localization. However, as  
[issue 1744](https://github.com/codenameone/CodenameOne/issues/1744) pointed out this needs to be done for  
other components too…​

So we added the same method to `SpanLabel`, `SpanButton` & `MultiButton`. You can now control localization  
specifically in all of those components.

### Better Locking

Image locking is an esoteric performance implementation detail that most of you can and should be unaware of.

If you don’t care about the nitty gritty skip this section, for those of you who care and don’t know what I’m talking  
about you can  
[read this section and sidebar in the developer guide](/manual/graphics/#_encodedimage).

In the coming update we fixed issue [#1746](https://github.com/codenameone/CodenameOne/issues/1746) to  
also lock other button states (this was very visible with things like toggle buttons). But the main change was  
also changing the locking behavior to act more like a smartpointer and less as a boolean flag which should reduce  
the cases of memory thrashing when using the same encoded image, over and over…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chibuike Mba** — May 31, 2016 at 11:59 pm ([permalink](/blog/new-preferences-command-state-localization-locking/#comment-22680))

> Hi Shai,
>
> addMaterialCommandToLeftBar of the Toolbar adds the command to the right instead of left.
>
> Secondly, FontImage passed to any of these methods does not show addMaterialCommandToRightBar and addMaterialCommandToLeftBar.
>
> Regards.
>



### **Shai Almog** — June 1, 2016 at 3:43 am ([permalink](/blog/new-preferences-command-state-localization-locking/#comment-22739))

> Shai Almog says:
>
> Hi,  
> thanks for noticing 😉  
> We fixed that in git.
>
> We also fixed the issue and added an additional version of the method that accepts font size. Should be in this Friday release.
>



### **Chibuike Mba** — June 1, 2016 at 10:09 am ([permalink](/blog/new-preferences-command-state-localization-locking/#comment-22651))

> Chibuike Mba says:
>
> Nice one Shai. Waiting for Friday release.
>
> I also noticed that FontImage added to addMaterialCommandToSideMenu does not change color based on the command state Pressed/Unselected. Has that been taken care of already and pending Friday release?
>
> Regards.
>



### **Shai Almog** — June 2, 2016 at 3:35 am ([permalink](/blog/new-preferences-command-state-localization-locking/#comment-22543))

> Shai Almog says:
>
> I hope it does. If it doesn’t after this Friday release please file an issue.
>



### **Chibuike Mba** — June 2, 2016 at 2:32 pm ([permalink](/blog/new-preferences-command-state-localization-locking/#comment-22738))

> Chibuike Mba says:
>
> OK.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
