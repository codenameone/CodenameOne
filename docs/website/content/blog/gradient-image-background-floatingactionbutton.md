---
title: Gradient and Image Background on FloatingActionButton
slug: gradient-image-background-floatingactionbutton
url: /blog/gradient-image-background-floatingactionbutton/
original_url: https://www.codenameone.com/blog/gradient-image-background-floatingactionbutton.html
aliases:
- /blog/gradient-image-background-floatingactionbutton.html
date: '2016-11-01'
author: Shai Almog
---

![Header Image](/blog/gradient-image-background-floatingactionbutton/gradients-image-background-floatingactionbutton.jpg)

The Phoenix theme had a [FloatingActionButton](/blog/floating-button.html) with a gradient on top. This goes against the mostly flat material  
design spec but after looking at the design with a solid color I came to the conclusion that the designer was totally  
right to use a gradient in this case. Unfortunately we didn’t build that support into the `FloatingActionButton`.

So as part of that work I added a special mode to the `RoundBorder` that uses the parent UIID to draw the background.  
This sounds like a “no brainer” but there is a catch: the round border needs to be “round”.

Gradient and image primitives don’t draw within a round shape, so the only solution was  
[shape clipping](/blog/shape-clipping-bubble-transition.html) which has a performance penalty as well  
the associated problem of not working everywhere…​

So right now this is off by default as the behavior doesn’t work exactly the same on the JavaScript or Windows ports  
where shape clipping is still missing.

To enable this we need to change the border of the component by making it use the UIID. We need to do that  
from code using the `RoundBorder.uiid(boolean)` method. We can do this using:
    
    
    RoundBorder rb = (RoundBorder)fab.getUnselectedStyle().getBorder();
    rb.uiid(true);

If the background style of the round border includes a gradient or an image you will see the effects at once.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
