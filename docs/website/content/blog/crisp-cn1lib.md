---
title: Crisp cn1lib
slug: crisp-cn1lib
url: /blog/crisp-cn1lib/
original_url: https://www.codenameone.com/blog/crisp-cn1lib.html
aliases:
- /blog/crisp-cn1lib.html
date: '2018-10-22'
author: Shai Almog
---

![Header Image](/blog/crisp-cn1lib/new-features-1.jpg)

Crisp powers the chat button in the bottom right portion of our site. It also handles emails and a host of other great features. One feature we didn’t take advantage of is the mobile app support. To solve that we just issued a new [Crisp cn1lib](https://github.com/codenameone/CrispCodenameOneSDK) which we integrated into the new versions of our [Android](https://www.codenameone.com/blog/build-app-beta.html) and [iOS](https://www.codenameone.com/blog/build-app-on-ios.html) apps.

You can install it yourself using the extension manager and use it with the instructions [here](https://github.com/codenameone/CrispCodenameOneSDK).

There is some implementation detail related to the library which I think would be interesting to developers building similar solutions.

### Why HTML/JS and Not Native?

When I started the work of porting the library I looked at the Crisp native SDKs for iOS/Android. I even got some code working but as I looked through the actual SDK source code it became apparent that Crisps native SDKs for iOS and Android don’t leverage native functionality. This is perfectly OK as HTML/JS can deliver a fine experience for this type of app.

However, its silly to wrap the native libraries when I can use HTML directly and get greater portability. As a result of that I threw away the code I wrote for the original integration and used the HTML approach. This highlights the importants of reviewing the implementation before we start implementing a cn1lib.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Boniface N. Githinji** — October 26, 2018 at 3:37 pm ([permalink](https://www.codenameone.com/blog/crisp-cn1lib.html#comment-24039))

> Boniface N. Githinji says:
>
> Great job Shai. I’d love to re-style the FAB button – change the bg color. Any pointers on how I could do this? I tried ‘Crisp.getInstance().chatFab().getAllStyles().setBgColor(0x24d07a);’ but this didn’t work. FAB still had the red color.
>
> Thank you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcrisp-cn1lib.html)


### **Boniface N. Githinji** — October 26, 2018 at 3:44 pm ([permalink](https://www.codenameone.com/blog/crisp-cn1lib.html#comment-24064))

> Boniface N. Githinji says:
>
> Got it to work by overriding the ‘FloatingActionButton’ UUID. Changed BgColor to 24d07a.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcrisp-cn1lib.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
