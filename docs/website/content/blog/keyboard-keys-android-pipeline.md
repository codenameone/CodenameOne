---
title: Keyboard Keys & Android Pipeline
slug: keyboard-keys-android-pipeline
url: /blog/keyboard-keys-android-pipeline/
original_url: https://www.codenameone.com/blog/keyboard-keys-android-pipeline.html
aliases:
- /blog/keyboard-keys-android-pipeline.html
date: '2014-06-17'
author: Shai Almog
---

![Header Image](/blog/keyboard-keys-android-pipeline/keyboard-keys-android-pipeline-1.png)

  
  
  
  
![Picture](/blog/keyboard-keys-android-pipeline/keyboard-keys-android-pipeline-1.png)  
  
  
  

Some features we have in Codename One are a bit hidden behind the surface, we add them as a patch for a developer and don’t have the proper place to document them so they get buried. Case in point is the ability in Android to use the search magnifier icon instead of the done button. To get that functionality you can just set a client property on the relevant text field as such:  
  
Display.getInstance().putClientProperty(“searchField”, Boolean.TRUE); 

This also allows other options such as sendButton & goButton. 

On an unrelated note we are now making some progress on the new  
[  
Android graphics pipeline  
](http://www.codenameone.com/3/post/2014/03/new-android-pipeline-fixes.html)  
implementation which should hopefully allow us to scale our graphics further. We also made quite a few improvements to the new Codename One VM on iOS squashing quite a few bugs. However, more issues and performance penalties remain so we are still actively improving this. If you have test cases and issues specific to the VM please let us know so we can address them and move this new VM to a production quality product.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — October 30, 2014 at 3:16 pm ([permalink](https://www.codenameone.com/blog/keyboard-keys-android-pipeline.html#comment-22145))

> Anonymous says:
>
> How do i listen to the done or search event when the user presses either the search button or send. I want to capture the event. Also, instead of the search icon, can I set the text to something like “create”?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fkeyboard-keys-android-pipeline.html)


### **Anonymous** — October 30, 2014 at 9:53 pm ([permalink](https://www.codenameone.com/blog/keyboard-keys-android-pipeline.html#comment-22006))

> Anonymous says:
>
> goButton instead of searchButton might produce something that’s closer to what you want. 
>
> You can just use action listener to get the event of text input completion.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fkeyboard-keys-android-pipeline.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
