---
title: Version 4.1 and Launch Screen Storyboards
slug: version-4-1-launch-screen-storyboards
url: /blog/version-4-1-launch-screen-storyboards/
original_url: https://www.codenameone.com/blog/version-4-1-launch-screen-storyboards.html
aliases:
- /blog/version-4-1-launch-screen-storyboards.html
date: '2018-04-23'
author: Shai Almog
---

![Header Image](/blog/version-4-1-launch-screen-storyboards/codenameone-4-0-release-image-taxi.jpg)

This weekend we pushed out an update that also included new versions of the GUI builder and the designer. We didn’t update the plugins but we still think it warrants the 4.1 version moniker even though we don’t support it in versioned builds. Due to one of the enhancements we added in this update we had a regression in command behavior that we fixed with an update within a few hours.

This regression was caused by new features in the `Command` class that now has the ability to set the material icon. Unfortunately we neglected to add this into the equals method which cascaded into a hard to track issue in toolbar handling. This impacted people using the `addMaterialCommand` API’s which also included the GUI builder. The update framework allowed us to issue a quick update and resolve that issue.

### Launch Screen Storyboards

With the shift to Xcode 9, which is the default version on the Codename One build servers as of [February 2018](https://www.codenameone.com/blog/xcode-9-on-by-default.html), it is now possible to use a launch-screen storyboard as the splash screen instead of launch images. This will potentially solve the issue of the proliferation of screenshots, as you can supply a single storyboard which will work on all devices. Launch storyboards are disabled by default at this time, but we will flip the switch for 5.0 so they will be the default. You can enable the LaunchScreen storyboard by adding the `ios.multitasking=true` build hint and explicitly disable them by setting it to `false`.

The build hint is called “multitasking”, because iOS’ split-screen and multi-tasking feature **requires** that the app uses a launch storyboard rather than launch images.

Besides the advantage of multi-tasking support the launch screen storyboards have a few advantages:

  * It’s the official direction – Apple is clearly heading in this direction instead of splash screens so it’s more “future proof”

  * Faster builds – by now we need to generate 16 screenshots per build, this can slow down builds noticeably

  * Smaller binaries – some of these 16 screenshots can be very large

  * Native widgets & OS fidelity – the screenshot process has a lot of problems most noticeably it fails if you use a native widget in the home screen. These problems go away with this approach

#### Launch Storyboard vs Launch Images

A key benefit of using a launch storyboard right now is that it allows your app to be used in split-screen mode. Storyboards, however, work a little bit differently than launch images. They don’t show a screenshot of the first page of your app. The default Codename One launch storyboard simply shows your app’s icon in the middle of the screen. You can customize the launch screen by providing one or more of the following files in your project’s native/ios directory

  1. `Launch.Foreground.png` – Will be shown instead of your app’s icon in the center of the screen.

  2. `Launch.Background.png` – Will fill the background of the screen.

  3. `LaunchScreen.storyboard` – A custom storyboard developed in Xcode, that will be used instead of the default storyboard.

__ |  Make sure to add the `ios.multitasking=true` build hint, or your launch storyboard will not be used.   
---|---  
  
### Facebook Clone Status Update

It seems pretty clear I won’t finish the Facebook Clone work by the end of the month…​ My newer course modules have been far more thorough than my earlier work and take much longer as a result…​

The upshot of this is that it already looks pretty amazing, besides the app itself I’m very pleased with the materials I’ve produced so far and I think content creation will reach a new level.

Unlike the Uber Clone, I decided to use CSS for the Facebook Clone app. I still prefer working visually with the designer but explaining CSS in a presentation is simpler. For the designer I need to go over multiple UI screens and explain everything which is ultimately slower.

As part of that I asked Steve to add a few enhancements to the CSS support which is now noticeably faster. Hopefully this is something we can further improve so it will provide the same benefits we have in the Component Inspector tool.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — April 25, 2018 at 8:05 am ([permalink](https://www.codenameone.com/blog/version-4-1-launch-screen-storyboards.html#comment-23969))

> Francesco Galgani says:
>
> You wrote: «The default Codename One launch storyboard simply shows your app’s icon in the middle of the screen». My question is about the size of this icon: because it’s always sized 512×512 pixels, does its actual size vary according the screen DPI? In my apps, I implemented a splash screen with the app’s icon sized as I want it, but if Codename One shows app’s icon in the middle of the screen, probably I’ll have two splash screens, that will be the same app’s icon sized differently… that is not what I want, of course.
>



### **Francesco Galgani** — April 25, 2018 at 12:22 pm ([permalink](https://www.codenameone.com/blog/version-4-1-launch-screen-storyboards.html#comment-23642))

> Francesco Galgani says:
>
> You wrote that we can get smaller binaries, but the screenshot images are still generated with ios.multitasking=true and without ios.fastbuild=true build hints. But ios.fastbuild=true is only for debug builds (according to the developer guide). Please see the shannah reply to this issue: [https://github.com/codename…](<https://github.com/codenameone/CodenameOne/issues/2396>)
>



### **Shai Almog** — April 26, 2018 at 5:42 am ([permalink](https://www.codenameone.com/blog/version-4-1-launch-screen-storyboards.html#comment-23925))

> Shai Almog says:
>
> It should adapt the size but that would probably be inconsistent with your splash screen.
>
> I misunderstood Steve’s implementation so the post above gets some current facts wrong. I think we need to shift the implementation to match the post not the other way around. We’re discussing this, I don’t think it makes sense to have both the screenshots and the xib.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
