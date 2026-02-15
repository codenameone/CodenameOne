---
title: Toolbar
slug: toolbar
url: /blog/toolbar/
original_url: https://www.codenameone.com/blog/toolbar.html
aliases:
- /blog/toolbar.html
date: '2014-12-21'
author: Shai Almog
---

![Header Image](/blog/toolbar/toolbar-1.png)

  
  
  
  
![Picture](/blog/toolbar/toolbar-1.png)  
  
  
  

  
  
  
  
One of the common requests we get from users is more customization for the title bar area with more flexibility e.g. placing a TextField for search or buttons in various ways. Chen recently took action on this by introducing the new Toolbar API that replicates some of the native functionality available on Android/iOS and integrates with features such as the side menu to provide very fine grained control over the title area behavior. 

Everything that the toolbar allows was possible in the past with various customizations, the difference is that the Toolbar API makes such use cases much simpler. To get started we can set a Toolbar to a Form using myForm.setToolbar(toolbar). 

At that point we can add commands to the side menu with most of the existing  
[  
side menu features  
](http://www.codenameone.com/blog/on-the-side-up-on-top)  
(such as SideComponent), however to add a command to the SideMenu we will need to use toolbar.  

  
  
  
  
addCommandToSideMenu() instead of the standard add command. We can add commands to 4 locations:  
  
  

  1.   
  
addCommandToSideMenu – adds to the sidemenu  
  
  

  2.   
  
addCommandToOverflowMenu – adds the command to an Android style … menu on the top right hand side  
  
  

  3.   
  
addCommandToRightBar – places the command on the right side of the title  
  
  

  4.   
  
  
  
addCommandToLeftBar – places the command on the left side of the title  
  
  
  
  

  
  
  
Normally you can just set a title with a String but if you would want the component to be a text field or a multi line label you can use  
  
  
  
  
setTitleComponent(Component) which allows you to install any component into the title area.

At this time the GUI builder doesn’t include toolbar specific functionality, depending on user requirements we might add this in the future.  
  

  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — December 22, 2014 at 6:23 pm ([permalink](/blog/toolbar/#comment-22247))

> Anonymous says:
>
> Nice! I hope it’s also easier now to change the color of the toolbar. Thanks for adding this!


### **Anonymous** — January 4, 2015 at 12:35 pm ([permalink](/blog/toolbar/#comment-22070))

> Anonymous says:
>
> How do you update the plugin to use this toolbar functionality ?


### **Anonymous** — January 5, 2015 at 4:11 am ([permalink](/blog/toolbar/#comment-21687))

> Anonymous says:
>
> We will make an update in a week or two.


### **Anonymous** — February 21, 2015 at 1:42 pm ([permalink](/blog/toolbar/#comment-21610))

> Anonymous says:
>
> i get below error when using this command: 
>
> mainForm.setTitleComponent(titleCmb); 
>
> The method setTitleComponent(Label) in the type Form is not applicable for the arguments (ComboBox)


### **Anonymous** — February 22, 2015 at 2:20 am ([permalink](/blog/toolbar/#comment-21920))

> Anonymous says:
>
> Use the method in the Toolbar not in the form.


### **james agada** — March 24, 2015 at 9:51 pm ([permalink](/blog/toolbar/#comment-21988))

> james agada says:
>
> ToolBar only adds to forms right? not to containers.


### **Shai Almog** — March 25, 2015 at 5:06 am ([permalink](/blog/toolbar/#comment-22237))

> Shai Almog says:
>
> Yes.


### **Chidiebere Okwudire** — January 25, 2016 at 9:15 am ([permalink](/blog/toolbar/#comment-22508))

> Chidiebere Okwudire says:
>
> Hi,
>
> On iOS, the toolbar looks pretty native and comparable to the NavigationBar but on Android, the toolbar looks anything but native when compared to the Android Toolbar material design (see section ‘App bar’ here [[https://www.google.com/desi…](<https://www.google.com/design/spec/layout/structure.html#structure-toolbars>)]).
>
> Why doesn’t the Android implementation by default comply with these guidelines (height, nav icon size and position, background, etc.)? It’s annoying to have to tweak the toolbar for every single app just to get the default-like look on Android whereas it just works on iOS. Can you fix this issue or am I missing something?


### **Shai Almog** — January 25, 2016 at 12:09 pm ([permalink](/blog/toolbar/#comment-22534))

> Shai Almog says:
>
> I agree we should do it. Generally customizing the toolbar without breaking existing code was pretty hard but for a newly created app the toolbar should come preconfigured and easy to use.
>
> Matching material design on Android is our #1 priority based on our recent roadmap (together with performance) so this is something we want to do and we do want things to be far more refined than they are right now. Can you file an issue with suggestions e.g. what sort of tweeks you usually do to a project to get it to match?


### **Chidiebere Okwudire** — January 25, 2016 at 2:28 pm ([permalink](/blog/toolbar/#comment-22678))

> Chidiebere Okwudire says:
>
> Hi,
>
> I’m still busy with the app 😉 I’ll file an issue after I’m done with the toolbar customization. That will be next week at the earliest.


### **Shai Almog** — January 25, 2016 at 6:20 pm ([permalink](/blog/toolbar/#comment-22690))

> Shai Almog says:
>
> Hi,  
> no rush whatsoever. Thanks.


### **Gareth Murfin** — January 15, 2019 at 9:39 pm ([permalink](/blog/toolbar/#comment-23884))

> Gareth Murfin says:
>
> i dont understand how the search bar was added to the toolbar, for me nothing appears


### **Shai Almog** — January 16, 2019 at 4:45 am ([permalink](/blog/toolbar/#comment-24031))

> Shai Almog says:
>
> This is from 2014, I suggest looking at the Javadocs where there are two separate code samples for search

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
