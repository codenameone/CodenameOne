---
title: Subscription Pitfall
slug: subscription-pitfall
url: /blog/subscription-pitfall/
original_url: https://www.codenameone.com/blog/subscription-pitfall.html
aliases:
- /blog/subscription-pitfall.html
date: '2018-11-05'
author: Shai Almog
---

![Header Image](/blog/subscription-pitfall/in-app-purchase.jpg)

A while back [Steve wrote about auto-renewing subscriptions](/blog/autorenewing-subscriptions-in-ios-and-android.html) and I recently got a chance to implement such a subscription in an app. However, it seems that all the changes in the world of in-app purchase created a situation where API’s work in some cases and don’t work for all of them.

__ |  After publishing this post we walked back on this, you now need to use subscribe for subscriptions again!   
---|---  
  
In the blog post, Steve used the `purchase(sku)` API to subscribe. This worked correctly as subscriptions are determined by the respective app store. As I implemented this code I chose to use the `subscribe(sku)` method which seems to make more sense. Unfortunately it doesn’t work and would be deprecated with the update this Friday.

It seems to work and even works on Android/iOS however, it doesn’t work with the receipt API which is an important part of the IAP workflow.

Despite a lot of the work we did for IAP it’s still one of the more painful API’s we need to work with. Dealing with the server side API is a nightmare. I hope we’ll come up with a better implementation for that moving forward.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
