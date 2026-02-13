---
title: Peering Revisited
slug: peering-revisited
url: /blog/peering-revisited/
original_url: https://www.codenameone.com/blog/peering-revisited.html
aliases:
- /blog/peering-revisited.html
date: '2016-07-05'
author: Shai Almog
---

![Header Image](/blog/peering-revisited/native-peer-revisited.png)

I blogged about  
[peer components](/blog/understanding-peer-native-components-why-codename-one-is-so-portable.html)  
all the way back in 2014 trying to explain basic facts about their limitations/behaviors. A lot of those limitations  
are inherent but over the past year or so we’ve been thinking more and more about the z-order limitation.

As part of that train of thought I filed [this issue](https://github.com/codenameone/CodenameOne/issues/1758)  
with a few suggestions about working around some of those limitations. I think these approaches would work really  
well on iOS which from my experience is more “amiable” to such hacks. Android is a different beast though.  
Android’s rendering logic is a weird hackish nightmare filled with bugs and lore…​

So naturally I picked Android first to experiment with and as you can see from the screenshot above, the button  
and labels are Codename One widgets…​

This is still buggy and there is no guarantee we’ll be able to bring this to production grade but I’m generally optimistic  
that this is a doable task that opens up Codename One to a HUGE set of applications in media/mapping etc.  
that up until now required way too much native code to work properly.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
