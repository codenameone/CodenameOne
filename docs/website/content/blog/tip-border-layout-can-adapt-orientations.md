---
title: 'TIP: Border Layout can Adapt to Orientation'
slug: tip-border-layout-can-adapt-orientations
url: /blog/tip-border-layout-can-adapt-orientations/
original_url: https://www.codenameone.com/blog/tip-border-layout-can-adapt-orientations.html
aliases:
- /blog/tip-border-layout-can-adapt-orientations.html
date: '2016-10-30'
author: Shai Almog
---

![Header Image](/blog/tip-border-layout-can-adapt-orientations/just-the-tip.jpg)

Some designs don’t work well in landscape/portrait mode and we need to adapt them to fit the orientation of  
the device e.g. when we have a large graphic element (icon etc.) on top/below and we no longer have room  
for that element.

As an example check out the code below:
    
    
    Form hi = new Form("Swap", new BorderLayout());
    
    hi.add(BorderLayout.NORTH,
            FlowLayout.encloseCenter(
                    new Label(duke.scaledWidth(Display.getInstance().convertToPixels(40)))));
    
    hi.add(BorderLayout.CENTER,
            ComponentGroup.enclose(
                    new TextField("", "Username"),
                    new TextField("", "Password", 20, TextField.PASSWORD)
            ));
    
    hi.show();

It produces this image in portrait which matches our expectations:

![Portrait mode border layout image](/blog/tip-border-layout-can-adapt-orientations/tip-border-layout-can-adapt-orientations1.png)

Figure 1. Portrait mode border layout image

When we rotate the device though, things go south…​ Literally:

![In landscape mode the image hides the actual content of the UI](/blog/tip-border-layout-can-adapt-orientations/tip-border-layout-can-adapt-orientations2.png)

Figure 2. In landscape mode the image hides the actual content of the UI

The thing is that there is plenty of space in landscape it is just distributed differently, so if the image was on the east  
instead of the north position it would “just work”. That’s exactly what swap border position does. By using this  
code the component in the north will automatically shift positions when the device is in landscape mode:
    
    
    BorderLayout bl = new BorderLayout();
    Form hi = new Form("Swap", bl);
    bl.defineLandscapeSwap(BorderLayout.NORTH, BorderLayout.EAST);

![Automatic position swapping with the defineLandscapeSwap method](/blog/tip-border-layout-can-adapt-orientations/tip-border-layout-can-adapt-orientations3.png)

Figure 3. Automatic position swapping with the defineLandscapeSwap method

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
