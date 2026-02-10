---
title: A Thank You & an Important Update On Android Builds
slug: a-thank-you-an-important-update-on-android-builds
url: /blog/a-thank-you-an-important-update-on-android-builds/
original_url: https://www.codenameone.com/blog/a-thank-you-an-important-update-on-android-builds.html
aliases:
- /blog/a-thank-you-an-important-update-on-android-builds.html
date: '2016-02-02'
author: Shai Almog
---

![Header Image](/blog/a-thank-you-an-important-update-on-android-builds/html5-banner.jpg)

We’d like to thank all of you who signed up to the pro subscription, the release of 3.3 is the perfect time to  
do that. So we are opening up the JavaScript build target for 1 year until March 1st 2017 to all current pro subscribers!  
If you have a pro subscription you can start sending a JavaScript build right away and experiment with porting  
your app to the web…  
If you don’t have a pro license currently then you have until March 15th to upgrade and enjoy this offer. After  
March 15th the JavaScript port will return to enterprise only status for everyone who didn’t signup prior to that. 

If you are not familiar with the JavaScript port check out Steve’s [great writeup](/blog/javascript-port.html)  
on it, you can also try out some of our demos live right now [here](/demos.html)  
(using desktop or mobile browsers). Just click the JS Port link at the bottom right section of a demo e.g.  
restaurant or property cross.   
FYI If you cancel the subscription during this time or let it lapse this capability will be lost so make sure to keep it in place. 

#### Memory Issue on Android Builds

With the switch to gradle in Android builds we experienced memory issues for some cases when building huge  
apps. For some cases ading `android.gradle=false` to the build hints was enough but for others  
not so much. 

The problem relates to the size of Google Play Services which are an essential part of Android applications but  
have grown to a size that is pretty big. We need play services for better location tracking, in-app-purchase, push  
notification, maps etc.  
In the past we had the build hint `android.includeGPlayServices` which  
tried to be smart about play services but its a bit too coarse as it only accepts true/false. 

To alleviate this issue we deprecated the `android.includeGPlayServices` and are introducing  
the new build hints below that will allow you to selectively include a play service. This means that future builds  
to the Codename One build servers can have one of the following 5 states: 

  1. `android.includeGPlayServices=true` & one or more of the `android.playService`  
entries defined – this is an illegal state and will cause the build to fail
  2. `android.includeGPlayServices=true` & no `android.playService`  
entries defined – this will fallback to compatibility mode
  3. `android.includeGPlayServices=false` & no `android.playService`  
entries defined – play services won’t be included
  4. `android.includeGPlayServices` undefined & no `android.playService`  
entries defined – some play services will be included by default specifically: plus, auth, base, analytics, gcm, location, maps,  
ads.
  5. `android.includeGPlayServices` undefined & one or more `android.playService`  
entries defined – only the play services you explicitly select will be included.

The last two are a bit confusing so just to clarify if you do this: 
    
    
    android.playService.plus=true

The only play service included will be plus. However, if you don’t define any build hints specifically the above list  
of play services will be included. This is a “sensible default mode” that we picked to make the transition easier. 

The list of supported hints follows, they all accept true/false as arguments for inclusion/exclusion. 
    
    
    android.playService.plus
    android.playService.auth
    android.playService.base
    android.playService.identity
    android.playService.indexing
    android.playService.appInvite
    android.playService.analytics
    android.playService.cast
    android.playService.gcm
    android.playService.drive
    android.playService.fitness
    android.playService.location
    android.playService.maps
    android.playService.ads
    android.playService.vision
    android.playService.nearby
    android.playService.panorama
    android.playService.games
    android.playService.safetynet
    android.playService.wallet
    android.playService.wearable

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
