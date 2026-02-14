---
title: Use our Open Source Code to Build Codename One Offline
slug: use-open-source-build-offline
url: /blog/use-open-source-build-offline/
original_url: https://www.codenameone.com/blog/use-open-source-build-offline.html
aliases:
- /blog/use-open-source-build-offline.html
date: '2018-02-20'
author: Shai Almog
---

![Header Image](/blog/use-open-source-build-offline/deep-dive-into-mobile.jpg)

I promised 2 new course modules for February and just published the first one. It covers the process of building a Codename One app from the Codename One source code. The whole process is done **without using the Codename One plugin or build servers**. It uses only open source project code to deliver iOS/Android & desktop binaries!  
You can check out the full module in the [Deep Dive into Mobile Programming](https://codenameone.teachable.com/p/deep-dive-into-mobile-development-with-codename-one) course in the academy.

__ |  The deep dive course is bundled into the full [Build real world full stack mobile apps](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java) course for free!   
---|---  
  
Working with the Codename One sources is not for the faint of heart. You can learn a lot from going through the process. However, if your only goal is to avoid the build servers you might find it harder to work with.

In fact I personally use the build servers when building apps and testing them. I almost never use offline build or the sources directly. Instead I hack and test things via the include source options. However, learning this is still valuable and I’m aware of a few people who don’t share my opinion on this matter…​

Still, why would I create a guide for something like this?

There are 3 types of individuals I can think of who might benefit from this guide:

  * If you are the type of person who needs to do everything yourself then this is pretty much it

  * If you want to understand the underpinning of Codename One at a deeper level than the more abstract descriptions. Then this is a good first step

  * If you want to feel secure that you can hack Codename One manually if our service changes or becomes unavailable in the future then the mere existence of this guide should help calm some of those concerns

To me the third option makes a lot of sense. I think that the existence of this module is probably the biggest value it delivers.

### Performance

Next week I will post a module covering performance tuning, tips and pitfalls. I already wrote a lot of material for this module which is shaping up as an even bigger module.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Tommy Mogaka** — February 23, 2018 at 5:31 am ([permalink](https://www.codenameone.com/blog/use-open-source-build-offline.html#comment-23887))

> Tommy Mogaka says:
>
> Hi Shai,  
> Thanks for making this provision available. It is a brave move and it demonstrates confidence in cn1. I personally am happy to know that the option is available but I still continue to use the build servers but I know of some users who are a bit fastidious when it comes to security requirements. An example is banks. For the purposes of such clients, does the module also include the push messaging servers?  
> Best Regards!
>



### **Shai Almog** — February 24, 2018 at 4:54 am ([permalink](https://www.codenameone.com/blog/use-open-source-build-offline.html#comment-23608))

> Shai Almog says:
>
> Thanks!  
> No the module stops after running on the native IDE’s. The assumption is that if you got that far implementing native push directly won’t be a big challenge. You won’t be able to use our push servers without a subscription obviously so you’d need to write your own push code.
>



### **CosmicDan** — August 5, 2018 at 12:50 am ([permalink](https://www.codenameone.com/blog/use-open-source-build-offline.html#comment-23752))

> CosmicDan says:
>
> Was excited to hear this, but then disappointed to hear it’s behind a $200 payment. It’s a bit of a steep entry requirement for an open-source developer who just wants to look for options on Android and Windows Desktop app development without financially committing to one particular system for experimentation. But I understand, Codename One is a business after all.
>



### **Shai Almog** — August 5, 2018 at 4:54 am ([permalink](https://www.codenameone.com/blog/use-open-source-build-offline.html#comment-24042))

> Shai Almog says:
>
> All the material is available for free, this module just pools it into one location. Using the source code won’t make your life easier in building. You can use the build servers for free which is far easier.
>



### **CosmicDan** — August 5, 2018 at 5:04 am ([permalink](https://www.codenameone.com/blog/use-open-source-build-offline.html#comment-21578))

> CosmicDan says:
>
> Apologies, I misread the plan pricing thinking that I couldn’t make desktop apps with the free or cheaper tier. What that seems to be is just *native* desktop apps, which I don’t have an interest in after all – I am hoping that the CN1 UWP target UI can scale well to larger displays (tablets/laptops).
>



### **Shai Almog** — August 6, 2018 at 4:20 am ([permalink](https://www.codenameone.com/blog/use-open-source-build-offline.html#comment-24011))

> Shai Almog says:
>
> Notice that you can build everything including that. There are instructions in the group and it’s pretty obvious how to do it from the code. In terms of license you are allowed to do everything, we have no licensing restrictions unlike some other tools…
>
> There is one port whose code isn’t open source and that’s the JavaScript port.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
