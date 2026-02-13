---
title: Native Interface
slug: native-interface
url: /blog/native-interface/
original_url: https://www.codenameone.com/blog/native-interface.html
aliases:
- /blog/native-interface.html
date: '2014-01-14'
author: Shai Almog
---

![Header Image](/blog/native-interface/native-interface-1.png)

  
  
  
  
![Picture](/blog/native-interface/native-interface-1.png)  
  
  
  

Native interfaces allow developers to invoke platform native methods/functions/libraries and even widgets directly from within Codename One without having to adapt your code to every platform. They are a very powerful tool when bridging between Codename One and OS specific features to access functionality that might not yet be exposed in the Codename One platform.  
  
  
  
Normally in Java we would use JNI to access “native” however, JNI is designed to access C and we need much more. Codename One allows developers to access Dalvik (Java) when running under Android and defines that as “native”, C# when running in Windows Phone and Objective-C when running under iOS. Because of that it is remarkably hard to map arbitrary objects, callbacks and functionality to a native call. So to simplify that work we placed many restrictions on the construction of a native interface, however you can still accomplish pretty spectacular things such as a  
[  
complete working socket implementation  
](https://github.com/shannah/CN1Sockets)  
and more.  

  
Check out the new  
[  
How Do I? video we just published covering native interfaces  
](/how-do-i---access-native-device-functionality-invoke-native-interfaces.html)  
.  
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Gert** — July 13, 2017 at 2:14 am ([permalink](https://www.codenameone.com/blog/native-interface.html#comment-23710))

> Gert says:
>
> Is EVERYTHING of native code possible in codenameone by native interface?  
> In other word, when we assume we built some project by native code, is it possible to build ABSOLUTELY SAME PRODUCT (design, effect and functions etc,,of course, besides execution file size and response speed..) in cd1?  
> e.g.  
> Like Camera overlaying (for face detection, like MSQRD app, …)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-interface.html)


### **Shai Almog** — July 13, 2017 at 4:26 am ([permalink](https://www.codenameone.com/blog/native-interface.html#comment-23669))

> Shai Almog says:
>
> These things can be done in Codename One but “everything” is a bit of a big word.  
> Furthermore, lets separate “can” from “should”. If a lot of your code is native and you need a big native interface abstraction this might become painful to the point where Codename One isn’t worth it. I can’t put my finger on an exact point since it varies and this depends on your needs.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-interface.html)


### **Gert** — July 13, 2017 at 4:39 am ([permalink](https://www.codenameone.com/blog/native-interface.html#comment-23582))

> Gert says:
>
> Thanks for your kind answer.  
> Well, do you have any idea how to build face recognition app in cd1?  
> There, the issue for me is just how to get image info in real-time. (during camera showing)  
> Best.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-interface.html)


### **Shai Almog** — July 14, 2017 at 6:34 am ([permalink](https://www.codenameone.com/blog/native-interface.html#comment-23461))

> Shai Almog says:
>
> Look at how it’s done in native and follow those instructions. I haven’t done this so I can’t really point you at anything.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnative-interface.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
