---
title: Codename One Simulator Facelift
slug: codename-one-simulator-facelift
url: /blog/codename-one-simulator-facelift/
original_url: https://www.codenameone.com/blog/codename-one-simulator-facelift.html
aliases:
- /blog/codename-one-simulator-facelift.html
date: '2022-03-18'
author: Steve Hannah
description: With the latest Maven update (7.0.61), you will notice a few changes
  to the Codename One simulator that should improve your development experience.
---

With the latest Maven update (7.0.61), you will notice a few changes to the Codename One simulator that should improve your development experience.

![Simulator Facelift - Codename One](/blog/codename-one-simulator-facelift/Simulator-Facelift-Codename-One-1024x536.jpg)

We have consolidated the user interface inside a single window so that the component inspector, network monitor, and other tools feel more integrated.

The window has four regions (Left, Center, Right, and Bottom), and you are free to place any of the UI panels into any of these regions. The default configuration places the simulator in the center, the component tree on the left, and the component details and network monitor in the bottom, but you can change this using the `"Move To"` menu, and it will remember your preferences the next time you run the simulator.

![](/blog/codename-one-simulator-facelift/default-configuration.png) 

Figure 1. The default simulator window configuration, with component tree on the left, the simulator in the center, and the component details on the bottom.

For example, if you wanted the component tree to appear in the **“right”** region, you could click on the panel menu in the upper right of the `Components` panel.

![](/blog/codename-one-simulator-facelift/move-to-right.png)

And the resulting configuration would look like the following:

![](/blog/codename-one-simulator-facelift/comonents-on-right.png)

If you prefer to break out a panel into a separate window, you can do that also. When you close the window, it will automatically be returned to its original region inside the main simulator window.

For example, if we wanted to move the simulator itself to a separate window, we could select the panel menu in the uper right of the **Simulator** panel, and select `"Move To" > "New Window"`

![](/blog/codename-one-simulator-facelift/move-to-separate-window.png)

The result would be the simulator in its own window as shown below:

![](/blog/codename-one-simulator-facelift/simulator-in-separate-window.png)

Your configuration will be remembered for your next run, so you wan’t have to repeat yourself.

### Menu Changes

As part of this change, we have moved some of the items from the **Simulator** menu onto a new toolbar in the **Simulator** panel. For example instead of a `"Simulator" > "Rotate"` menu item, there are toggle buttons on the toolbar for `"Portrait" and "Landscape".` And instead of `"Simulator" > "Zoom",` there are `Zoom In` and `Zoom Out` buttons on the toolbar.

![](/blog/codename-one-simulator-facelift/simulator-toolbar.png)

### More to come…​

Over the next few updates we will be doing some more reorganization of menu items and panels to make the experience more cohesive. We will also be adding a few new features to enhance the dev experience. Stay tuned…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Diamond Mubaarak** — March 22, 2022 at 6:22 am ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24514))

> Diamond Mubaarak says:
>
> Consolidating the simulating tools is excellent. I know this is a work in progress but some functionalities are off or could be improved.
>
> – Network monitor has no records.  
> – Components are not selectable on UI with MapContainer. The tree could be expanded, though. Only the root form is selectable.  
> – Selection of a component in the Components pane should switch to the Component Details tab automatically.  
> – Rotating a device should automatically increase and decrease in size the Simulator pane to show the whole device.  
> – “Always on Top” functionality should apply to secondary windows like “Push Simulation” or when I set the Simulator pane to show as a new window, as these windows are hidden behind the main window until it’s unchecked.  
> – The “Move To” overflow menu appears behind the Simulator until you hover on it.  
> – Zooming in or out is great, however, zoom in should come with the ability to pan the screen.
>
> I’m running the new Simulator on a Linux OS.
>



### **Steve Hannah** — March 22, 2022 at 6:59 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24515))

> Steve Hannah says:
>
> Thanks for the feedback. I have moved this into an issue and am working through the points one by one. You can follow the progress at <https://github.com/codenameone/CodenameOne/issues/3566>
>



### **Steve Hannah** — March 28, 2022 at 1:46 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24524))

> Steve Hannah says:
>
> All of these issues are resolved in the latest version (7.0.62). You can follow the specific changes in the issue tracker issue that I opened for this at <https://github.com/codenameone/CodenameOne/issues/3566>
>



### **plumberg** — March 28, 2022 at 8:23 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24525))

> plumberg says:
>
> Hi,
>
> I tried opening Network Monitor after last update (just updated cn1 few min ago), but Network monitor window is not opening. Nothing is happening
>
> I’ll appreciate your help!
>



### **plumberg** — March 28, 2022 at 8:27 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24526))

> plumberg says:
>
> Nevermind, I just found it in the same window on the bottom.
>



### **Steve Hannah** — March 28, 2022 at 11:06 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24527))

> Steve Hannah says:
>
> Thanks for sharing that you had trouble find it. I’ll see what I can do to make this more intuitive.


### **Abdelaziz Makhlouf** — March 25, 2022 at 1:00 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24516))

> Abdelaziz Makhlouf says:
>
> Hello,  
> Due to this update, all components are very small now : <https://prnt.sc/YEkeZp_XWEdP>  
> How to fix this please or how to get back to the older version ? We can’t even read the text.
>



### **Steve Hannah** — March 25, 2022 at 2:16 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24517))

> Steve Hannah says:
>
> The changes to the simulator shouldn’t have any effect on component sizes in your app. One way to confirm this is to revert the version to 7.0.58. If you change the cn1.version property to 7.0.58 in your project’s pom.xml file, does that “fix” the issue?
>



### **Fawaz Qamhawi** — March 26, 2022 at 8:39 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24518))

> Fawaz Qamhawi says:
>
> This is happening with me.  
> If I will need to save the css file again after each run, it will compile again. That will fix the problem.
>
> By the way, I lost all my skins and there is no way to upload other skins as it used to be before this update.
>



### **Steve Hannah** — March 27, 2022 at 12:37 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24519))

> Steve Hannah says:
>
> > By the way, I lost all my skins and there is no way to upload other skins as it used to be before this update.
>
> You can still add skins in exactly the same way that you added skins before. “Skins” > “More”. or “Skins” > “Add New”. This functionality is unchanged.
>
> > If I will need to save the css file again after each run, it will compile again. That will fix the problem.
>
> I’m not sure I understand the issue? Are you saying that your problem with styles is fixed after you recompile the CSS file?
>



### **Steve Hannah** — March 28, 2022 at 1:44 pm ([permalink](https://www.codenameone.com/blog/codename-one-simulator-facelift.html#comment-24523))

> Steve Hannah says:
>
> I have opened an issue in the issue tracker at <https://github.com/codenameone/CodenameOne/issues/3572>  
> If you experience this issue, please update that issue with details to help reproduce it.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
