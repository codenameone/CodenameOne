---
title: Scrollbars & Tooltips
slug: scrollbars-tooltips
url: /blog/scrollbars-tooltips/
original_url: https://www.codenameone.com/blog/scrollbars-tooltips.html
aliases:
- /blog/scrollbars-tooltips.html
date: '2016-07-18'
author: Shai Almog
---

![Header Image](/blog/scrollbars-tooltips/scrollbar.png)

One of the big decisions we made a while back was to build the new GUI builder on top of Codename One itself,  
we extended that decision three months ago with the decision to build the Codename One settings in Codename One  
and then deciding to make it the default preferences UI for all IDE’s…​

Those were great decisions in retrospect, they helped us consolidate code across the different IDE’s.  
Furthermore using Codename One is far simpler than Swing/SWT or FX. At least for us…​

The look of these new UI’s is far more modern than the Swing alternative. Unlike these other API’s we  
designed Codename One for mobile and not for desktop so basic desktop staples like scrollbars, tooltips, menubars etc.  
weren’t available.

We worked around the menu bar functionality using native code and we added scrolling based on the logic  
[here](/blog/permanent-sidemenu-getAllStyles-scrollbar-and-more.html) (this isn’t yet in the current shipping version).

We made slight improvements to that code that also recalculate the height and allow for arrows. One of the improvements  
that did make the current version is support for tooltips which are convenient for desktop applications.  
We added some initial work around tooltips based on the `pointerHover` callbacks which now have a mapping  
in desktop apps but this isn’t turned on by default. We chose to use the glasspane for the tooltips instead of the  
layered pane which might have an issue when displaying a tooltip on the toolbar.

Since we are pretty late in the 3.5 release cycle we chose not to expose any of this in the API. Our  
current line of thinking is to create an overarching `com.codename1.ui.desktop` package that will include  
tools for desktop API’s such as the ability to define a menu bar, tooltips, scrollbars and other desktop oriented  
capabilities.

If this is something you find interesting please let us know in the comments, we’d like feedback about the type  
of features/functionality you would expect in such an API.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ulises Escobar** — July 19, 2016 at 5:13 pm ([permalink](/blog/scrollbars-tooltips/#comment-22710))

> Ulises Escobar says:
>
> I wish I could move from one text field to another with Tab
>



### **Shai Almog** — July 20, 2016 at 4:27 am ([permalink](/blog/scrollbars-tooltips/#comment-22741))

> Shai Almog says:
>
> This should already work in the current versions and even in the simulator.
>



### **beck** — July 20, 2016 at 6:47 am ([permalink](/blog/scrollbars-tooltips/#comment-22653))

> beck says:
>
> I dont know it is related or not but It’d be really really cool if we can see the component highlighted by border or smth when it is selected in component inspector. Sometime it takes much time to search the component in component inspector
>



### **Shai Almog** — July 21, 2016 at 3:49 am ([permalink](/blog/scrollbars-tooltips/#comment-21516))

> Shai Almog says:
>
> Probably not related but yes I’d like that too. But we’re not doing this for the current code as it’s a bit limited.
>
> We have some very interesting ideas moving forward with the simulator and the component inspector.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
