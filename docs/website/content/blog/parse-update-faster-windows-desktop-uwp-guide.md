---
title: Parse Update, Faster Windows Desktop & UWP Guide
slug: parse-update-faster-windows-desktop-uwp-guide
url: /blog/parse-update-faster-windows-desktop-uwp-guide/
original_url: https://www.codenameone.com/blog/parse-update-faster-windows-desktop-uwp-guide.html
aliases:
- /blog/parse-update-faster-windows-desktop-uwp-guide.html
date: '2016-06-21'
author: Shai Almog
---

![Header Image](/blog/parse-update-faster-windows-desktop-uwp-guide/universal-windows-apps_thumb.jpg)

[Chidiebere Okwudire](https://github.com/sidiabale) of [SMash ICT Solutions](https://www.smash-ict.com/) just  
released [version 3.0 of the parse4cn1 library](https://github.com/sidiabale/parse4cn1/releases/tag/parse4cn1-3.0).  
The biggest feature of which is support for the open source Parse server which should work with some of the  
parse alternatives that popped up to fill the void left by Facebook.

This is great news. In a way I’m more optimistic about the future of Parse than most other MBaaS  
(mobile backend as a service) solutions (e.g. Firebase). Now we have competition and options within  
the Parse space which aren’t as common for other MBaaS solutions.

Having worked with Parse4cn1 in the past I’m pretty excited about this update.

I hope to have a blog post in the near future detailing the migration to new parse servers and working with the  
API in the post Facebook era.

### New Windows Servers & UWP Docs

One somewhat undocumented pleasant result of the new UWP (Universal Windows Platform) support is the fact  
that we needed new Windows based build servers to provide support for UWP. As part of that need we now have  
better (faster and more reliable) servers for Windows desktop builds which should hopefully spend less time in  
the building queue.

While we are on the subject of the UWP support Steve is working on making this target more seamless. There  
are still some issues in the current version when targeting phones and we will probably need to do some work  
to get this to the smoothness enjoyed by other platforms. We will also need to release a new plugin as the  
settings for UWP builds have become far more challenging.

This level of complexity justified a new developer guide section dedicated to UWP, this is still under active  
development but you can follow it on [our website here](/manual/appendix-uwp.html) or in the  
[wiki](https://github.com/codenameone/CodenameOne/wiki/Working-with-UWP)  
that hosts the entire developer guide (which you can  
[help us write/edit](/blog/wiki-parparvm-performance-actionevent-type.html) !).
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — June 23, 2016 at 8:06 am ([permalink](https://www.codenameone.com/blog/parse-update-faster-windows-desktop-uwp-guide.html#comment-22555))

> Chidiebere Okwudire says:
>
> HI Shai, thanks for promptly sharing the parse4cn1 update!
>
> I also agree with you that Parse Server is actually a blessing in disguise. More on that in the upcoming article that you referred to 😉


### **Carlos** — June 23, 2016 at 12:54 pm ([permalink](https://www.codenameone.com/blog/parse-update-faster-windows-desktop-uwp-guide.html#comment-22544))

> Carlos says:
>
> Looking forward to that blog post about parse migration

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
