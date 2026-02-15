---
title: Release Plan For 3.2 & New Approach To Demos
slug: release-plan-for-3-2-new-approach-to-demos
url: /blog/release-plan-for-3-2-new-approach-to-demos/
original_url: https://www.codenameone.com/blog/release-plan-for-3-2-new-approach-to-demos.html
aliases:
- /blog/release-plan-for-3-2-new-approach-to-demos.html
date: '2015-10-07'
author: Shai Almog
---

![Header Image](/blog/release-plan-for-3-2-new-approach-to-demos/solitaire.png)

Codename One 3.2 is scheduled for Tuesday the 27th of October. In keeping with our successful 3.1 release we’ll  
use a very short one week code freeze on the 20th of October at which point we will only commit crucial fixes  
with code review. I hope we can land quite a few new features for the release, the GUI builder is getting  
very close although its still a very rough product and will only be featured as a “technology preview” showing  
the direction we are heading rather than a final product. 

#### New Approach To Demos

Up until now we always thought about demos as developers often do: “As a tool to show how to program using  
a set of features in the tool”.  
Recently, after working on my spouses Yoga Studio management app I came to the epiphany that this isn’t  
the best way for mobile app demos. The main problem is that we can’t upload these demos to app stores  
and show them running on a device. So with that in mind we are rethinking some future demos starting with  
a quick and dirty [Solitaire Klondike demo](/solitaire-klondike/) that we made within a weekend  
and submitted to the store. 

Check out the demo page where you can see the full code of what is now a production app that you can download  
via itunes and google play.  
We have a couple of additional apps that we need to cleanup and submit including the Yoga Studio app  
that demonstrates a very common use case of a database driven app. We’ll try to clean that up and prepare  
it for mass consumption/store submission. 

#### In Other News

The performance enhancement for image tiling from Fabricio that we discussed last week finally landed after  
a lot of hard work debugging it by Fabricio and Steve. You should notice 9-piece images performing better  
on both iOS & Android.  
We are moving to make Java 8 builds into the default build mode, we are pretty thrilled by the Java 8 support  
so far and want more newbies to be exposed to it. You would still have the checkbox for Java 8 mode when creating  
a new project (if you are running on JDK 8 or newer!) but now it will be checked by default.  
We also added `get/setGlobalResources` methods to the `Resources` class.  
This really simplifies working with the new GUI builder since new GUI builder files will no longer be a part of  
a resource file you would need to pass them a resources object somehow. Initially we thought about passing  
it in the constructor but this seems tedious, with the global resources this should be much simpler.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ross Taylor** — October 27, 2015 at 11:12 am ([permalink](https://www.codenameone.com/blog/release-plan-for-3-2-new-approach-to-demos.html#comment-22463))

> Ross Taylor says:
>
> Hey I tried your solitaire app. However I have some critiques and wonder if its anything related to codenameone. The first is the card images are grainy (medium – low quality). I suppose you did this to save app size? The second is there is a line separation in the menu command after the “About”, where I think it shouldn’t be since it makes the menu command separation look untidy (looks like something broke off suddenly). The third is the background blurs (or refreshes?) when the menu command and the About dialog is disposed. The last is that when the button of the dialog is pressed in the pressed state, the highlighted area of the button way exceeds the area of the actual button. Other than these UI blips, the app seems to be pretty fast.
>



### **Shai Almog** — October 28, 2015 at 3:29 am ([permalink](https://www.codenameone.com/blog/release-plan-for-3-2-new-approach-to-demos.html#comment-22414))

> Shai Almog says:
>
> Hi. Its mostly related to my lazyness when building the app. I also neglected to customize the overflow menu icon properly and its sizing/color differ from the color of the other icons.  
> Specifically the overflow feature of the toolbar needs some refinement in the default theme, both its in/out transition and look need some work in the default theme. Menus are remarkably customizable in Codename One and this type of menu is pretty new so we didn’t get much feedback for it, I’ve passed your comments to the guy who did it and he agrees we should fix that by default.
>
> The larger button than the dialog is something I just noticed for the first time, it seems that just the edges of the button exceed. Haven’t noticed it because I normally press and release. This is a property of the Android theme and isn’t indicative of anything inherent. We do need to improve that behavior though.
>



### **Joe** — November 2, 2015 at 1:06 pm ([permalink](https://www.codenameone.com/blog/release-plan-for-3-2-new-approach-to-demos.html#comment-22129))

> Joe says:
>
> I cloned the demo on my local. Looks like the project files are for NetBean Do you have the instruction of how to import the projects into Eclipse?
>



### **Shai Almog** — November 3, 2015 at 4:29 am ([permalink](https://www.codenameone.com/blog/release-plan-for-3-2-new-approach-to-demos.html#comment-22512))

> Shai Almog says:
>
> Generally for every imported NetBeans project just create a new project in eclipse using the same main class and package.  
> Copy the src dir and libs dir on top of their Eclipse counterparts.  
> Copy the [codenameone_settings.proper…](<http://codenameone_settings.properties>) on top of the existing one and the icon.png.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
