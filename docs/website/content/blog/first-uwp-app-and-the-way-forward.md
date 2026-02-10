---
title: First UWP App and the Way Forward
slug: first-uwp-app-and-the-way-forward
url: /blog/first-uwp-app-and-the-way-forward/
original_url: https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html
aliases:
- /blog/first-uwp-app-and-the-way-forward.html
date: '2016-09-04'
author: Shai Almog
---

![Header Image](/blog/first-uwp-app-and-the-way-forward/solitaire-uwp.jpg)

The UWP (Universal Windows Platform) port is finally stable enough to get an  
[app into the Microsoft store](https://www.microsoft.com/en-us/store/p/codename-one-solitaire/9nblggh51z60).  
Steve published out Solitaire demo into the Microsoft appstore and it passed thru the whole process. You  
can download it, install it on your device and try it.

We’ll try to setup a company account to publish the kitchen sink as well moving forward.

This is a huge step for the UWP port showing its maturity and readiness for prime-time.

### Future of Windows Phone Port

We weren’t bullish on the continued maintenence of the Windows Phone port but with recent events e.g. MS discontinuing  
Skype on Windows Phone it is pretty clear the platform is dead. Had the port been mature this wouldn’t have been a  
problem, but the Windows Phone port has huge glaring problems…​

Fixing these problems is an enormous task and one that just isn’t viable for a discontinued platform. So we are officially  
deprecating the Windows Phone target and will mark it as deprecated starting with the next release.

At the moment we won’t remove it since we know people are still using it but we might do so in the near future as  
we need to self host the servers to support this platform and that is a very problematic setup. These problems don’t  
affect the UWP port!
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **bryan** — September 5, 2016 at 8:30 pm ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-22735))

> bryan says:
>
> Overflow menu commands don’t display correctly
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **Shai Almog** — September 6, 2016 at 3:56 am ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-23006))

> Shai Almog says:
>
> Thanks, we noticed that too after this went live. It’s an odd issue.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **bryan** — September 6, 2016 at 3:59 am ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-22751))

> bryan says:
>
> It’s the same issue I raised a few days ago. It works fine in a debug build, but not in an app store build.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **Ross Taylor** — September 6, 2016 at 12:51 pm ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-22971))

> Ross Taylor says:
>
> Strange afaik, Skype will be discontinued for Windows phones 7 – 8.1, but nothing was mentioned about Windows Phone 10?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **Shai Almog** — September 6, 2016 at 1:30 pm ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-22946))

> Shai Almog says:
>
> MS’s lineup is a confusing mess.
>
> There is no Windows Phone 10 and there never will be.
>
> There is Windows 10 Mobile which is really a shrunk down version of Windows 10. To reach both Windows 10 (Desktop & Mobile) MS invented the Universal Windows Platform == UWP.
>
> So we support the Windows 10+ versions that run on phones, desktops and tablets. We are ending support for the exact same thing that Skype is no longer supporting.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **Ross Taylor** — September 6, 2016 at 1:49 pm ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-23021))

> Ross Taylor says:
>
> Ah, thanks clearing things up! Good to know CN1 is making effort to reach all platforms.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **Jaanus Hansen** — September 11, 2016 at 11:01 am ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-22953))

> Jaanus Hansen says:
>
> Very cool news, but why images are so blurry on Desktop?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)


### **Shai Almog** — September 12, 2016 at 4:20 am ([permalink](https://www.codenameone.com/blog/first-uwp-app-and-the-way-forward.html#comment-22947))

> Shai Almog says:
>
> I think that’s due to the apps resources. When I originally wrote the app I had a set of cards at a given resolution. It would have meant quite a bit of work to do a new high DPI deck.
>
> Notice that the toolbar icons aren’t blurry.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-uwp-app-and-the-way-forward.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
