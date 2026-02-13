---
title: Better Error Logging
slug: better-error-logging
url: /blog/better-error-logging/
original_url: https://www.codenameone.com/blog/better-error-logging.html
aliases:
- /blog/better-error-logging.html
date: '2019-04-15'
author: Shai Almog
---

![Header Image](/blog/better-error-logging/debugging.jpg)

A common pain point in most GUI frameworks is the hidden stack traces. When we have an app in production we get occasional emails from crash protection which are cryptic and hard to figure out. They usually start with the EDT loop and make no sense.

The reason for that is `callSerially()`. When we have code that invokes `callSerially` we essentially lose the previous stack trace. So your stack trace would look roughly like this:
    
    
    java.lang.RuntimeException:
            at com.mycompany.MyClass.myMethod(MyClass.java:400)
            at com.codename1.ui.Display.edtLoopImpl(Display.java:1166)
            at com.codename1.ui.Display.mainEDTLoop(Display.java:1070)
            at com.codename1.ui.RunnableWrapper.run(RunnableWrapper.java:120)
            at com.codename1.impl.CodenameOneThread.run(CodenameOneThread.java:176)

For most cases you can just fix line 400 of `MyClass` so it won’t throw an exception but you might be hiding a worse bug. Lets say that line fails because it expects a specific condition to exist in the app and that condition isn’t met. A good example for this would be logged in users. Lets say your app expects the user to be logged in before `myMethod` is invoked but for some reason he isn’t.

That means the real bug occurred elsewhere probably in the area of code where `callSerially() → myClass.myMethod(;` was called. Lets say you looked over the entire body of code and you have suspects but can’t tell which part is at fault. Narrowing this down would help…​

That’s where `Display.setEnableAsyncStackTraces()` comes in. When set to true it creates a “fake” exception for every `callSerially` if there’s a “real” exception thrown within the `callSerially` it uses this “fake” one as the cause. That means you will be able to see the cause for a specific bug when this is enabled.

Notice that this API is potentially prohibitive in terms of performance. As such we recommend that people don’t turn this on by default. You can include this as a user configuration or use it in debug builds.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — April 17, 2019 at 3:10 pm ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-23990))

> Francesco Galgani says:
>
> In the Javadoc of `Display.getInstance().setEnableAsyncStackTraces(…);`, it’s written: «Currently this is only supported in the JavaSE/Simulator port». Is it true? In that case, it’s not useful for crash reports…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Shai Almog** — April 18, 2019 at 2:43 am ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-24057))

> Shai Almog says:
>
> It should work for desktop builds and Android as far as the code goes. Some platforms might fail because of the way stack traces are handled but I don’t see anything in the code that indicates this was actually enforced. It might not work everywhere e.g. on iOS.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Francesco Galgani** — April 23, 2019 at 1:58 pm ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-24108))

> Francesco Galgani says:
>
> There is something strange, maybe you can help me to better understand the logic of setEnableAsyncStackTraces. Without setEnableAsyncStackTraces, I have a NullPointerException without any indication of the line of code that thrown the exception (so the log is useless), while with setEnableAsyncStackTraces(true) I have a com.codename1.ui.Display$EdtException, but in the log there is no mention of any NullPointerException, so it’s unclear where is the exception and why is there an exception.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Francesco Galgani** — April 23, 2019 at 2:21 pm ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-24096))

> Francesco Galgani says:
>
> I identified the cause of this exception, it was a popup.remove() (note  
> that “popup” and its parent weren’t null), however the solution that I  
> found is to use removeComponent (from the parent of the parent) instead  
> of popup.remove(). It’s unclear to me the logic of this exception,  
> however my question is the same: why does a NullPointerException become a  
> Display$EdtException using setEnableAsyncStackTraces?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Shai Almog** — April 23, 2019 at 4:55 pm ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-24115))

> Shai Almog says:
>
> This sounds like a bug in Codename One that triggered a cascading error.
>
> What’s popup? What type of component is it?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Francesco Galgani** — April 23, 2019 at 5:57 pm ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-23921))

> Francesco Galgani says:
>
> This is the code: [https://gist.github.com/jsf…](<https://gist.github.com/jsfan3/9f9865c28b70b9e19c737a4ea65cace4>)  
> As you can see, I’m trying to adapt AutoCompleteTextField to my needs. Note at the bottom of the code the commented popup.remove(). I’m trying to close the popup list immediately when setEditable(false) is called. A safe and correct way to do it could fit with my use case, but maybe my code is not safe. Moreover, calling setEditable(true) after setEditable(false) causes that the popup list will not appear again when the user start typing in the field. I hope that few changes can fix this code. Do you have any suggestion? (I know that Stack Overflow is more suitable for this type of questions, however we started the discussion here. Thank you for your support)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Shai Almog** — April 24, 2019 at 4:04 am ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-24040))

> Shai Almog says:
>
> Maybe this is related to an ongoing animation which triggered this. That might explain the broken stack trace. It might be necessary to flush animations first for this method to work.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)


### **Francesco Galgani** — April 24, 2019 at 6:20 am ([permalink](https://www.codenameone.com/blog/better-error-logging.html#comment-24066))

> Francesco Galgani says:
>
> You’re right!!! Thank you! You gave me the right hint to solve half of this issue: the first half is what you supposed (that solved the issue in several cases), the second half was that an override of initComponent() and deinitalize() allowed me to solve the exception in other cases. This is my fixed code: [https://gist.github.com/jsf…](<https://gist.github.com/jsfan3/d8c9698afe63f8cf466679e6a1d79a34>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fbetter-error-logging.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
