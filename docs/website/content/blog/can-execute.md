---
title: Can Execute
slug: can-execute
url: /blog/can-execute/
original_url: https://www.codenameone.com/blog/can-execute.html
aliases:
- /blog/can-execute.html
date: '2014-07-13'
author: Shai Almog
---

![Header Image](/blog/can-execute/can-execute-1.png)

  
  
  
  
![Picture](/blog/can-execute/can-execute-1.png)  
  
  
  

Runtime.exec is pretty familiar to Java developers (and the process builder in later versions), however for mobile applications we usually don’t have access to an executable. The solution is to invoke a URL which is bound to a particular application, this works rather well assuming the application is installed and you can activate quite a few things e.g. this is a partial list of  
[  
URL’s that work on iOS  
](http://wiki.akosma.com/IPhone_URL_Schemes)  
. (notice that a lot of these are redundant since we have builtin portable functionality to address those features).  
  
  
Normally on iOS you would do something like  
  
canOpenURL followed by openURL assuming the can message returned true. However, Android et al doesn’t have anything quite like that. To enable this at least for iOS if not elsewhere we added a Display.canExecute() method to go with the Display.execute() method. However, canExecute returns a Boolean instead of a boolean which allows us to support 3 result states:  
  
  
1\. Boolean.TRUE – the URL can be executed.  
  
  
  
  
2\. Boolean.FALSE – the URL can be executed.  
  
  
3\. null – we have no idea whether this will work on this platform.  
  
  
  
  
We’ve added an experimental BigInteger/BigDecimal implementation to the util package. This is a very basic implementation lifted from the Bouncy Castle work, but these are big omissions in our current code which should allow more business/scientific code to be ported.  
  
  
  
  
We also have an experimental (deprecated) port of the MiG layout in place. We ported it due to a long standing RFE but it doesn’t seem to be working as expected, its hard to tell the source of the problem and we aren’t sure if we want to go in that direction. If this is something that is interesting to you let us know and also check out if you can spot our mistakes in the port.  
  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — September 18, 2014 at 5:18 am ([permalink](https://www.codenameone.com/blog/can-execute.html#comment-21978))

> Anonymous says:
>
> Is Can execute also usable on Android? If so, is there somewhere a list of Android specific urls? 
>
> Wim
>



### **Anonymous** — September 18, 2014 at 3:27 pm ([permalink](https://www.codenameone.com/blog/can-execute.html#comment-22030))

> Anonymous says:
>
> No, Android doesn’t really have anything quite like that. It has something potentially more powerful: intents. Which you can invoke via the execute call and also receive a response from said intent. 
>
> Since everything in Android is an Intent there isn’t so much a list of them as much as just googling the intent for whatever you want.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
