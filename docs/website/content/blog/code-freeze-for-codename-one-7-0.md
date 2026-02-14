---
title: Code Freeze for Codename One 7.0
slug: code-freeze-for-codename-one-7-0
url: /blog/code-freeze-for-codename-one-7-0/
original_url: https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html
aliases:
- /blog/code-freeze-for-codename-one-7-0.html
date: '2021-01-29'
author: Shai Almog
---

![Codename One 7.0 - Video](/blog/code-freeze-for-codename-one-7-0/7.0-Video-1c.jpg)

At long last we’re entering code freeze for Codename One 7.0. This release cycle has been longer than it should have been because of many detours along the way.

But finally if all goes according to plan, **version 7.0 should be out next Friday**.

The code freeze won’t impact most of you as it’s mostly an artifact of our release cycle.

We will have the regular Friday release but will only have critical reviewed commits during this week.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — February 1, 2021 at 9:59 pm ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24392))

> Javier Anton says:
>
> I’m embarrassed to admit to how long I spend each day working with CN1. Thank you for all the effort, this project is a huge undertaking. You all deserve a gold medal for this
>
> As I begin expanding my use of CN1, I keep needing more and more to append my final native sources and rebuild. This process takes considerable time, as each finished build needs to be edited so it ships with everything else (I am mainly talking about XCode extensions and capabilities here). I have no idea how it’d work, but it would be great if this process could be automated in one way or another. I get dizzy from having to do this each time I build.. sometimes for extremely small changes (since the built sources are illegible and can’t realistically be changed after they’ve come out of the build server). Anyway, just a thought, thanks again guys
>



### **Shai Almog** — February 2, 2021 at 2:38 am ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24393))

> Shai Almog says:
>
> You can use build hints and native code to customize the project for most intents and purposes e.g. ios.add_libs allows you to add frameworks from xcode. The idea is to get a fully functional app without requiring any customization after the fact.
>



### **Javier Anton** — February 6, 2021 at 11:33 pm ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24401))

> Javier Anton says:
>
> I’m still learning the ropes when it comes to XCode. In a specific example: where in my netbeans project should I put the code with a framework that contains a Share Extension (with my own code) in order for it to be picked up by the ios.add_libs build hint? Is this possible?
>



### **Shai Almog** — February 7, 2021 at 1:58 am ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24403))

> Shai Almog says:
>
> I’m not familiar enough with share extensions but if you need a specific framework to be added just add it to the list in add_libs e.g. ios.add_libs=x.framework;y.framework;mylib.a
>



### **Javier Anton** — February 7, 2021 at 6:21 pm ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24406))

> Javier Anton says:
>
> I shall look into this, thanks


### **Francesco Galgani** — February 2, 2021 at 4:57 pm ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24394))

> Francesco Galgani says:
>
> I use native interfaces extensively (which is the solution to the problem pointed out by Javier Anton). In my opinion, it would be very useful to add support for all Swift libraries and create a documentation on native interfaces that is more extensive and up-to-date than the current one. Wrapping some Android or iOS SDKs in Codename One is really difficult for me, I often go by trial and error and sometimes I’m forced to give up. A practical example is this one, in which I pointed out a problem that I don’t know how to solve (wrapping requires Swift support): <https://stackoverflow.com/questions/65698741/current-support-status-of-webrtc-in-codename-one-and-antmedia-usage>
>
> However, it’s not just a Swift support problem. It also takes very advanced knowledge of how Codename One works, which we simple developers don’t always have.
>



### **Shai Almog** — February 3, 2021 at 3:13 am ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24396))

> Shai Almog says:
>
> We did some deep dive tutorials on that but it’s hard to go deep as the ground is constantly shifting and you end up having to teach the native platforms themselves with all the related complexity. Unfortunately, there aren’t tricks in our quiver to solve that. It’s just hard trial and error until it works with the various native platforms. Ideally if you can package stuff as a POD or dependency then you’re 90% of the way to getting it working and most things should work that way.  
> The best tip is to send a build with “include native source” and build on the native platform then migrate your changes back to Codename One. This isn’t trivial but it gives you a good starting point.
>
> We have an RFE on Swift in the issue tracker if I remember correctly. I’m not sure when we’ll get to it as our issue pipeline is pretty deep and our manpower is heavily committed to some deep tasks. This is also a pretty hard task to implement and will produce a sub-par result since Swift is inherently problematic with VMs due to ARC.
>



### **Javier Anton** — February 6, 2021 at 11:41 pm ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24402))

> Javier Anton says:
>
> What I meant by appending the native sources was adding Targets (Share Extensions, Notification Service/Content Extensions, etc) and modifying core files like delegate/view controller. I don’t think this can be accomplished using native interfaces please correct me if I am wrong. I also don’t know if using Targets will in some way solve your swift problem since each target can have either objc/swift and it all works together
>



### **Shai Almog** — February 7, 2021 at 2:03 am ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24404))

> Shai Almog says:
>
> You can inject sources to various files such as the delegates with build hints. If you look at the source code in git you’ll see various magical comments that generally contain the word “REPLACE” these are special comments that our build servers replace and we can give you a build hint to replace code in that area (some of these build hints are documented).
>



### **Javier Anton** — February 7, 2021 at 6:21 pm ([permalink](https://www.codenameone.com/blog/code-freeze-for-codename-one-7-0.html#comment-24405))

> Javier Anton says:
>
> I know, and thanks. I still need to inject code in areas where there aren’t build hint markers as well as remove existing code. This is mostly due to the fact that I am implementing my own push, so I am probably just an outlier
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
