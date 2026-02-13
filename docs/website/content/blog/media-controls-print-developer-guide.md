---
title: Media Controls and Print Developer Guide
slug: media-controls-print-developer-guide
url: /blog/media-controls-print-developer-guide/
original_url: https://www.codenameone.com/blog/media-controls-print-developer-guide.html
aliases:
- /blog/media-controls-print-developer-guide.html
date: '2017-10-10'
author: Shai Almog
---

![Header Image](/blog/media-controls-print-developer-guide/new-features-2.jpg)

I did a lot of work on the developer guide PDF making it more suitable to print, as part of this work I submitted the guide to Amazons KDP which means you can now order a physical book of the developer guide. I reduced the page count significantly for lower cost and image size requirements. As a result the book is much smaller but contains the exact same information in a denser package.

You can [order the book on Amazon](https://www.amazon.com/dp/1549910035), make sure you select the print edition.

### Media Controls & Stability

Steve did a lot of work on media over the past week and it should be more stable/consistent across platforms. One of the big changes is support for native video controls which up until now was platform specific. Starting with the next update you would be able to hint to the player whether you want native playback controls or not by using this code to show the native controls:
    
    
    someMedia.setVariable(Media.VARIABLE_NATIVE_CONTRLOLS_EMBEDDED, true);

You can use `false` to hide them.

### On Top Sidemenu Update & Regressions

We’ve put a lot of effort into refining the on-top sidemenu which is already showing a lot of promise. Unfortunately one of the changes we made caused a regression to the general functionality of Codename One. We’ve posted a fix and might push an earlier update to the libs before Friday to workaround this issue.

The on-top side menu is still off by default and will remain that way with the coming update due to these regressions. We still think it’s important to turn this on by default with enough time for the 3.8 release so this feature becomes stable.

So far the on-top side menu is a huge improvement especially in UI’s that contain native peers such as Maps where this feature is invaluable. The regular side menu causes a noticeable flicker which doesn’t happen with the on-top variety.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — October 18, 2017 at 10:28 am ([permalink](https://www.codenameone.com/blog/media-controls-print-developer-guide.html#comment-23815))

> Francesco Galgani says:
>
> At the moment, the developer guide pdf has 576 pages. Is this the reduced version?  
> [https://www.codenameone.com…](<https://www.codenameone.com/files/developer-guide.pdf>)  
> I prefer to print by myself the book, because the shipping cost from USA to my country is very expensive (a lot more than the book itself).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmedia-controls-print-developer-guide.html)


### **Shai Almog** — October 19, 2017 at 5:56 am ([permalink](https://www.codenameone.com/blog/media-controls-print-developer-guide.html#comment-23605))

> Shai Almog says:
>
> Yes it’s the reduced size. The original was just under 1000 pages…  
> Personally I prefer the feel of a professionally printed/bound book over A4’s. Either way we don’t make a profit over the book (it’s just printing costs) so feel free to print it yourself.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fmedia-controls-print-developer-guide.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
