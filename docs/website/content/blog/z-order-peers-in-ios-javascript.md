---
title: Z-Order Peers in iOS & JavaScript
slug: z-order-peers-in-ios-javascript
url: /blog/z-order-peers-in-ios-javascript/
original_url: https://www.codenameone.com/blog/z-order-peers-in-ios-javascript.html
aliases:
- /blog/z-order-peers-in-ios-javascript.html
date: '2017-01-25'
author: Shai Almog
---

![Header Image](/blog/z-order-peers-in-ios-javascript/maps.jpg)

Just last week [I mentioned](/blog/html-maps-z-order-peer-properties-update.html) the effort we were taking to bring the z-ordered peer components into other platforms and I’m pretty happy to report that tomorrows update should include support for iOS & the JavaScript ports for z-ordering.

What this means is that peer components (e.g. maps, videos, browser etc.) can reside below a component in the Codename One hierarchy and won’t **always** be on top as it is now. This opens up Codename One for a huge set of use cases and makes building some types of apps (e.g. Maps) **much** easier!

The implementation on iOS is very different from Android. Z-Ordering on Android was “relatively” simple as it allowed us to place components pretty much within their place in the hierarchy. However, on iOS and other platforms this is impractical as it might cause an issue with some complex components. Our initial solution was to define two layers, everything that paints below the peer and everything that paints above the peer.

This would have meant that if you had two peers in a Form they would both be in the same Z location and won’t be mixed.

However, we found a better way using a “clear” operation where each peer component will clear the elements below it and paint in the right location. That way peers are always painted at the bottom but have a special clear graphics function to remove anything that should be underneath. This does mean that peers should be opaque though…​

The “clear” approach is the one we took in the JavaScript port and it will carry those tradeoffs.

This is a HUGE change so if your app relies on peers (Maps, Video, Browser etc.) I suggest testing it extensively and letting us know at once if you run into any regressions!

Only UWP remains as a target for this feature, thanks to the work done in iOS/JavaScript this might not be too hard.

### Moving Forward

Now that this is effectively working we need to use peers more both in our demos and in practice. A huge case is in the mapping applications where we need to enhance the demo with peers.

Another thing we need to work on is better video support and especially video/image capture using a peer. This is something we should offer relatively easily and it’s a HUGE advantage over tools such as PhoneGap/Cordova/Qt etc.  
all of which are incapable of doing this.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
