---
title: Static Global Context
slug: static-global-context
url: /blog/static-global-context/
original_url: https://www.codenameone.com/blog/static-global-context.html
aliases:
- /blog/static-global-context.html
date: '2017-05-29'
author: Shai Almog
---

![Header Image](/blog/static-global-context/new-features-1.jpg)

A developer recently asked me why `Display` is called `Display` when it has such a broad purpose?  
The reason is historic with roots in Codename One’s origin back in 2007, when we formed the company Chen advocated for a rename of that class and I disagreed. In retrospect I was wrong, the name doesn’t work.

This isn’t something we can easily fix but it’s something we can replace and improve…​

### Static Context

When we released Codename One’s first beta almost everything was different but two huge differences matter for this post:

  * We only supported JDK 1.3 era CLDC subset

  * We used XMLVM which didn’t implement static methods very efficiently

Both of these are no longer correct. Under ParparVM static methods perform better than instance methods (as they should) and we always use Java 8 syntax. This means that classes like `Display` which works as a singleton are at a disadvantage when compared to a class where all the methods are static. In such a class the performance will be faster:

  * We remove the `getInstance()` call

  * A single static call is faster than an instance method call

But there is another advantage of shorter syntax, e.g. instead of doing something like:
    
    
    int width = Display.getInstance().getDisplayWidth();

We could (theoretically) do:
    
    
    int width = Display.getDisplayWidth();

But that’s not all, static methods have the advantage of newer Java static import syntax which allows us to add one import statement:
    
    
    import static com.codename1.ui.Display.*;

Then write something like:
    
    
    int width = getDisplayWidth();

### Why do it in Display?

Adding something like this into `Display` doesn’t make sense…​ It would make that class huge and just persist a design mistake from years ago. It’s better to start with a new global context class that will provide us static methods for all the common things we need where it’s common constants or `NetworkManager` methods etc.

This doesn’t deprecate `Display` (yet), it provides a better approach for doing the things we do today in `Display` with the new `CN` class. It also adds common methods & constants from several other classes so Codename One code will feel more terse e.g. once we do:
    
    
    import static com.codename1.ui.CN.*;

__ |  That’s optional, if you don’t like static imports you can just write `CN.` for every element   
---|---  
  
From that point on you can write code that looks like this:
    
    
    callSerially(() -> runThisOnTheEDT());

Instead of:
    
    
    Display.getInstance().callSerially(() -> runThisOnTheEDT());

The same applies for most network manager calls e.g.:
    
    
    addToQueue(myConnectionRequest);

Some things were changed so we won’t have too many conflicts e.g. `Log.p` or `Log.e` would have been problematic so we now have:
    
    
    log("my log message");
    log(myException);

Instead of `Display.getInstance().getCurrent()` we now have `getCurrentForm()` since `getCurrent()` is too generic. But for most methods you should just be able to remove the `NetworkManager` or `Display` access and it should “just work”.

I ported the Kitchen Sink to use this new convention, to see a sample of how this can cut down on code clutter check of this [diff of my commit](https://github.com/codenameone/KitchenSink/commit/6c4ed67579ec8cbbe3a3da67d30224688c9fe602).

The motivation for this change is three fold:

  * Terse code

  * Small performance gain

  * Cleaner API without some of the baggage in `Display` or `NetworkManager`

Both of these classes will probably be around and won’t be deprecated any time soon.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Diamond** — May 30, 2017 at 3:13 pm ([permalink](https://www.codenameone.com/blog/static-global-context.html#comment-23499))

> Diamond says:
>
> Great improvement! How about changing CN to CN1 to actually read more like the platform name?…since this is a major class most CN1 developers will be calling to perform some generic tasks.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fstatic-global-context.html)


### **Shai Almog** — May 31, 2017 at 4:41 am ([permalink](https://www.codenameone.com/blog/static-global-context.html#comment-23059))

> Shai Almog says:
>
> I actually started with CN1 as I had a similar thought process. After wrestling with it a bit I eventually settled on CN. My reasoning for this is 4 fold:
>
> 1\. Not a fan of numbers in class names.  
> 2\. It’s slightly shorter which isn’t much but still…  
> 3\. It’s not about the brand name it’s just a class name and calling it CN keeps it simple  
> 4\. We might change our name under a future rebrand. E.g. on iOS NSString is cemented because of NextStep. I don’t mind having a CN class to mark the history but a CN1 class might be too much.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fstatic-global-context.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
