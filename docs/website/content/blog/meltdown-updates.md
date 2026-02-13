---
title: Meltdown and Other Updates
slug: meltdown-updates
url: /blog/meltdown-updates/
original_url: https://www.codenameone.com/blog/meltdown-updates.html
aliases:
- /blog/meltdown-updates.html
date: '2018-01-16'
author: Shai Almog
---

![Header Image](/blog/meltdown-updates/new-features-3.jpg)

I’ve been so busy I just don’t have time to blog as much as I should. I do hope next month will be better in this regard (more on that below) but right now I have to make an important announcement. There are new chip vulnerabilities I’m sure you heard a lot about specifically [Meltdown & Spectre](https://arstechnica.com/gadgets/2018/01/heres-how-and-why-the-spectre-and-meltdown-patches-will-hurt-performance/). Thankfully we are at a layer that shouldn’t be impacted by these issues but we need to update our servers and will be doing so over the course of the next few days (possibly more as patches get updated).

So you might see some sudden downtime starting today, don’t be alarmed but let us know in case we didn’t notice. You can post to the [google group](https://groups.google.com/forum/#!forum/codenameone-discussions) even if our website goes down…​

There is some concern about potential impact to the build process speed. We don’t think this should change significantly as the big bottleneck in our build process isn’t CPU speed it’s IO. The kernel is involved in IO but it’s far from the bottleneck of that process.

### New cn1libs for Objective-C & Code Scanning

In other news Steve published two new cn1libs. The first is [CN1ObjCBridge](https://github.com/shannah/CN1ObjCBridge) which is effectively reflection from Java into the Objective-C platform on iOS.

Instead of using [native interfaces](https://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html) to invoke native code you could use an API that lets you send Objective-C messages (their equivalent of method calls) from Codename One Java code. That’s pretty impressive.

For most normal cases I think using native interfaces would still be better but this could fill in a niche for things that could use reflection or better callback functionality.

If you want to see a usage example check out his new [scandit library](https://github.com/shannah/cn1-codescan-scandit) which uses the [Scandit barcode/QR code scanning API](https://www.scandit.com/products/barcode-scanner/).

Scandit provides much faster barcode/QR code scanning speeds but it comes at a price. You need to pay per seat licensing fees that can be a bit high. The worst part of it is that we can’t redistribute their binaries or use them with cocoapods/gradle. This means we can’t distribute a precompiled cn1lib for this product. However, if you need professional grade bardcode scanning this should work rather well.

__ |  We implemented this library based on a request from an enterprise subscriber   
---|---  
  
### Push Console

We added a lot of new features over the time I’ve taken away from blogging. I’ll write more about those but for now I’ll just mention one new feature. Push simulator.

In the simulator you can now open a window that will help you debug push applications. E.g. you can press a button to send a registration success callback where you will get a push key. You can also send a registration error and send a message.

Notice that there is no message type option as that’s mostly seamless for the client. E.g. if you send a type 3 message it’s really just two separate message.

Type 2 or 1 etc. are all meaningless in the simulator as we only simulate the running application and not background behavior. Still I found it very useful to simulate these messages and was able to debug some nuanced behavior in NetBeans.

### Status of Uber Module

I was hoping that the [Uber module](/blog/uber-clone-trickling-down.html) would be completely finished by now but it isn’t. I’m pretty close though.

I’ve done 30 lessons by now and uploaded 23. I’m guessing 40 or 45 lessons should be the final number when I’m done. I thought I’d finish by next Sunday but that’s already pushed back to Tuesday and I’m not sure I can make that deadline either. I’m doing everything I can to finish this before the end of the month as we need to start preparing to the 4.0 release which is already looming.

The reasons this is taking so long have a lot to do with the amount of extra work I need to do but they also have a lot to do with how I divide my time. I have some administrative tasks in the company that just keep me from finishing this process.

Regardless of the above I’m pretty happy with the results so far and I have a lot to say about this once I’m done.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Tommy Mogaka** — January 18, 2018 at 10:05 pm ([permalink](https://www.codenameone.com/blog/meltdown-updates.html#comment-23706))

> Tommy Mogaka says:
>
> Hi Shai, great work by Steve on the Scandit cn1lib. I noticed that CodenameOne is not listed in the Scandit Developer page. I saw a bunch of other development platforms there and I thought perhaps you guys could consider getting it listed there to get more traction. Best Regards!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmeltdown-updates.html)


### **Shai Almog** — January 19, 2018 at 5:17 am ([permalink](https://www.codenameone.com/blog/meltdown-updates.html#comment-23714))

> Shai Almog says:
>
> Thanks. We already asked for a listing. I posted this and followed by sending them the details so it will probably take some time for everything to update on their end.  
> Their stuff seems pretty cool with dedicated barcode scanning phone cases etc.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmeltdown-updates.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
