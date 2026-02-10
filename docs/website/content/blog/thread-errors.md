---
title: Thread Errors
slug: thread-errors
url: /blog/thread-errors/
original_url: https://www.codenameone.com/blog/thread-errors.html
aliases:
- /blog/thread-errors.html
date: '2020-04-10'
author: Shai Almog
---

![Header Image](/blog/thread-errors/error-detected.jpg)

I wrote before about [EasyThread](https://www.codenameone.com/blog/easy-thread.html) which makes it much easier to write multi-threaded code in Codename One. One problem in that scenario was the inability to define a generic exception handler for that scenario.

With the current version of Codename One we now have a new generic error handling API for easy threads:
    
    
    public void addErrorListener(ErrorListener err);
    public static void addGlobalErrorListener(ErrorListener err);

These methods add a callback for error events, either globally or for a specific thread. Notice that these methods aren’t thread safe and should be invoked synchronously. So make sure to invoke them only from one thread e.g. the EDT.

These methods must never be invoked from within the resulting callback code!

So you can’t do this:
    
    
    t.addErrorListener((t, c, e) -> {
       // do stuff ...
    
       // this is illegal:
       t.removeErrorListener(listener);
    });

The error listener interface looks like this:
    
    
    public static interface ErrorListener<T> {
        /**
         * Invoked when an exception is thrown on an easy thread. Notice
         * this callback occurs within the thread and not on the EDT. This
         * method blocks the current easy thread until it completes.
         * @param t the thread
         * @param callback the callback that triggered the exception
         * @param error the exception that occurred
         */
        void onError(EasyThread t, T callback, Throwable error);
    }

This should provide you with all the details you need to handle an error in a generic way.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — April 14, 2020 at 9:42 am ([permalink](https://www.codenameone.com/blog/thread-errors.html#comment-21392))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> Great stuff. Is this wrapping an exception handler around the execution code or is it wrapping a threading exception around the EasyThread?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fthread-errors.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
