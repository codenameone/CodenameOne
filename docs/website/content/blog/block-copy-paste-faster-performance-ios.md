---
title: Block Copy/Paste & Faster Performance on iOS
slug: block-copy-paste-faster-performance-ios
url: /blog/block-copy-paste-faster-performance-ios/
original_url: https://www.codenameone.com/blog/block-copy-paste-faster-performance-ios.html
aliases:
- /blog/block-copy-paste-faster-performance-ios.html
date: '2017-02-06'
author: Shai Almog
---

![Header Image](/blog/block-copy-paste-faster-performance-ios/security.jpg)

I discussed both of these last week but we’ve made some progress that warrants a dedicated post. We added a new feature that allows you to [block copy & paste](/blog/disable-screenshots-copy-paste.html) on a text component either globally or on a case by case basis. This is helpful for highly sensitive applications.

This feature was previously restricted to Android but is now available to iOS as well with no change required to your code.

### Build Performance

What started as a bunch of optimizations to fix [issue 2024](https://github.com/codenameone/CodenameOne/issues/2024) evolved to a set of optimizations that should make the generated iOS code more readable, smaller & faster. This took some twists and turns and for now we reverted this set of changes until next Friday.

However, the end result is that this should also shorten build times noticeably although not for everyone…​

The `clang` compiler is very slow when dealing with large methods and macros. By optimizing away some of the more ASM oriented conventions of the bytecode and substituting them with direct variable/constant usage we reduced some overheads.

If you weren’t directly impacted by this you probably won’t see any performance impact. The speed of the JVM rarely factors into the performance of Codename One which is governed more by the speed of the native port than anything else.  
One of the things to notice in the issue discussion is that the original optimization fixed the problem while causing worse compilation times for different code pathways. That’s why optimizations & fixes are so tricky in a product as big as Codename One as regressions can be very complicated.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **M Usman Nu** — February 10, 2017 at 1:52 pm ([permalink](https://www.codenameone.com/blog/block-copy-paste-faster-performance-ios.html#comment-23327))

> M Usman Nu says:
>
> Do i need to add ios.disableScreenshots in built-hints in order to enable this feature ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fblock-copy-paste-faster-performance-ios.html)


### **Shai Almog** — February 11, 2017 at 5:57 am ([permalink](https://www.codenameone.com/blog/block-copy-paste-faster-performance-ios.html#comment-23098))

> Shai Almog says:
>
> No, that has nothing to do with that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fblock-copy-paste-faster-performance-ios.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
