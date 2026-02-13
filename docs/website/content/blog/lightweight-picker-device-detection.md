---
title: Lightweight Picker, Device Detection and More
slug: lightweight-picker-device-detection
url: /blog/lightweight-picker-device-detection/
original_url: https://www.codenameone.com/blog/lightweight-picker-device-detection.html
aliases:
- /blog/lightweight-picker-device-detection.html
date: '2018-05-21'
author: Shai Almog
---

![Header Image](/blog/lightweight-picker-device-detection/pixel-perfect.jpg)

One of the worst components in Codename One is the picker component. It’s origin lies in the migration to iOS 7 where the native picker introduced a 3d effect that was hard to replicate with our old graphics layer. We had no choice. We used a native widget for that picker and regretted that decision ever since.

It looks bad on the simulator, it misbehaves and with every update from Apple things break. This has again proven to us the importance of the lightweight architecture of Codename One!

Since the introduction of the picker our graphics layer was heavily revised enough to support something as elaborate as the iOS picker UI. With that Steve spent a lot of time doing a from scratch implementation of the iOS picker UI which you can try right now.

![New Picker on the Simulator](/blog/lightweight-picker-device-detection/lightweight-picker.png)

Figure 1. New Picker on the Simulator

The new implementation is on right now!

If the platform supports native pickers it will use the native picker UI. If it doesn’t it will default to the lightweight UI.

You can force the lightweight mode by invoking: `picker.setLightweightMode(true);`.

Since picker is native by default the UI changes in iOS/Android where we get the native Android picker interface. Right now we only replicated the iOS UI, ideally we’d do the same for the Android picker although the urgency is much lower as it’s surprisingly more robust than the iOS picker API.

As part of this implementation Steve introduced some API’s such as a scene-graph API which is currently marked as deprecated as it’s still under development. If there is some interest in it we might explore some of those capabilities in the future.

### Device Detection

On a different note…​ We hid most of the device detection functionality in Codename One to prevent the situation where developers write code such as:
    
    
    if(deviceX) {
    } else if(device Y) {
    }
    //....

This starts as a quick bandaid to a problem & snowballs into something that’s unmaintainable.

Having said that there are some valid use cases for device detection such as statistics & analysis. There are some edge cases that can only be solved in this way e.g. on iOS it’s often impossible to calculate the accurate device DPI without detection code.

With that in mind Diamond introduced [this cn1lib](https://github.com/diamonddevgroup/CN1-Device/) which is available in the extension manager. It includes native device detection code that would you to detect the device. Hopefully this will be used for good.

### Facebook Clone Update

I’m past the 50% of the content mark (roughly at 65-70% by my estimate) but that doesn’t leave me that much time. Currently I have 25 lessons nearly ready and 17 out of them are already published. I estimate this will take another 25 lessons to complete since I still have a lot of material that’s prepared but not segmented. For comparison the Uber clone module took 40 lessons to complete.

To be fair, these lessons are shorter as I get to the point faster and I improved the material presentation significantly. Either way I’ll need to pick up the pace if I hope to release this module before the end of the month.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Stefan Eder** — May 23, 2018 at 10:27 am ([permalink](https://www.codenameone.com/blog/lightweight-picker-device-detection.html#comment-23619))

> Would it be possible now to have a UI like in the IOS calendar app where the pickers slide in between the other controls – thus not open a dialog to choose but stay on the same form?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flightweight-picker-device-detection.html)


### **Stefan Eder** — May 23, 2018 at 11:13 am ([permalink](https://www.codenameone.com/blog/lightweight-picker-device-detection.html#comment-23683))

> Stefan Eder says:
>
> Apparently even if the locale is german and the date is displayed in the german format befor editing – in the picker UI in the simulator the date format is the US date format and the buttons are english.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flightweight-picker-device-detection.html)


### **Shai Almog** — May 24, 2018 at 4:05 am ([permalink](https://www.codenameone.com/blog/lightweight-picker-device-detection.html#comment-23946))

> Shai Almog says:
>
> Can you please file an issue on this?  
> Since this is a lightweight component we’ll localize it with the rest of the localization logic. All of this should be declared in the javadoc to the class to make it easier to do.  
> It should be possible to embed this but right now it’s not a priority so we hid/deprecated those internal API’s as they are still not mature enough for use. Notice that there are small nuances with embedding e.g. if you have a text field followed by a picker you’d expect the “next” cycle to include the picker (that currently doesn’t work in the picker) but it would be hard to do with embedding.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flightweight-picker-device-detection.html)


### **Francesco Galgani** — May 25, 2018 at 3:36 pm ([permalink](https://www.codenameone.com/blog/lightweight-picker-device-detection.html#comment-21636))

> Francesco Galgani says:
>
> The Diamond’s CN1Lib to detect the device works perfectly! Thanks! 😀
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Flightweight-picker-device-detection.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
