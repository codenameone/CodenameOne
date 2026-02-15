---
title: Easy Thread
slug: easy-thread
url: /blog/easy-thread/
original_url: https://www.codenameone.com/blog/easy-thread.html
aliases:
- /blog/easy-thread.html
date: '2017-05-08'
author: Shai Almog
---

![Header Image](/blog/easy-thread/new-features-5.jpg)

Working with threads is usually ranked as one of the least intuitive and painful tasks in programming. This is such an error prone task that some platforms/languages took the route of avoiding threads entirely. I needed to convert some code to work on a separate thread but I still wanted the ability to communicate and transfer data from that thread.

This is possible in Java but non-trivial, the thing is that this is relatively easy to do in Codename One with tools such as `callSerially` I can let arbitrary code run on the EDT. Why not offer that to any random thread?

That’s why I created `EasyThread` which takes some of the concepts of Codeame One’s threading and makes them more accessible to an arbitrary thread. This way you can move things like resource loading into a separate thread and easily synchronize the data back into the EDT as needed…​

Easy thread can be created like this:
    
    
    EasyThread e = EasyThread.start("ThreadName");

You can just send a task to the thread using:
    
    
    e.run(() -> doThisOnTheThread());

But it gets better, say you want to return a value:
    
    
    e.run((success) -> success.onSuccess(doThisOnTheThread()), (myResult) -> onEDTGotResult(myRsult));

Lets break that down…​ We ran the thread with the success callback on the new thread then the callback got invoked on the EDT as a result. So this code `(success) → success.onSuccess(doThisOnTheThread())` ran off the EDT in the thread and when we invoked the `onSuccess` callback it sent it asynchronously to the EDT here: `(myResult) → onEDTGotResult(myRsult)`.

These asynchronous calls make things a bit painful to wade thru so instead I chose to wrap them in a simplified synchronous version:
    
    
    EasyThread e = EasyThread.start("Hi");
    int result = e.run(() -> {
        System.out.println("This is a thread");
        return 3;
    });

There are a few other variants like `runAndWait` and there is a `kill()` method which stops a thread and releases its resources.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chad Elofson** — May 10, 2017 at 5:14 am ([permalink](/blog/easy-thread/#comment-23513))

> Chad Elofson says:
>
> That’s pretty slick!


### **Javier Anton** — February 18, 2020 at 3:51 pm ([permalink](/blog/easy-thread/#comment-21384))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Really good, but perhaps in the future a method similar to interrupt() could be added to cancel all the pending tasks (since kill() doesn’t cancel pending tasks)
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
