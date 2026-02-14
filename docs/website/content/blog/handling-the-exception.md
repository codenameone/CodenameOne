---
title: Handling The Exception
slug: handling-the-exception
url: /blog/handling-the-exception/
original_url: https://www.codenameone.com/blog/handling-the-exception.html
aliases:
- /blog/handling-the-exception.html
date: '2013-07-07'
author: Shai Almog
---

![Header Image](/blog/handling-the-exception/handling-the-exception-1.png)

  
  
  
[  
![Exception](/blog/handling-the-exception/handling-the-exception-1.png)  
](/img/blog/old_posts/handling-the-exception-large-2.png)  
  
  

Handling errors or exceptions in a deployed product is pretty difficult, most users would just throw away your app and some would give it a negative rating without providing you with the opportunity to actually fix the bug that might have happened. 

  
Google improved on this a bit by allowing users to submit stack traces for failures on Android devices but this requires the users approval for sending personal data which you might not need if you only want to receive the stack trace and maybe some basic application state (without violating user privacy).  

  
For quite some time Codename One had a very powerful feature that allows you to both catch and report such errors, the error reporting feature uses the Codename One cloud which is exclusive for pro/enterprise users. Normally in Codename One we catch all exceptions on the EDT (which is where most exceptions occur) and just display an error to the user   
  
as you can see in the picture. Unfortunately this isn’t very helpful to us as developers who really want to see the stack, furthermore we might prefer the user doesn’t see an error message at all!

  
Codename One allows us to grab all exceptions that occur on the EDT and handle them using the method  
  
addEdtErrorHandler in the Display class. Adding this to the Log’s ability to report errors directly to us and we can get a very powerful tool that will send us an email with information when a crash occurs!  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — April 5, 2016 at 9:51 pm ([permalink](https://www.codenameone.com/blog/handling-the-exception.html#comment-21483))

> Chidiebere Okwudire says:
>
> Is it possible to send log to another email address than the developer’s?
>



### **Shai Almog** — April 6, 2016 at 2:41 am ([permalink](https://www.codenameone.com/blog/handling-the-exception.html#comment-22447))

> Shai Almog says:
>
> No. A lot of features are locked directly to that email account.  
> It’s a problem to accept an email dynamically for some services as they might be handled in complex ways e.g. sending an email from a production app. So it’s important to us that one valid email is used and that an email is used per developer to avoid abuse.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
