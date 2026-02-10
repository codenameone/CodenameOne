---
title: iOS Http URL's
slug: ios-http-urls
url: /blog/ios-http-urls/
original_url: https://www.codenameone.com/blog/ios-http-urls.html
aliases:
- /blog/ios-http-urls.html
date: '2016-06-15'
author: Shai Almog
---

![Header Image](/blog/ios-http-urls/xcode-migration.jpg)

We’ll be [migrating to the new iOS build servers](/blog/ios-server-migration-plan.html)  
this Sunday & this does entail one major thing you need to be aware of. With the new version of xcode http  
URL’s [are blocked by Apple](/blog/hiding-url-security-advocacy.html). We  
blogged about this a while back but this bares repeating as it’s something a lot of you will start running into.

To get an overview of the issue check out  
[this article](http://code.tutsplus.com/articles/apple-tightens-security-with-app-transport-security—​cms-24420) or  
the [actual document from Apple](https://developer.apple.com/library/mac/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html).  
In a nutshell http URL’s are no longer supported by Apple to facilitate proper security.

You can disable this block by using the build hint  
`ios.plistInject=<key>NSAppTransportSecurity</key><dict><key>NSAllowsArbitraryLoads</key><true/></dict>`  
but if you don’t have a good reason to do this Apple will reject your app and won’t let you ship your app  
thru the appstore.

Be sure to update your apps!

Starting with this update our simulator will print out a warning every time you try to connect to an HTTP URL  
to help you detect the cases where you do so. Hopefully this will make the migration to the new servers smoother.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gareth Murfin** — June 16, 2016 at 5:02 pm ([permalink](https://www.codenameone.com/blog/ios-http-urls.html#comment-22893))

> Gareth Murfin says:
>
> So HttpS will be fine right?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fios-http-urls.html)


### **Shai Almog** — June 17, 2016 at 3:40 am ([permalink](https://www.codenameone.com/blog/ios-http-urls.html#comment-22827))

> Shai Almog says:
>
> That’s the goal. To force apps to use proper https sites and proper security.  
> I understand the logic here. In a browser you can see the lock icon on the top left and know if you are submitting to a secure website. In an app there is no such indication.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fios-http-urls.html)


### **Eric Coolman** — July 1, 2016 at 11:24 pm ([permalink](https://www.codenameone.com/blog/ios-http-urls.html#comment-22704))

> Eric Coolman says:
>
> ATS minimum defaults: HTTPS + TLS 1.2 + FS
>
> [https://developer.apple.com…](<https://developer.apple.com/library/ios/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html>)
>
> So far we haven’t been flagged for enabling arbitrary loads support with our banking apps, but I’m sure Apple will start clamping down soon.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fios-http-urls.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
