---
title: 'TIP: Stop Editing'
slug: tip-stop-editing
url: /blog/tip-stop-editing/
original_url: https://www.codenameone.com/blog/tip-stop-editing.html
aliases:
- /blog/tip-stop-editing.html
date: '2018-02-05'
author: Shai Almog
---

![Header Image](/blog/tip-stop-editing/tip.jpg)

Device only bugs are the worse. You need to go through a device build and reproduce/rinse/repeat. Thankfully these bugs are rare but sometimes they just hit you smack in the face. One such problem occurred when I was debugging a transition on Android related to a login form. I would move between a Form where I had the keyboard open to one where it was closed. This created a nasty effect where the keyboard folded leaving a black space and the transition played out about that black space.

On the simulator this won’t happen, we can’t realistically simulate the virtual keyboard.

It won’t happen on iOS either. Only on Android.

The Android port resizes the display during input and that behavior triggers this end result where the display doesn’t have time to recover before the transition starts.

Initially I thought I can workaround this by invoking:
    
    
    textField.stopEditing();
    callSerially(() -> showOtherForm());

But that only helped on some cases, not all. Even the fact that I used `callSerially` didn’t help as this depends on a native event going through.

The solution is to use the new `stopEditing(Runnable)` API. On most OS’s the runnable will be invoked immediately but on Android it will wait for the screen resize before it invokes the code. So this will work as you would expect:
    
    
    textField.stopEditing(() -> showOtherForm());
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **3lix** — February 7, 2018 at 2:41 am ([permalink](https://www.codenameone.com/blog/tip-stop-editing.html#comment-21634))

> 3lix says:
>
> Seeking an advice: should on each form I have TextFields, check to see which TextField, if any, has focus calling the hasFocus function and then when navigating to the next form make sure we had stopped editing ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-stop-editing.html)


### **Shai Almog** — February 7, 2018 at 5:04 am ([permalink](https://www.codenameone.com/blog/tip-stop-editing.html#comment-21534))

> Shai Almog says:
>
> I would suggest avoiding that unless you actually see a problem during transition. Normally transitions work but in some special cases this is obvious. In this specific case I had a morph transition from a form with a keyboard to one without.  
> Normally if you are transitioning using other transitions or to a form where editing is still open this would work as you would have expected.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-stop-editing.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
