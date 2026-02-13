---
title: Admob Interstitial Ads & Supertabs
slug: admob-interstitial-ads-supertabs
url: /blog/admob-interstitial-ads-supertabs/
original_url: https://www.codenameone.com/blog/admob-interstitial-ads-supertabs.html
aliases:
- /blog/admob-interstitial-ads-supertabs.html
date: '2014-10-26'
author: Shai Almog
---

![Header Image](/blog/admob-interstitial-ads-supertabs/admob-interstitial-ads-supertabs-1.png)

  
  
  
  
![Picture](/blog/admob-interstitial-ads-supertabs/admob-interstitial-ads-supertabs-1.png)  
  
  
  

  
  
  
  
Ram (the developer of  
[  
yhomework  
](http://www.codenameone.com/featured-yhomework.html)  
), wanted to improve his ad revenue on Android/iOS thru interstitial (full screen) ads and integrated those using native interfaces. Kindly enough he contributed these changes back and Chen packaged this as a cn1lib which you can now easily use to add support for  
[  
full screen ads to your Android/iOS apps  
](https://code.google.com/p/admobfullscreen-codenameone/)  
. 

On a different subject a long time RFE has been the ability to customize the tabs using a more powerful API. The main blocker for that has been the hardcoding of Buttons (or really radio buttons) as tabs in the Tabs component.

With the upcoming version of Codename One you will be able to replace the usage of Button with just about anything by overriding a set of protected methods in the Tabs component. These effectively allow the Tabs class to understand the structure of your potentially complex class and communicate the requirements of the component. In the relatively simplistic implementation below I just used a layered layout to place a close button in the right side of every tab. This is code I used in the kitchen sink demo to test this effect.  
  

  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
