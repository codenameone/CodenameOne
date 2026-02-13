---
title: 'TIP: When Shouldn''t I Use Codename One'
slug: tip-when-shouldnt-i-use-codename-one
url: /blog/tip-when-shouldnt-i-use-codename-one/
original_url: https://www.codenameone.com/blog/tip-when-shouldnt-i-use-codename-one.html
aliases:
- /blog/tip-when-shouldnt-i-use-codename-one.html
date: '2017-03-19'
author: Shai Almog
---

![Header Image](/blog/tip-when-shouldnt-i-use-codename-one/tip.jpg)

Bootcamp registration closed well and we are currently in the pre-course (more on that in another post) and already the Facebook group of the bootcamp is seeing decent activity. Being as busy as I am I thought I’d lift a question that was asked there to headline this post: “When shouldn’t I use Codename One?”.

That’s a great question. If Codename One was perfect for every use case it would probably suck. One of the biggest reasons for the complexity of the Android API is that it tries to answer every use case (device vendors, utility developers, game developers, app developers etc.).

I’ll ignore the obvious use cases for people who don’t like Java. Codename One is designed for Java developers so this is the baseline I’m addressing.

Codename One is optimized for specific use cases: Apps (mostly standard types) & portability.

If the app you are building doesn’t fit in either one of these molds Codename One doesn’t make sense.

E.g. if you are building a game then Codename One isn’t optimized for that.  
You can build simple games and they will work fine but once you try to do things like 3d graphics etc. this will become a problem. In the case of gaming I would personally go with a framework more designed for gaming rather than native.

Then there are device specific tools/utilities e.g. if I wanted to build an Android task manager, this makes no sense in other OS’s. With such a tool bulk of my code would be native Android code anyway so the benefit of using Codename One becomes a hindrance.  
Codename One provides access to the native layer so you can patch small missing pieces but if the entire functionality of your app needs to be in native and can’t be encapsulated the pain of going back and forth to the native interface to communicate back to Codename One might not be worth it.

In the past this also applied for apps like mapping apps, e.g. up until recently if you wanted to build an app like Uber on top of Codename One this would have been a huge pain as every bit of “on-map” functionality would have required adding support natively to the google maps native code. We recently introduced the ability to place components on top of heavyweight widgets (e.g. maps) so the functionality of an app like Uber can be accomplished mostly within portable Codename One logic.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Maxim Geraskyn** — April 10, 2017 at 5:47 pm ([permalink](https://www.codenameone.com/blog/tip-when-shouldnt-i-use-codename-one.html#comment-23213))

> Maxim Geraskyn says:
>
> Hi Shai!
>
> There is a real magic in your tool. I’m leading the development of unTill software ([www.untill.com](<http://www.untill.com>)), currently it is rather a big windows desktop app to automate restaurants and hotels, recently we faced a challenge to migrate MOST of our logic to mobile platforms, iOS and Android, so portability is a cornerstone.
>
> At this stage my goal was to get right feeling of the development tools. I played a bit with the latest Android SDK, watched an hour or two of training videos and still was unable to develop an Android app. Then I found a reference to the Codenameone and in a half an hour got a feeling that I understand the core idea of it and how to create a first app, very simple and clean ideas.
>
> But, the question is – would you recommend to use it as a base for a rather big development, eventually it will contain around 200K-500K lines of code?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-when-shouldnt-i-use-codename-one.html)


### **Shai Almog** — April 11, 2017 at 4:32 am ([permalink](https://www.codenameone.com/blog/tip-when-shouldnt-i-use-codename-one.html#comment-23345))

> Shai Almog says:
>
> Thanks!
>
> I’ve seen projects of this size work with Codename One but I would cull them. Mobile interfaces (even tablets) should be smaller as deep functionality within a mobile app is often hard on the end user. Apps need to be simpler, smaller and answer narrower use cases than the full blown desktop experience.
>
> This is also good in terms of size/performance/build speed for the resulting app. So one of the more important things to do when moving to mobile (regardless of technology used) is to disconnect from the desktop/web version of your application and think smaller.
>
> A good example is in the division of use cases. E.g. a corporate application that tracks the sales process might be used by managers and line workers. The former use the application very differently from the latter who only need the reports. These should really be two separate apps with common code.
>
> As a side note, a lot of desktop related code can be optimized away in the move to mobile e.g. storage can be streamlined as a lot of features (e.g. export) don’t make as much sense in mobile. Codename One also includes a lot of abstractions to help you cull away code and making it more generic.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftip-when-shouldnt-i-use-codename-one.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
