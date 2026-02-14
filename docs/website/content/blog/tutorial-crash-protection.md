---
title: Tutorial – Crash Protection
slug: tutorial-crash-protection
url: /blog/tutorial-crash-protection/
original_url: https://www.codenameone.com/blog/tutorial-crash-protection.html
aliases:
- /blog/tutorial-crash-protection.html
date: '2017-09-13'
author: Shai Almog
---

![Header Image](/blog/tutorial-crash-protection/learn-codenameone-2.jpg)

Continuing the trend I revisited the old crash protection video in the “how do I” section and updated it with current information and details. The old video still featured an old flag that should be avoided…​  
The new video is relatively short and simple as the feature isn’t very complex, I hope to produce several more of these and bolster the video section further.

On a different subject I also neglected to mention the new security related module in the [Codename One Academy courses](https://codenameone.teachable.com/p/build-real-world-full-stack-mobile-apps-in-java). I’ll drop additional videos there later in the month.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — September 23, 2017 at 10:27 pm ([permalink](https://www.codenameone.com/blog/tutorial-crash-protection.html#comment-23777))

> Francesco Galgani says:
>
> Thank you for the two new lessons about security. Is it possible to receive an e-mail every time that you add a new lesson in the Codename One Academy?
>



### **Francesco Galgani** — October 18, 2017 at 10:21 am ([permalink](https://www.codenameone.com/blog/tutorial-crash-protection.html#comment-23810))

> Francesco Galgani says:
>
> (This is the second time that I try to submit this comment)
>
> About crash protection and error reporting, I have two questions:
>
> 1\. Is there a code that automatically catches (and reports by e-mail) all exceptions in all threads, not only in the EDT?
>
> 2\. In the manual, section “Logging & Crash Protection”, at the page: [https://www.codenameone.com…](<https://www.codenameone.com/manual/files-storage-networking.html#_logging_crash_protection>) , there are the following two lines of code: I don’t understood if it makes sense use them in conjunction with Log.bindCrashProtection(true)
>
> Log.setReportingLevel(Log.REPORTING_DEBUG);  
> DefaultCrashReporter.init(true, 2);
>
> Thank you for any clarification.
>



### **Shai Almog** — October 19, 2017 at 5:50 am ([permalink](https://www.codenameone.com/blog/tutorial-crash-protection.html#comment-23826))

> Shai Almog says:
>
> Sorry I missed that. No there is no standard way to do that without bothering everyone unfortunately. I will however post with updates to the blog occasionally.
>



### **Shai Almog** — October 19, 2017 at 5:54 am ([permalink](https://www.codenameone.com/blog/tutorial-crash-protection.html#comment-23833))

> Shai Almog says:
>
> A comment goes directly to moderation if you include a link which disqus sometimes thinks are just strings with dots e.g. a stack trace is “links”.
>
> 1\. If you use the bind method this will happen implicitly and also send an email for other threads. I think I mentioned that in the video. I’d recommend using Display.startThread() instead of new Thread() to guarantee that.
>
> 2\. The crash reporter sends an email every couple of minutes with the content of the log. It’s an approach we have phased out as we catch errors more effectively now thanks to the new changes in the iOS VM.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
