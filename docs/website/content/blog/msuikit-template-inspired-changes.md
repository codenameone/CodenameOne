---
title: MSUIKit Template Inspired Changes
slug: msuikit-template-inspired-changes
url: /blog/msuikit-template-inspired-changes/
original_url: https://www.codenameone.com/blog/msuikit-template-inspired-changes.html
aliases:
- /blog/msuikit-template-inspired-changes.html
date: '2016-10-12'
author: Shai Almog
---

![Header Image](/blog/msuikit-template-inspired-changes/Mobile-Material-UI-Screens.png)

There was much left unsaid about the  
[template UI](/blog/template-mobile-material-screens-ui-kit.html) that I did earlier in the week.  
It was doable & reasonably easy but I still had some pitfalls. I was able to circumvent them easily but I’m not so  
sure these would have been easy for other developers out there. As a result we decided to simplify some use  
cases in Codename One as a result.

I chose to separate these to a different post as most of the subjects here will be resolved in the next update, but  
the template post would probably still be useful when this post becomes history…​

### Side Menu Opening

The designs to the template UI placed the menu button at the top, we have a special case to allow that but that  
special case doesn’t deal with the settings placement at the top too. The solution I took was to define the  
theme constant `hideLeftSideMenuBool=true` which hides the sidemenu button. I then added my own button with  
the menu button icon as such:
    
    
    Button menuButton = new Button("");
    menuButton.setUIID("Title");
    FontImage.setMaterialIcon(menuButton, FontImage.MATERIAL_MENU);
    menuButton.addActionListener(e -> ((SideMenuBar)getToolbar().getMenuBar()).openMenu(null));

Most of that is pretty standard and easy except of the downcast to the `SideMenuBar` class. So we’ve added  
a method to `Toolbar` named `openSideMenu` which would make this far more reasonable:
    
    
    menuButton.addActionListener(e -> getToolbar().openSideMenu());

### Status Bar Control

Another difficulty is in the level of control over the status bar in iOS. iOS apps space their top portion to leave room  
for the status bar. You can customize that UIID but that means customizing it for all the forms which might not  
be what I want…​

So we’ve decided to add two methods to `Form`:
    
    
    protected boolean shouldPaintStatusBar();
    protected Component createStatusBar();

These should allow you to override whether the status bar is even shown on a per form basis and also manipulate  
it as needed during the creation phase.

__ |  The `StatusBarSideMenu` UIID lets you disable the status bar in the side menu only. You can just define  
its padding to 0 to remove the status bar from the sidemenu UI   
---|---  
  
### Same Size

It’s very common to want two components to occupy the same size. We often want that to align components  
so we have the methods:
    
    
    Component.setSameHeight(Component... cmps);
    Component.setSameWidth(Component... cmps);

These create a special case where the components in cmps return the same preferred width/height as the largest  
component in the group. Thus they all appear to have the same width or height.

The use case of layout is great but I found myself using these methods like this:
    
    
    Component.setSameHeight(notesLabel, notesPlaceholder);
    Component.setSameWidth(notesLabel, notesPlaceholder);

To allow a component and its placeholder to have the same size before I use `Container.replace` to fade in  
the component. So I added:
    
    
    Component.setSameSize(Component... cmps);

Which saves writing the same line twice…​

Now you might be thinking, why not use `notesPlaceholder.setPreferredSize(notesLabel.getPreferredSize())`.

This is how we used to do things and it does work well for simple cases like this. But what if I want to replace  
an entry not with a placeholder but rather with an entry that might be bigger on an axis?

In that case I’ll need to check which component is bigger in each axis and apply the right dimension which isn’t  
a problem with the original code.

__ |  I ignored the fact that `setPreferredSize` is deprecated because I don’t think that should be **the** reason   
---|---

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
