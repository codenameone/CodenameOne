---
title: When A Dialog Isn't A Dialog
slug: when-a-dialog-isnt-a-dialog
url: /blog/when-a-dialog-isnt-a-dialog/
original_url: https://www.codenameone.com/blog/when-a-dialog-isnt-a-dialog.html
aliases:
- /blog/when-a-dialog-isnt-a-dialog.html
date: '2013-06-02'
author: Shai Almog
---

![Header Image](/blog/when-a-dialog-isnt-a-dialog/when-a-dialog-isnt-a-dialog-1.png)

  

  
  
  
[  
![Fake dialog with spinner](/blog/when-a-dialog-isnt-a-dialog/when-a-dialog-isnt-a-dialog-1.png)  
](/img/blog/old_posts/when-a-dialog-isnt-a-dialog-large-2.png)  
  
  

One of the things we’ve been working on with Maker is getting the new GUI builder to support hierarchies/layouts which should be landing tomorrow or so. When we initially built the GUI builder within the Codename One designer we made many mistakes but one of the big ones was with layouts, it takes too long to see how a layout affects something (you need to physically accept a dialog/rinse repeat) so people just don’t experiment enough with the options. 

  
In Maker we intend to fix that, by providing  
  
the UI you see in the video and the image. Essentially when you spin the spinner the layout changes automatically and animates the components into place giving a clear indication of the change between positions (if they “jump” its harder to notice differences in some cases).

  
The problem is that a “normal” dialog doesn’t allow for this UI, it doesn’t allow an “always on top” window and it doesn’t really allow the underlying form to “animate”. If you will study the code the reasoning for that will become crystal clear: a dialog is really a Form that takes up the entire screen and just “draws” the previous form behind. Since the previous form is “deinitialize” it just won’t run most of the animations (to preserve CPU). This was done ages ago with a very specific set of use cases in mind and its generally very efficient but we just can’t implement something like this using this approach.  

  
  
So how did we implement this feature?

  
We used LayeredLayout.  
  
  
  
  
  
  
The LayeredLayout allows placing components one on top of the other. So we created a LayeredLayout and when time came to show the fake dialog we just did something similar to this:  
  

* * *

As you can see the “Dialog” is just a container with the right UIID and some additional components faking the title and content pane.  
  
  
We place it in the correct location by using a layout  
  
manager and we can animate it into place (and out) by using layout animations or replace (for disposing the dialog).  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 8, 2013 at 11:17 am ([permalink](/blog/when-a-dialog-isnt-a-dialog/#comment-21705))

> Anonymous says:
>
> Great job. have been waiting for something like this. 
>
> Thanks.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
