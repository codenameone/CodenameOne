---
title: MoPub Ad Support
slug: mopub-ad-support
url: /blog/mopub-ad-support/
original_url: https://www.codenameone.com/blog/mopub-ad-support.html
aliases:
- /blog/mopub-ad-support.html
date: '2013-07-09'
author: Shai Almog
---

![Header Image](/blog/mopub-ad-support/mopub-ad-support-1.png)

  
  
  
[  
![Picture](/blog/mopub-ad-support/mopub-ad-support-1.png)  
](/img/blog/old_posts/mopub-ad-support-large-2.png)  
  
  

**  
Updated:  
**  
We also added the option: ios.mopubTabletId which is required to build tablet compilant apps. It must point to a leaderboard ad unit.  
  
  
  
  
  
  
For a long time we’ve been looking for a proper partner on banner ads and could only find mediocre networks or worse. Unfortunately it seems the field of proper advertising on device is still problematic and we just couldn’t find anyone with an offering that will work properly across the globe/devices. 

  
So we picked the next best thing, we integrated  
[  
MoPub  
](http://www.mopub.com/)  
support for Android/iOS only (for true cross platform our Inneractive support is still in place).  
  
MoPub isn’t an advertizing network, its an ad exchange. So effectively when you place a banner ad via MoPub you can get an ad from AdMob, iAd or any one of a relatively large list of partner networks.

  
The benefit here is in fill rates, you can improve ad fillrates and payments since if network X doesn’t have the right ad network Y could kick into place! You can mix and match many different networks including iAd which works really well on iOS but isn’t available elsewhere.  

  
To work with MoPub you will need to signup to their service and create ad units, apps etc. using their interface. Make sure to create both banners and leaderboards since iOS will use banners for iPhone but leaderboards for iPad (otherwise iAd’s look awful). Then get the integration code from their unit (different codes for Android/iOS) and add the build arguments:  
  
  
ios.mopubId=Your ad unit here  
  
ios.mopubTabletId=Leaderboard ad unit for a tablet  
  
android.mopubId=Your ad unit here

  
And you will get banners like the one in the picture.  

  
Notice that currently the ads take over the bottom portion of the screen so you might want to add padding or margin there  
  
. We are thinking whether this should be automated and if so in what way. This is an experimental feature at the moment so some things might change with it as we move along, but we felt that its a rather urgent feature which is why we are releasing it right now. Hopefully you will find it useful.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
