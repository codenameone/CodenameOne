---
title: Processing, Responsiveness, Native & more
slug: processing-responsiveness-native-more
url: /blog/processing-responsiveness-native-more/
original_url: https://www.codenameone.com/blog/processing-responsiveness-native-more.html
aliases:
- /blog/processing-responsiveness-native-more.html
date: '2014-03-18'
author: Shai Almog
---

![Header Image](/blog/processing-responsiveness-native-more/processing-responsiveness-native-more-1.png)

  
  
  
[  
![Picture](/blog/processing-responsiveness-native-more/processing-responsiveness-native-more-1.png)  
](/img/blog/old_posts/processing-responsiveness-native-more-large-2.png)  
  
  

Some of you have already noticed a build error when building for iOS if you used the processing package. This is due to a small change we made to the package where we replaced all usage of Hashtable/Vector with Map/List. That allows the processing package to work with the new parseJSON method that returns the new collection code and thus be MUCH faster on iOS. 

Unfortunately due to the way java codes method signatures you need to update the libraries in order to for this change to work, we automatically update the libraries when you send the build but that means you will need to send again if you get that failure.

We also improved button responsiveness on iOS, this was a very hard to track issue which we’ve been following for a while. Older iOS devices are somewhat overeager with pointer dragged events and tend to send them all over the place. Unfortunately this triggered a release of a button in some cases making it feel unresponsive. Normally dragging a pressed button should not release it but we had some very old pre-Codename One code that did just that for some cases. This code is now remove and hopefully your UI’s will just “magically” become more responsive.

If you do any native interfaces programming in Android you should be familiar with our AndroidUtil class which allows you to access native device functionality more easily from the native code. E.g. many Android API’s need access to the Activity which you can get by calling AndroidNativeUtil.getActivity() which is much simpler than the alternative approaches.

We now also have additional functions in the native util including the following –  
  
  
AndroidNativeUtil.addLifecycleListener/removeLifecycleListener  
  
– these will essentially provide you with a callback to lifecycle events: onCreate etc. which can be pretty useful for some cases.  
  
  
  
  
AndroidNativeUtil.registerViewRenderer – PeerComponent’s are usually shown on top of the UI since they are rendered within their own thread outside of the EDT cycle. So when we need to show a Dialog on top of the peer we grab a screenshot of the peer, hide it and then show the dialog with the image as the background (the same applies for transitions). Unfortunately some components (specifically the MapView) might not render properly and require custom code to implement the transferal to a native Bitmap, this API allows you to do just that. Esoteric but if you need it then its a lifesaver!  
  
  
  
  
  
Last but not least is AndroidImplementation.runOnUiThreadAndBlock(Runnable) – this is such a common pattern that we had to generalize it into a public static method. Its identical to Activity.runOnUiThread but blocks until the runnable finishes execution. Very important.  
  
  
  
  
Samples for usage for all of these is available in our  
[  
map library source code  
](https://github.com/codenameone/codenameone-google-maps/blob/master/GoogleMaps/src/com/codename1/googlemaps/InternalNativeMaps.java)  
.  
  
  

One of Androids annoying “features” is that everything needs to be coded at a very low level and a lot of components don’t do the “right thing” by default. E.g. mailto: URL’s don’t launch the mail app by default when clicked from a BrowserComponent on Android. We fixed this to be the sensible default but we still pass the shouldNavigate call in case you want to do something differently.

If you aren’t familiar with Dialog.showPopup(Component) it allows you to create the UI common on the iPad where a dialog points at the component that launched it. However, if you have a rather complex UI you might not have a component to point at, in which case we can use a new version of the method that accepts a rectangle to point towards as Dialog.showPopup(Rectangle).  
  
  
  
  
On the right hand side you should see some new promotional graphics. It includes some featured Codename One apps including  
[  
qmarkets  
](/featured-qmarkets.html)  
,  
[  
yhomework  
](/featured-yhomework.html)  
&  
[  
travel together  
](https://play.google.com/store/apps/details?id=il.co.fooxia.apps.travel2gether)  
.  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
