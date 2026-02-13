---
title: Understanding Peer (native) Components & Why Codename One is so portable
slug: understanding-peer-native-components-why-codename-one-is-so-portable
url: /blog/understanding-peer-native-components-why-codename-one-is-so-portable/
original_url: https://www.codenameone.com/blog/understanding-peer-native-components-why-codename-one-is-so-portable.html
aliases:
- /blog/understanding-peer-native-components-why-codename-one-is-so-portable.html
date: '2014-05-02'
author: Shai Almog
---

![Header Image](/blog/understanding-peer-native-components-why-codename-one-is-so-portable/understanding-peer-native-components-why-codename-one-is-so-portable-1.png)

  
  
  
  
![Picture](/blog/understanding-peer-native-components-why-codename-one-is-so-portable/understanding-peer-native-components-why-codename-one-is-so-portable-1.png)  
  
  
  

I’ve had some talks with rather savvy Codename One developers recently which made me realize that quite a few developers don’t truly grasp the reason why we have a separation between peer (native) components and Codename One components. This is a crucial thing you need to understand especially if you plan on working with native widgets e.g. Web Browser,  
[  
native maps  
](http://www.codenameone.com/3/post/2014/03/mapping-natively.html)  
, text input, media and  
[  
native interfaces  
](http://www.codenameone.com/how-do-i---access-native-device-functionality-invoke-native-interfaces.html)  
(which can return a peer). 

Codename One draws all of its widgets on its own, this is a concept which we modeled in part after Swing. This allows you to do various things in Codename One that you just can’t do in any native platform:  
  
1\. GUI builder/simulator that’s pretty close to how the app will look on the device – notice that this also enables the build cloud, otherwise you would be drowned by device specific bugs making the build cloud redundant.

2\. Ability to override everything – paint, pointer, key events are all overridable and replaceable. You can also paint over everything e.g. glasspane and layered pane.

3\. Consistency – you can get identical functionality on all platforms for the most part. 

This all contributes to our ease of working with Codename One and maintaining Codename One. More than 95% of Codename One’s code is in Java hence its really portable and pretty easy to maintain!

**  
So why add native widgets at all?  
**  
  
We need the native device to do input, html rendering etc. these are just too big and too complex tasks for us to do from scratch.

So whats the problems with native widgets?  
  
Codename One does pretty much everything on the EDT (Event Dispatch Thread), this provides a lot of cool features e.g. modal dialogs, invokeAndBlock etc. however native widgets have to be drawn on their own thread… So the process for drawing a native widgets has to occur in the naive rendering thread. This means that drawing looks something like this:  
  
1\. Loop over all Codename One components and paint them.  
  
2\. Loop over all native peer components and paint them.

This effectively means that all peer components will always be on top of the Codename One components. 

**  
So how do we show dialogs on top of Peer Components?  
**  
  
This is tricky; we grab a screenshot of the peer, hide it and then we can just show the screenshot. Since the screenshot is static it can be rendered via the standard UI. Naturally we can’t do that always since grabbing a screenshot is an expensive process on all platforms and must be performed on the native device thread.

**  
Why can’t we combine peer component scrolling and Codename One scrolling?  
**  
  
Since the form title/footer etc. are drawn by Codename One the peer component might paint itself on top of them. Clipping a peer component is often pretty difficult.  
  
Furthermore, if the user drags his finger within the peer component he might trigger the native scroll within the might collide with our scrolling?  
  
The old text edit defaulted to stopping the editing during scrolling for exactly that reason, however the new iOS always on VKB mode doesn’t do that. It just hides the peer component the moment scrolling is detected to prevent these issues from happening.

There are also some additional problems that might be counter intuitive. E.g. iOS has  
[  
7 screenshot images  
](http://www.codenameone.com/3/post/2014/03/the-7-screenshots-of-ios.html)  
representing the first form. If your first page is an HTML or a native map (or other peer widget) the screenshot process on our build server will show fallback code instead of the real thing thus providing sub-par behavior. Some developers asked us about the HTML aspect, but its hard for us to grab a desktop screenshot of the web component (JavaFX thread pain) and it would also look completely different from the web component running on the device.

I hope this clarifies the problem with native peers and why we try to add as few of them as reasonably possible.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
