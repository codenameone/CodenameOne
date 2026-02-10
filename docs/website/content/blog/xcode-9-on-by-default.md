---
title: Xcode 9.2 on by Default this Friday
slug: xcode-9-on-by-default
url: /blog/xcode-9-on-by-default/
original_url: https://www.codenameone.com/blog/xcode-9-on-by-default.html
aliases:
- /blog/xcode-9-on-by-default.html
date: '2018-02-19'
author: Shai Almog
---

![Header Image](/blog/xcode-9-on-by-default/xcode-migration.jpg)

A few weeks ago I announced the [xcode 9.2 mode](/blog/xcode-9-mode.html) and was rather happy that we can take our time with the migration. Unfortunately, that wasn’t meant to be. Apple will require all new submissions to use xcode 9 within the next few months so it makes no sense to keep 7.3 as the default. This weekend we will flip the switch and builds will default to 9.2.

It’s earlier than we wanted to but it’s crucial that Codename One 4.0 will work well with xcode 9.2 otherwise developers won’t be able to use versioned build halfway through the 4.0 cycle.

As a reminder if you want to still use xcode 7.3 because you are concerned that this change broke something in your code just add the build hint:

`ios.xcode_version=7.3`

To test xcode 9.2 before the update this weekend just use the build hint:

`ios.xcode_version=9.2`

For the most part the change should be seamless at this time. I would suggest that you read the [original article](/blog/xcode-9-mode.html) where I discuss the refined permissions that are now a part of xcode 9. If you run into issues please check that they happen in 9 and not in 7 and [file issues](http://github.com/codenameone/CodenameOne/issues/new) immediately!

### Multitasking Flag

To support multi-tasking in iPads where we can have a side by side view of the UI we need to use a xib launch file. Unfortunately we just couldn’t nail this correctly for all the use cases so we had to keep the existing screenshot code in place for now.

__ |  Regular iOS multitasking will still work as usual this refers only to the iPad split screen support   
---|---  
  
If you are willing to compromise on some edge case odd behavior for the launch image in exchange for side by side view you can turn this on explicitly by using the `ios.multitasking=true` build hint. Hopefully we’ll be able to refine the xib behavior and turn this on by default for 5.0.

### Plugin Update & Release Delay

With the update of the libs this weekend we’ll add a strong nag to people who haven’t updated their plugins. We need to deploy the new [update framework](/blog/new-update-framework.html) as it allows us to solve some problems in very elegant ways.

If you haven’t updated your IDE plugin please do so soon.

We are considering delaying the release that is planned for March 6th by a week or even two. This would give us more time to test these two big changes: update framework & the 9.2 migration.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Stefan Eder** — February 20, 2018 at 2:04 pm ([permalink](https://www.codenameone.com/blog/xcode-9-on-by-default.html#comment-23528))

> Stefan Eder says:
>
> Does that mean one could use Swift code, too?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-9-on-by-default.html)


### **Shai Almog** — February 21, 2018 at 5:25 am ([permalink](https://www.codenameone.com/blog/xcode-9-on-by-default.html#comment-23923))

> Shai Almog says:
>
> Not directly.
>
> The problem is ARC, Swift requires it but it collides with our GC.
>
> You can use swift in a static library and invoke it from a native interface wrapper.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-9-on-by-default.html)


### **Stefan Eder** — February 21, 2018 at 6:20 am ([permalink](https://www.codenameone.com/blog/xcode-9-on-by-default.html#comment-23731))

> Stefan Eder says:
>
> It would be nice to have an example on how to do this
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-9-on-by-default.html)


### **Shai Almog** — February 22, 2018 at 8:41 am ([permalink](https://www.codenameone.com/blog/xcode-9-on-by-default.html#comment-23853))

> Shai Almog says:
>
> Doing this is not intuitive and most of the sample would be in xcode so would step too deep out of the comfort zone. It won’t produce any benefit either in simplicity, maintainability or performance. If you have a large library of swift code you can use it this way but if you have a large library of swift code you probably know how to do it to begin with an don’t need our help…
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-9-on-by-default.html)


### **Stefan Eder** — February 23, 2018 at 6:49 am ([permalink](https://www.codenameone.com/blog/xcode-9-on-by-default.html#comment-23836))

> Stefan Eder says:
>
> I know your attitude but that does not help me further. I tried to use Codename One several years and paid for it – and eventually abandoned.  
> In an ideal situation I’d love to support Codename One by providing solutions for everyone but the world is not ideal and my situation is not either.  
> I have about four hours a week for Codename One and yet I can not accept that some things just do not work. Of course it would help if, in exceptional cases and only for me, I could make additions and corrections. Preventing that from happening prevents me from using Codename One – that’s how easy it is.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-9-on-by-default.html)


### **Shai Almog** — February 24, 2018 at 4:59 am ([permalink](https://www.codenameone.com/blog/xcode-9-on-by-default.html#comment-23690))

> Shai Almog says:
>
> As I said before, I’m sorry about that. But there are some things we won’t do even for paying customers. Every cross platform tool has its limitations. You want our tool to be both easy and use the build servers. That’s not possible. It’s a Pandoras box that we won’t open.
>
> That has nothing to do with Swift support which is a technical issue related to ARC.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fxcode-9-on-by-default.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
