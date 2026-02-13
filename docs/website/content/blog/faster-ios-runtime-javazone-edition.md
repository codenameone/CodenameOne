---
title: Faster iOS Runtime – JavaZone Edition
slug: faster-ios-runtime-javazone-edition
url: /blog/faster-ios-runtime-javazone-edition/
original_url: https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html
aliases:
- /blog/faster-ios-runtime-javazone-edition.html
date: '2013-09-06'
author: Shai Almog
---

![Header Image](/blog/faster-ios-runtime-javazone-edition/faster-ios-runtime-javazone-edition-1.jpg)

  
  
  
[  
![Content of Shai's Backpack](/blog/faster-ios-runtime-javazone-edition/faster-ios-runtime-javazone-edition-1.jpg)  
](/img/blog/old_posts/faster-ios-runtime-javazone-edition-large-2.jpg)  
  
  

Before we get into the subject of today’s post a small public service announcement: we recently added the ability to create annual pro subscriptions. This provides a 10% discount over our standard pro subscription rates.  
  
  
  
  
I’m writing this while preparing for my JavaZone flight. What you see in the picture on the right is the typical content of my backpack which I carry with me everywhere in case there is a problem I need to debug, this is somewhat of a visual tutorial of “what it takes” to be a mobile developer today. I took this picture for the JavaZone presentation I’m making and I think it illustrates well why Codename One exists (BTW it is missing some of my testing devices such as the iPad 2, the Android tablets and a few J2ME phones). From left to right top to bottom:  

  1.   
Windows 8 Machine (for building Windows Phone apps)  

  2.   
iPad 3 (retina – iOS 7 beta)  

  3.   
Mac (for building iOS apps)  

  4.   
Blackberry Z10 (BB OS 10)  
  

  5.   
Nokia Asha (S40)  

  6.   
iPod Touch (iOS 6)  

  7.   
Nexus One (Gingerbread)  

  8.   
Galaxy Nexus (Jelly Bean)  

  9.   
Blackberry Torch (BB OS 6)  

  10.   
Nokia Lumia 520 (Windows Phone 8)  
  

  
  
Who said mobile development isn’t back breaking work!  
  
  
  
  
  
And now for something completely different: On iOS the translation tool we use for converting bytecode to xcode applications is tuned for compatibility more than it is tuned for speed. This means that generated code performs a lot of null pointer checks (so it can throw a NullPointerException) and performs array boundary checks. Both of these effectively slow the execution of your application significantly.  

  
We now have a new build flag for iOS called:  
  
ios.unsafe  
  
  
If you define that build argument as true:  
  
ios.unsafe=true  
  
  
You will get an application that won’t throw ArrayIndexOutOfBounds exceptions or NullPointerExceptions, however it might crash for such cases!  
  
  
  
This is one of those flags that you will need to test REALLY well before using in production, however once enabled it should drastically improve the performance of Codename One.  

  
Notice that some things are never tested by the translated code such as class cast exception etc. (so code that relies on those will just crash).  

  
We are now in the process of re-designing our iOS backend which includes some tough decisions regarding backend technology, our goals are:  
  

  1.   
Shortening build times.  
  
  

  2.   
Improved crash analysis  
  
  
.  
  
  
  

  3.   
Faster execution.  
  
  
  

  4.   
Improved graphics support.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — September 9, 2013 at 8:55 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21957))

> Anonymous says:
>
> how much weight this total stuff 😉 ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — September 20, 2013 at 8:33 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-22023))

> Anonymous says:
>
> If we don’t use the ‘unsafe’ flag, do unnecessary null pointer checks get optimised out anyway? 
>
> If the Java->C conversion just converts the opcodes, you’ll get a lot of unnecessary ones. e.g. References to ‘this’ will still have the check, as will references returned from the ‘new’ operator. I think you can also cut out quite a lot by only checking references stored in local variables once after each assignment. 
>
> I think you can make considerable speed savings with those techniques, and they aren’t that hard to implement. Importantly, you don’t change the semantics of the code at all…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — September 20, 2013 at 2:17 pm ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21930))

> Anonymous says:
>
> Back breaking weight…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — September 20, 2013 at 2:18 pm ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21628))

> Anonymous says:
>
> True, the main cost though is in loops.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — September 23, 2013 at 5:50 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21980))

> Anonymous says:
>
> Yes, there are a few things you can do to speed loops up, though mostly only when you’re using final/local variables due to potential threading issues. 
>
> Anyway, my question was: do you do any of those optimisations when ios.unsafe = false?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — September 23, 2013 at 8:56 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21965))

> Anonymous says:
>
> No. The only thing the unsafe flag does is remove the checks, most of the optimizations in the LLVM compiler from Apple are actually pretty great although we can/should definitely improve threading/monitors. 
>
> I’m now in the process of overhauling our entire VM/graphics infrastructure so I’m looking into improvements there as well. 
>
> (Replying to myself since blog software supports limited nesting)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — January 10, 2014 at 12:07 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21959))

> Anonymous says:
>
> So what happens when things crash with this flag set? App just vanishes with no message? Im going to turn it on right now!!!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — January 10, 2014 at 12:12 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-21882))

> Anonymous says:
>
> ps – does this mean it will run faster than Android? or not ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)


### **Anonymous** — January 10, 2014 at 2:50 am ([permalink](https://www.codenameone.com/blog/faster-ios-runtime-javazone-edition.html#comment-22056))

> Anonymous says:
>
> It will crash. You can disable exception messages without using this by defining an error handler. 
>
> Android has a JIT, static compilation can’t beat JIT for some use cases.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffaster-ios-runtime-javazone-edition.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
