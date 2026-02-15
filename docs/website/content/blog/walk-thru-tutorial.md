---
title: Walk-thru Tutorial
slug: walk-thru-tutorial
url: /blog/walk-thru-tutorial/
original_url: https://www.codenameone.com/blog/walk-thru-tutorial.html
aliases:
- /blog/walk-thru-tutorial.html
date: '2013-06-09'
author: Shai Almog
---

![Header Image](/blog/walk-thru-tutorial/hqdefault.jpg)

  

Codename One Maker is a pretty elaborate app, there is only so far we can go with simplifying the app itself. 

  
So we added a tutorial mode which is common in mobile/web apps  
  
, to walk the user through the process of creating a simple application and using the GUI builder (we also added a  
[  
Udemy course  
](https://www.udemy.com/cn1maker)  
but I digress). This feature is probably useful for almost every app out there, so here is how you can achieve that with Codename One…

  
There are generally two approaches for laying an overlay:  
  

  1.   
Glass pane – this is how we used to do things in the old days, its powerful but has many limitations. You essentially need to draw everything using graphics.  
  
A glass pane is just a “layer” on top of all components that you can draw on. It might seem that you can “emulate” the glass pane by overriding the paint method in Form and that would indeed work for simple use cases however the glass pane is “clever” and knows how to repaint itself when a component is updated (e.g. if a ticker is running the whole form won’t repaint so glass pane will work but overriding Form paint would not!). 

  2.   
Layered layout – this is a more “modern” approach we take where we essentially place two containers in a LayeredLayout and the last one added remains on top.  
  

Generally the glass pane is not interactive (it just draws) where the layered layout can actually grab input etc. we made use of the layered layout in a previous post (  
[  
when a dialog is no a dialog  
](http:///3/post/2013/06/when-a-dialog-isnt-a-dialog.html)  
). In this case though, I chose to use a glass pane mostly because I was too lazy to go back to every form and add a layered layout to the hierarchy

  
  
  
  

You will notice several interesting things about the tutorial mode. We added an option to “swipe” out of the tutorial mode at any time (this is the bottom portion starting at line 47).  
  
  
We draw everything manually in the glass pane (that’s how it works).  

  
You will notice that some sections accept null as the highlight component, this can happen when we use the title as the highlight component on Android 4.x.  
  
  
  
Newer Android devices use the native action bar for the title and so will return null when we query for the title area.  

  
We hope this is useful for you when building your apps.  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — July 23, 2014 at 8:21 am ([permalink](/blog/walk-thru-tutorial/#comment-22068))

> Anonymous says:
>
> I used this code snippet with a highlightComponent = null, and the glasspane with the text appears as it should – it’s a very coll feature. I can’t swipe out though. I’m not sure I understand the swipe logic. Can you explain how it works ?
>



### **Anonymous** — July 23, 2014 at 12:25 pm ([permalink](/blog/walk-thru-tutorial/#comment-22165))

> Anonymous says:
>
> We add a pointer dragged listener to the parent form as you see at the bottom of the source. If a user swipes a 5th of the screen from left to right the tutorial should end. 
>
> Are you getting the pointer events? Should be pretty easy to debug if this is causing issues in the simulator.
>



### **Anonymous** — July 23, 2014 at 6:14 pm ([permalink](/blog/walk-thru-tutorial/#comment-22051))

> Anonymous says:
>
> I’ve got a side menu on the form and swiping left to right exposes the side menu form, so I guess it’s the order in which pointer dragged events are consumed.
>



### **Anonymous** — July 24, 2014 at 3:29 am ([permalink](/blog/walk-thru-tutorial/#comment-21899))

> Anonymous says:
>
> Yes, this predated the sidemenu by a while. You can just edit that logic to work with a different gesture or a tap on a specific screen location.
>



### **Anonymous** — July 24, 2014 at 3:53 am ([permalink](/blog/walk-thru-tutorial/#comment-21994))

> Anonymous says:
>
> OK – I changed the logic to swipe right to left and works well. Where would you normally call showTutorialOverlay() ? I have a call in postShow for the form but wonder if that’s what you had in mind as a use case.
>



### **Anonymous** — July 24, 2014 at 2:12 pm ([permalink](/blog/walk-thru-tutorial/#comment-22115))

> Anonymous says:
>
> Post show should work out nicely.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
