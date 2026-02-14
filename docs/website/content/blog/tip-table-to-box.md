---
title: 'TIP: Table to Box'
slug: tip-table-to-box
url: /blog/tip-table-to-box/
original_url: https://www.codenameone.com/blog/tip-table-to-box.html
aliases:
- /blog/tip-table-to-box.html
date: '2016-12-18'
author: Shai Almog
---

![Header Image](/blog/tip-table-to-box/just-the-tip.jpg)

One of the most painful aspects in any mobile app is input, besides the difficulty of viewing the details on a  
tiny cramped screen the input via the virtual keyboard is nowhere near the input comfort of a full fledged computer  
or even a tablet.

A great app adapts to the screen size and uses the available space more effectively, we tried to do this in the kitchen  
sink demo where user input appears like this in portrait:

![The kitchen sink input demo in portrait mode](/blog/tip-table-to-box/kitchen-sink-input-portrait.png)

Figure 1. The kitchen sink input demo in portrait mode

![The kitchen sink input demo in landscape  mode](/blog/tip-table-to-box/kitchen-sink-input-landscape.png)

Figure 2. The kitchen sink input demo in landscape mode

You will notice that the demo uses table layout when in landscape (or running on a tablet) and a box layout  
on the `Y_AXIS` when running in a phone in portrait to use the space better. One of the things that isn’t obvious  
from the screenshots above is the nice animation we get when rotating the device.

The code that makes this possible is really simple and should be easily adaptable to any input form you might  
have:
    
    
    private void addComps(Form parent, Container cnt, Component... cmps) { __**(1)**
        if(Display.getInstance().isTablet() || !Display.getInstance().isPortrait()) { __**(2)**
            TableLayout tl = new TableLayout(cmps.length / 2, 2);
            cnt.setLayout(tl);
            tl.setGrowHorizontally(true);
            for(Component c : cmps) {
                if(c instanceof Container) { __**(3)**
                    cnt.add(tl.createConstraint().horizontalSpan(2), c);
                } else {
                    cnt.add(c);
                }
            }
        } else {
            cnt.setLayout(BoxLayout.y());
            for(Component c : cmps) {
                cnt.add(c);
            }
        }
        if(cnt.getClientProperty("bound") == null) { __**(4)**
            cnt.putClientProperty("bound", "true");
            if(!Display.getInstance().isTablet()) {
                parent.addOrientationListener((e) -> {
                    Display.getInstance().callSerially(() -> {
                        cnt.removeAll(); __**(5)**
                        addComps(parent, cnt, cmps);
                        cnt.animateLayout(800);
                    });
                });
            }
        }
    }
    
    addComps(parent, comps,
            new Label("Name", "InputContainerLabel"),
            name,
            new Label("E-Mail", "InputContainerLabel"),
            email,
            new Label("Password", "InputContainerLabel"),
            password,
            BorderLayout.center(new Label("Birthday", "InputContainerLabel")).
                    add(BorderLayout.EAST, birthday),
            new Label("Bio", "InputContainerLabel"),
            bio,
            BorderLayout.center(new Label("Join Mailing List", "InputContainerLabel")).
                    add(BorderLayout.EAST, joinMailingList));

__**1** | The `addComps` method just adds all the components to a dynamically changing input. You can  
use this method almost “as is” to get the effect above  
---|---  
__**2** | In tablets/desktops we always use the table mode so the box layout only applies in portrait phones and  
nowhere else  
__**3** | We have a special case for container rows where we might have a custom UI element, in this case we  
always make it span the whole row even in a table layout mode  
__**4** | Without these lines the listener code would be bound twice and the layouts would blink  
__**5** | The animation logic just recurses the method so the elements are re-added with the right constraint then animated.  
Since the elements already have the right position the layout animation will produce that great rotation effect  
  
### Final Word

What makes an app delightful is the attention to detail, small changes like this make the UX of an app  
and it’s esthetic far more appealing.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — December 19, 2016 at 2:39 pm ([permalink](https://www.codenameone.com/blog/tip-table-to-box.html#comment-24244))

> Excellent functionality, I look forward to using this in my next apps, I am ready to swap to new gui editor now too I think :-)))) Keep up the good work.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
