---
title: Cloud Storage, Cloud Bind(tm), iPhone 5 simulator, barcodes and much more
slug: cloud-storage-cloud-bindtm-iphone-5-simulator-barcodes-and-much-more
url: /blog/cloud-storage-cloud-bindtm-iphone-5-simulator-barcodes-and-much-more/
original_url: https://www.codenameone.com/blog/cloud-storage-cloud-bindtm-iphone-5-simulator-barcodes-and-much-more.html
aliases:
- /blog/cloud-storage-cloud-bindtm-iphone-5-simulator-barcodes-and-much-more.html
date: '2012-11-14'
author: Shai Almog
---

![Header Image](/blog/cloud-storage-cloud-bindtm-iphone-5-simulator-barcodes-and-much-more/codename-one-charts-1.png)

We just made a major update including a pile of fixes and features. One of the biggest things we are launching right now is an early preview of our new Cloud Storage and Cloud Bind ™ solutions. 

Cloud Storage allows you to effectively use our cloud as a big object database, similar to other big data solutions as a sort of key/value pair lookup engine that allows you to share/sync between devices.  
  
The Cloud Bind ™ solution allows you to seamlessly bind components such as lists, text components etc. to the cloud where changes automatically persist to the Cloud Storage and data is automatically fetched from there. 

These are currently limited only to pro users mostly because we haven’t yet figured out the free quotas we want to allocate for free/basic users. We will publish more tutorials and information when we have this fleshed out.

We also added an iPhone 5 simulator skin allowing you to generate iPhone 5 resolution screenshots. 

And we finally added a bar code/qr code reader API. However, this wasn’t as easy as one would suspect. The problem is that ZXing our API of choice for the QR code in the demo, doesn’t do barcodes on iOS… So we had to use a different implementation on iOS which might not be as good as ZXing with QR codes. So the old native approach will still work if you want it too, but you don’t have to because we have a much simpler API.

There is allot more coming in the next couple of months… Stay tuned. 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
