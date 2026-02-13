---
title: Windows Phone 8.1 & UWP Support
slug: windows-phone-8-1-uwp-support
url: /blog/windows-phone-8-1-uwp-support/
original_url: https://www.codenameone.com/blog/windows-phone-8-1-uwp-support.html
aliases:
- /blog/windows-phone-8-1-uwp-support.html
date: '2016-05-01'
author: Shai Almog
---

![Header Image](/blog/windows-phone-8-1-uwp-support/universal-windows-apps_thumb.jpg)

[Fabricio](https://twitter.com/ravnos_kun) just  
[submitted a pull request](https://github.com/codenameone/CodenameOne/pull/1757) that Steve  
merged to provide support for Windows Phone 8.1 in our new UWP (Universal Windows Platform) port of Codename One.

This is huge news as it means we can fully migrate to the new port without leaving developers behind!

This means we’ll be able to make the migration to the [new Windows port](/blog/new-windows-port.html) quicker  
and throw away the old port without losing much.

### New Migration Plan

Up until now we were working under the assumption that UWP would be similar to Windows Phone in terms of  
authorization and build process. This doesn’t seem to be the case.

UWP requires a certificate file to authorize a build similarly to iOS/Android builds, this is probably a good thing  
as it might mean that sideloading over the air might finally be supported on Windows devices.

__ |  Windows Phone is the only mobile platform we worked with that didn’t support installing files over the air!   
---|---  
  
This means we will need to make some changes to all IDE plugins and to the build.xml file in order to support the  
UWP target, these changes will allow us to make the migration process much smoother and phase out the old  
Windows phone target gradually. This also means the Windows section in the IDE will also include more options  
such as the certificate required for UWP. We’ll keep you posted on this process as we setup the build servers  
and get the IDE plugin updates out of the door.

### When will this be Out?

This is probably the main question here and we still don’t have a final answer.

Until we start seeing builds going thru it’s hard to say what would work and what wouldn’t. We had a big issue with  
iKVM this past week and needed help from Microsoft to resolve that (they were quite helpful here). We have  
no way of knowing if this is the “last issue” we will run into. Setting up a Windows build server is the equivalent  
of a root canal without the pleasant bits or anesthesia!

We literally had to get Microsoft tech support to help with basic things and are still struggling to get the build  
tools to run there.

We hope to have a beta version out for developers before the 3.6 release which is currently slated to August. However,  
this is a tentative date and until we actually see everything working for elaborate apps we won’t commit to anything.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — May 2, 2016 at 1:40 pm ([permalink](https://www.codenameone.com/blog/windows-phone-8-1-uwp-support.html#comment-22798))

> Chidiebere Okwudire says:
>
> This is good progress! Hopefully things will work according to schedule (read: pleasant bits and anesthesia for the rest of the ‘surgery’, haha) and we’ll finally be able to confidently make decent WP apps.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-8-1-uwp-support.html)


### **Gareth Murfin** — May 3, 2016 at 11:27 am ([permalink](https://www.codenameone.com/blog/windows-phone-8-1-uwp-support.html#comment-22756))

> Gareth Murfin says:
>
> This sounds promising, although I have not targeted win phone because I assumed their market share was almost 0 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-8-1-uwp-support.html)


### **Shai Almog** — May 4, 2016 at 5:04 am ([permalink](https://www.codenameone.com/blog/windows-phone-8-1-uwp-support.html#comment-22729))

> Shai Almog says:
>
> I think Windows Phone is dead but some companies need to support “everything”. The value isn’t Windows on phones as much as on tablets/PC’s which the UWP already supports. Windows 10 store was already available on 250M devices quite a while back so it’s one of the bigger target markets out there and has a strong enterprise presence.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-8-1-uwp-support.html)


### **Lana** — March 27, 2017 at 1:48 am ([permalink](https://www.codenameone.com/blog/windows-phone-8-1-uwp-support.html#comment-23170))

> Lana says:
>
> Hi Shai,
>
> I looked it up but couldn’t keep track, are windows phone builds supported or not? It seems that our builds fail but I can’t tell if that’s a bug or if you aren’t supporting windows phone builds for lack of corporate clients.  
> It’d be very cool if you could write a one-line summary about whether it’s currently supported or not on the main page.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-8-1-uwp-support.html)


### **shannah78** — March 27, 2017 at 5:28 pm ([permalink](https://www.codenameone.com/blog/windows-phone-8-1-uwp-support.html#comment-21574))

> shannah78 says:
>
> WP 8.1 is not supported. WP 10 (UWP) is supported.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwindows-phone-8-1-uwp-support.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
