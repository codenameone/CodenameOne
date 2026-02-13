---
title: Don't Touch That Code
slug: dont-touch-that-code
url: /blog/dont-touch-that-code/
original_url: https://www.codenameone.com/blog/dont-touch-that-code.html
aliases:
- /blog/dont-touch-that-code.html
date: '2017-10-17'
author: Shai Almog
---

![Header Image](/blog/dont-touch-that-code/generic-java-2.jpg)

Last week scrolling broke and we had a few relatively complex regressions. This can be traced back to a change we did to the `getComponentAt(x, y)` method, this change in itself fixed a problematic bug but triggered far worse bugs and we just had to revert the whole thing…​

So why did we even do a change to a method that’s so deep in the code and so risky?

The logic behind that is to prevent situations like this. `getComponentAt(x, y)` is a remarkably important method which is used all over the place to implement touch interface logic. Unfortunately, as one of those methods that evolved it became “unwieldy” and because it’s so deep we tried to avoid changing it…​

> Don’t touch it! It works! 

— Hacker Culture 

These types of methods exist in every project and they form code rot around them. That’s why we try to avoid that mentality and try to improve on the things that are working. Sometimes in cases such as this, we fail. But more often than not we succeed and that is truly important.

### Why this Method?

As I mentioned before `getComponentAt(x, y)` is used internally all over the place. The benefit of code reuse goes well beyond basic aesthetics and code reduction. It guarantees better test coverage, increased stability and performance as the compiler & optimizations can do much more.

However, that also means a method can become a point of hacks that impact everyone. So now that we reverted the change we’ll need to go over every use case and surgically remove the usage of this method so we can understand why these regressions happened and hopefully make the code more sensible.

Unfortunately, this delays our planned migration to the on-top side menu which we were hoping to do this week. Right now the menu just doesn’t work properly with peer components which was the exact fix we aimed at here.

### What does that Mean?

It means we might not be able to finish everything we wanted for the 3.8 release and need to move even more things to 3.9. Accessibility is something I specifically wanted to deliver for 3.8 but it’s a non-trivial feature so it probably won’t make it with our existing commitments.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
