---
title: Stacks & Flipping Switches
slug: stacks-flipping-switches
url: /blog/stacks-flipping-switches/
original_url: https://www.codenameone.com/blog/stacks-flipping-switches.html
aliases:
- /blog/stacks-flipping-switches.html
date: '2014-08-24'
author: Shai Almog
---

![Header Image](/blog/stacks-flipping-switches/stacks-flipping-switches-1.png)

  
  
  
[  
![Picture](/blog/stacks-flipping-switches/stacks-flipping-switches-1.png)  
](/img/blog/old_posts/stacks-flipping-switches-large-2.png)  
  
  

After a while with several new features being off by default we are officially flipping the switch on newer functionality in the build arguments. Notice that you can always disable this new functionality by explicitly defining the appropriate build argument, however you should probably let us know what isn’t working for you.  
  
  
  
  
We are turning on the new Android/iOS graphics pipelines by default which should mean the new shape/transform API’s should now “just work”. To disable the Android pipeline use android.asyncPaint=false and to disable the iOS pipeline use ios.newPipeline=false  
  
  
  
  
We are also switching on a virtual keyboard change for iOS which provides a more “native’ feel for the iOS keyboard, it still has some unique edge cases but we feel it provides a better default experience overall. This virtual keyboard mode keeps the keyboard open once you start editing even if you scroll the form, its generally more like the native editing functionality in iOS. You can disable it using ios.keyboardOpen=false.  
  
  
  
  
You’ll notice that the new VM still isn’t the default, we feel it hasn’t reached the maturity level of the current VM and with JavaOne looming we don’t want to disrupt the code stability too much.  
  
  
  
  
We did find a new and exciting way to generate stack traces on iOS. If you use our standard logging mechanism using Log.e() you can now get full stack traces on iOS devices (not including line numbers) even for the older VM. The traces would look a bit “weird” but they are readable and can be used to locate the source of a problem.  
  
  
  
  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — August 28, 2014 at 8:46 am ([permalink](https://www.codenameone.com/blog/stacks-flipping-switches.html#comment-22187))

> Anonymous says:
>
> Why is it that sometimes when you open a webview on android and it has a form, the virtual keyboard does not open on focus of an input element even when there is no other focus component on the form. Some times it works, other times it doesn’t.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fstacks-flipping-switches.html)


### **Anonymous** — August 28, 2014 at 4:13 pm ([permalink](https://www.codenameone.com/blog/stacks-flipping-switches.html#comment-22223))

> Anonymous says:
>
> That’s unrelated to the post. You should use the discussion forum [http://www.codenameone.com/…](<http://www.codenameone.com/discussion-forum.html>) or stack overflow for such questions which will also allow the community members to notice them. 
>
> This is an Android bug that we worked around once but it keeps resurfacing for some cases. We aren’t aware of a workaround for that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fstacks-flipping-switches.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
