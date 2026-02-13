---
title: Should we use Gradients?
slug: should-we-use-gradients
url: /blog/should-we-use-gradients/
original_url: https://www.codenameone.com/blog/should-we-use-gradients.html
aliases:
- /blog/should-we-use-gradients.html
date: '2016-04-24'
author: Shai Almog
---

![Header Image](/blog/should-we-use-gradients/gradients.png)

With the latest version of the Android port we fixed a long running  
[bug in gradient drawing](https://github.com/codenameone/CodenameOne/issues/1696) on Android.  
Gradients should now work correctly and will also be performant potentially even faster than images on Android.  
Our standing recommendation is to  
[avoid gradients](/how-do-i---improve-application-performance-or-track-down-performance-issues.html)  
as they pose a memory/performance penalty on most platforms  
and so this change raises the question of using gradients back into the forefront.

We gave this some thought and decided to keep our existing recommendation to avoid gradients in favor of  
images but soften it. Performance of gradients isn’t tested as extensively across platforms, we don’t use them  
at all in our themes and so they are just not as robust as other elements.

In order to get gradients to perform we implemented them natively on the host OS’s which means they might look  
different on the target device. This might work reasonably well but might pose issues in some cases.

To make matters worse our gradients are really trivial in terms of functionality. They don’t support orientation, breaks,  
alpha, paint (shapes) or a multitude of other capabilities that make gradients into a powerful tool.

### So Why Fix Gradients?

We’d like to fix gradients completely, currently our graphics capabilities still lag behind those of Java2D/FX. That’s  
not a horrible place to be in as both of these API’s weren’t designed for todays mobile environments and their  
oddities.

__ |  This is true for the most part but we do have at least one thing which Java2D doesn’t have: perspective transform   
---|---  
  
Eventually we think the road for API maturity is paved with small steps, fixing gradients today is just such a step  
in a long road that might lead to gradient filled shapes with complex gradient rules. At that point the decision between  
using a gradient and an image might not be as clear cut.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
