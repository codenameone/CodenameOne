---
title: Desktop Skin
slug: desktop-skin
url: /blog/desktop-skin/
original_url: https://www.codenameone.com/blog/desktop-skin.html
aliases:
- /blog/desktop-skin.html
date: '2017-07-03'
author: Shai Almog
---

![Header Image](/blog/desktop-skin/new-features-4.jpg)

During the final stages of the 3.7 release cycle we had a lot of material to go thru and Chen slipped a couple of features that we just didn’t have the time to discuss. One of those features is the new desktop skin which is a special case skin to debug desktop and JavaScript apps.

Unlike other skins it doesn’t scale, it resizes the UI when you resize it. It has no skin decorations except for a small bar at the bottom that indicates the current resolution. You can use this to simulate resizing from the user.

![Activate the desktop skin thru the skins menu](/blog/desktop-skin/desktop-skin-menu.png)

Figure 1. Activate the desktop skin thru the skins menu

![The desktop skin running the kitchen sink demo](/blog/desktop-skin/desktop-skin-running.png)

Figure 2. The desktop skin running the kitchen sink demo

### Desktop Features

We’ve been moving slowly to improve Codename Ones desktop support. Our own tools are already mostly written using Codename One (GUI builder, Settings).

We already feel that using Codename One in a desktop setting can produce a decent result in the right hands and we know a few people do exactly that. However, it’s hard for us to gauge expectations and desires from customers without feedback. When we build our own apps we use a lot of tricks we never bother to publish as our use cases are usually edge cases.

If you are looking to build desktop and web apps using Codename One let us know and we can discuss features that would make it better suited for those use cases.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Carlos** — July 4, 2017 at 6:53 pm ([permalink](https://www.codenameone.com/blog/desktop-skin.html#comment-23359))

> Good move.
>
> Is it possible to programmatically set the size of the window at startup? That is something that I’m missing.
>



### **Shai Almog** — July 5, 2017 at 4:10 am ([permalink](https://www.codenameone.com/blog/desktop-skin.html#comment-23652))

> Thanks.  
> Not at this time, we have the desktop.width and desktop.height build hints but not programmatically.
>
> You can use a native interface and just fine the JFrame then resize that using Swing code.
>



### **Gareth Murfin** — February 14, 2018 at 9:03 am ([permalink](https://www.codenameone.com/blog/desktop-skin.html#comment-23723))

> Gareth Murfin says:
>
> I love the desktop skin, it would definitely be great if when you resized it then remembered the size for next time, this skin makes the emulator very usable I think, and would be far cooler if it would remember resize too.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
