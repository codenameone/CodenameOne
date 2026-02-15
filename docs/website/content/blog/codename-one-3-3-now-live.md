---
title: Codename One 3.3 Now Live
slug: codename-one-3-3-now-live
url: /blog/codename-one-3-3-now-live/
original_url: https://www.codenameone.com/blog/codename-one-3-3-now-live.html
aliases:
- /blog/codename-one-3-3-now-live.html
date: '2016-01-26'
author: Shai Almog
---

![Header Image](/blog/codename-one-3-3-now-live/codenameone3.3-release.jpg)

We are thrilled to announce the immediate availability of Codename One 3.3!  
Version 3.3 was tumultuous, we made a lot of earth shattering changes to performance, animations, fonts  
and many other things. As a result we have a ground-breaking release that requires  
a step back.   
With 3.4 we want to tone down on the “big ticket changes” and work heavily on product refinement. We are already  
hard at work updating our docs and refining our general process..  
In 3.3 we focused a lot on the open source aspect of Codename One which is something we neglected  
to some extent in the past. We intend to keep pushing towards more transparency and community involvement  
as the project grows.  
Codename One 3.4 is currently scheduled for May 3rd 2016. Its chief goals are: Performance, Platform fidelity,  
Documentation & ease of use. 

### Highlights Of The Release – Click For Details

____Faster rendering of backgrounds & Labels

Up until now the logic for rendering the background of the component  
resided entirely within Component.java & DefaultLookAndFeel.java.  
This allowed for a simple rendering logic that is implemented in a single place, however it didn’t allow us  
to deeply optimize some operations in a platform specific way. We moved the rendering into  
CodenameOneImplementation.java which allowed us to override the logic both on Android & iOS  
to deliver native grade performance on any device.  
On iOS this has been a strait forward change where most of the low level logic is now written using  
very efficient C code. On Android the pipeline complexity is far greater, but thanks to this approach  
we were able to reuse many system resources and reduce JANK significantly in applications. This  
work is still ongoing but the bit effort has been implemented.  
This is probably the biggest piece of multiple changes that went into this release including fast tiling  
support, better font/string texture caching, Container API optimizations etc.  
Read more about this work in [this blog post](/blog/code-freeze-for-3.3-performance.html). 

____Animation Manager, Title Animations & Style Animations

We rewrote the animation logic in Codename One for 3.3.  
This broke some backwards compatibility but this was for a good cause as we now have a central class  
that manages all animation events going forward. This means that you should no longer get odd  
exceptions when using many animations in sequence.  
As part of this enhancement we also added new animation types such as title scroll animation and the  
ability to animate a style object UIID.  
Read more about this work in [this blog post](/blog/new-animation-manager.html). 

____“Remastered” Documentation (ongoing)

We are redoing a lot of the Codename One documentation from scratch with  
Codename One 3.3. This is ongoing and we barely just started but the new documentation is far more readable,  
detailed and clear. Moving forward we are confident that our developer guide, JavaDocs & videos will be in a  
league of their own!  
Read more about this work in [this blog post](/blog/wiki-parparvm-performance-actionevent-type.html). 

____Material Design Icons

FontImage has been around for a while but up until now  
we didn’t use it to its full extent. It required getting an icon font, configuring it and we just skipped it  
for some cases.  
With 3.3 we integrated the material design icon font which makes adding flat icons to your application  
remarkably simple!  
Read more about this work in [this blog post](/blog/material-icons-background-music-geofencing-gradle.html). 

____Media Playback & Geo Fencing in the Background

We continued the background process trend with  
3.3 as we enabled both geofencing (to track device location in the background) and media  
playback in the background.  
Read more about this work in [this blog post](/blog/material-icons-background-music-geofencing-gradle.html). 

____PhoneGap/Cordova Compatibility

Codename One always supported embedding  
HTML & JavaScript but it didn’t support embedding things such as the Cordova/PhoneGap API’s.  
With the new open source project we announced we can now convert many Cordova/PhoneGap apps to  
Codename One apps and deliver quite a few compelling advantages.  
Read more about this work in [this blog post](/blog/phonegap-cordova-compatibility-for-codename-one.html). 

____New hello world project & icon

A major focus of this release was making Codename One useful and  
attractive right out of the box. As part of that work we replaced the default icon, redid the hello world app to a  
more impressive (yet simple) demo and updated the default fonts.  
Read more about this work in [this blog post](/blog/good-looking-by-default-native-fonts-simulator-detection-more.html). 

____New Simplified Native Fonts

Fonts were a difficult subject prior to 3.3. You could either use the  
portable but ugly system fonts, or go with the gorgeous but flaky TTF fonts. Both don’t make sense when Android  
ships with the great Roboto font and iOS ships with the gorgeous Helvetica Neue font.  
We now have support for a new font notation with the native: prefix. This notation (supported by the Designer),  
allows us to leverage the existing native fonts of the device which look both native and gorgeous.  
Read more about this work in [this blog post](/blog/good-looking-by-default-native-fonts-simulator-detection-more.html). 

____Terse syntax enhancements

In 3.2 we started moving towards terse syntax for container hierarchy  
construction and with 3.3 we brought that to fruition. We added methods such as an add method that accepts an  
image. We added factory encloseIn methods to almost all of the layout managers, we added form constructors  
that accept layout managers and much more!  
  
Read more about this work in [this blog post](/blog/properties-continued-terseness.html). 

____ParparVM Performance & Open Source

Our iOS VM has been open source from the start but we didn’t encourage its  
usage outside of Codename One. This changed with 3.3 and we are actively promoting the ParparVM OpenSource  
project.  
Unrelated to that we made a lot of performance improvements to the core VM translation logic, it should be very  
competitive in terms of generated code to pretty much everything else on the market. Especially with API calls as  
our entire API is hand-coded and highly optimized.  
Read more about this work in [this blog post](/blog/parparvm-spreads-its-wings.html). 

____Properties file format support

We didn’t have support for Java venerable Properties file format before  
3.3. Surprisingly developers didn’t really complain about that ommission as we support XML, CSV & JSON.  
Now we can add Properties to that list!  
Read more about this work in [this blog post](/blog/properties-continued-terseness.html). 

____Ending Support for the codescan API

3.3 will be the last release that includes an implementation of the codescan  
API for QR code/barcode reading. We will remove this API completely and we ask users to migrate their code to  
use the new [codescan cn1lib](https://github.com/codenameone/cn1-codescan/). When we initially  
introduced this API we didn’t have support for cn1libs and integrated this into the core directly.  
Read more about this work in [this blog post](/blog/video-new-defaults-barcode-qrcode-changes.html). 

You can also read the far more detailed list of release notes [here](/codename-one-3-3-release-notes/).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — January 27, 2016 at 11:50 am ([permalink](https://www.codenameone.com/blog/codename-one-3-3-now-live.html#comment-21615))

> Chidiebere Okwudire says:
>
> Cheers guys! Well done!!
>
> Something is obviously missing: An update on the new GUI builder which should have entered beta with release 3.3. What’s the status and why isn’t it mentioned in the feature list above?
>



### **Shai Almog** — January 27, 2016 at 12:26 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-3-now-live.html#comment-22666))

> Shai Almog says:
>
> Thanks!
>
> We thought about this quite a bit and chose not to emphasis that with this release even though we’ve done a lot of work on the GUI builder we still don’t feel its ready for beta. The UI/UX still isn’t perfect and the tutorials aren’t even remotely close to done.
>
> We intended to do the finishing touches during the late part of 3.3’s release cycle but then we got delayed with regressions/fixes and are now delayed due to the documentation overhaul work. So we just didn’t get around to working on the GUI builder full time.
>
> I think that once we change the documentation/videos to use the new GUI builder people will start using it and will start filing issues. This will bring it to beta-status thru inertia during the 3.3 lifespan.
>



### **Chidiebere Okwudire** — January 27, 2016 at 12:36 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-3-now-live.html#comment-22699))

> Chidiebere Okwudire says:
>
> Clear and probably a good choice.
>
> I tried creating a new project with the new GUI builder last week and after a few crashes and unclarity of how to do simple things that I can easily do with the old builder (e.g. setting components to parts of a border layout), I quit and reverted back to the old GUI builder. I’ll stick with that till further notice. I also saw a button for converting an existing project to a new GUI builder project so I guess when it’s ready, I’ll be able to migrate away from the huge state machine class.
>



### **Shai Almog** — January 27, 2016 at 12:48 pm ([permalink](https://www.codenameone.com/blog/codename-one-3-3-now-live.html#comment-22637))

> Shai Almog says:
>
> Yes the migration wizard is already there.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
