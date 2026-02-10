---
title: Migrating To Androids In-App-Purchase 3.0
slug: migrating-to-androids-in-app-purchase-30
url: /blog/migrating-to-androids-in-app-purchase-30/
original_url: https://www.codenameone.com/blog/migrating-to-androids-in-app-purchase-30.html
aliases:
- /blog/migrating-to-androids-in-app-purchase-30.html
date: '2014-10-14'
author: Shai Almog
---

![Header Image](/blog/migrating-to-androids-in-app-purchase-30/migrating-to-androids-in-app-purchase-30-1.png)

  
  
  
  
![Google Play In-App-Purchase](/blog/migrating-to-androids-in-app-purchase-30/migrating-to-androids-in-app-purchase-30-1.png)  
  
  
  

  
  
  
  
  
  
**  
Updated:  
**  
Originally this article referred to a system based on Display.setProperty for distinguishing consumable types. This turned out to be an issue in beta testing and was replaced with either a naming convention or build argument. 

Google announced a couple of weeks ago that Android’s In App Purchase 2.x API will be retired soon and all code should be migrated to version 3.0 of the API. Unfortunately version 3.0 is a big departure from version 2.0 which we currently support and its difficult for us to support both so unlike the normal case where we try to maintain compatibility with build arguments we will make a clean break to 3.0.

Important: if you have a project that relies on in-app-purchase and you are about to make a release we will flip the switch in one week from now! This means that you will have to migrate version 3.0 if you build a version next week, so if you have a release pending you might want to get it out ASAP.

The Codename One purchase API remains unchanged and will work in exactly the same way, however due to integration with the way Google handles various things you might need to tune this a bit:

1\. You must have the android.licenseKey build hint set or the build will fail. You can get the license key from the Services & API section in the Google play store.

2\. In the 3.0 API products are consumable by default, if you want a product to be non-consumable you will need to explicitly declare it either by making the SKU end with the word  

  
  
  
  
  
  
  
  
nonconsume (e.g. MySKU001_nonconsume) or by setting the build argument  
  
  
  
  
  
  
  
  
  
android.nonconsumable=itema,  
  
  
itemb,itemc  
  
  
Apple explains the difference between consumable/non-consumable products as such: 

**  
Consumable products  
**  
– Items that get used up over the course of running your app. Examples include minutes for a Voice over IP app and one-time services such as voice transcription.  
  
**  
  
Non-consumable products  
**  
– Items that remain available to the user indefinitely on all of the user’s devices. They’re made available to all of the user’s devices. Examples include content, such as books and game levels, and additional app functionality.

3\. With the version 3 support we now have subscriptions on Android. However, Google’s API doesn’t allow unsubscribing from code so that functionality isn’t available. Users have the ability to unsubscribe via the portal.  
  

  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
