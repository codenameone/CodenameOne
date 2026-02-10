---
title: Wheel & Drag
slug: wheel-drag
url: /blog/wheel-drag/
original_url: https://www.codenameone.com/blog/wheel-drag.html
aliases:
- /blog/wheel-drag.html
date: '2014-02-08'
author: Shai Almog
---

![Header Image](/blog/wheel-drag/wheel-drag-1.png)

  
  
  
  
![Picture](/blog/wheel-drag/wheel-drag-1.png)  
  
  
  

In the latest update to desktop builds we made the mouse wheel scroll the UI which is something that we’ve meant to do for ages but just didn’t get around to doing. This will also work in the simulator when the next update arrives.  
  
  
  
  
This was a big challenging to implement properly since the mouse doesn’t actually touch a specific component, eventually we decided to just send press/release/drag events in sequence when a mouse wheel is active.  
  
  
  
  
We also made some improvements to drag behavior. One of the surprisingly difficult aspects of touch development is recognizing whether a user wants to drag or tap. This is especially difficult with touch UI since a user might drag over a button in order to scroll down so the button shouldn’t fire an action event in that case!  
  
  
  
  
Its the main reason we should avoid binding logic to the pointer pressed method and instead use the pointer released method (there is another reason but I digress…). The problem is further compounded by the fact that some devices send drag events even when there was no perceivable drag. This makes it even harder to differentiate such a case.  
  
  
  
  
  
  
To workaround this behavior the Codename One implementation layer filters out drag events until we decide that a drag is “really” occurring and isn’t just “noise”. We filter using two different methods:  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
1\. The number of drag events sent – this is problematic on some platforms e.g. Android where a ridiculous number of drag events can be sent without the user moving his finger.  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
2\. Drag percentage – if the user moved his finger more than a given percentage of the screen drag should be activated.  
  
  
  
  
The problem is that if we set drag percentage too low buttons become harder to press since they notice small drag events that happen unintentionally and we assume you are trying to scroll the screen.  
  
  
  
  
Codename One has a setter within Display called setDragStartPercentage and some use cases feel smoother when setting it to a lower value however this is very device specific and error prone.  
  
  
  
  
So we added a new method to Component to indicate if a drag in a particular area is positively a drag operation: protected boolean isDragRegion(int x, int y)  
  
  
For Lists this will return true if the list is large making it easier to scroll larger lists, for side menu this will return true if the drag operation is in the left side of the screen trying to open the sidemenu.  
  
  
In those cases drag percentage is implicitly treated as very low (it still has an effect) to create a smoother dragging experience.  
  
  
  
  
Notice that we might change that method in the future to become more expressive.  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
