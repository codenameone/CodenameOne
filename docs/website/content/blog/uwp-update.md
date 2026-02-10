---
title: UWP Update
slug: uwp-update
url: /blog/uwp-update/
original_url: https://www.codenameone.com/blog/uwp-update.html
aliases:
- /blog/uwp-update.html
date: '2016-10-03'
author: Shai Almog
---

![Header Image](/blog/uwp-update/uwp-on-codenameone.jpg)

We’ve mentioned the progress we made with the Universal Windows Platform support in the past. This support  
is moving at a very fast pace…​  
By now we support `SQLite` as well as `Media`, `Capture` and a slew of other features that weren’t available for  
the initial beta. At this rate we think that 3.6 can actually include a production version of the UWP port!

Other than that we also added support for things such as native OS sharing on UWP as well as low level graphics  
transforms. We also fixed many bugs in the port with text positioning, missing status bar, crashes etc.

We’re confident that we will see many new Codename One apps poping up in the Microsoft appstore  
in the coming months.

One of the problems we discovered is that the Nokia Lumia simulator that we sometimes used to debug windows  
apps uses the wrong theme. Instead of the black Windows Metro style theme it incorrectly used the Android  
holo theme. If you have that simulator skin installed we suggest updating it via the Skins → More menu option.

This problem caused us to incorrectly tune the colors in the native theme for the kitchen sink. After fixing this we  
were able to adapt the code. It’s important to notice that the default colors on Windows are the exact opposite  
of the Android/iOS black over white so if you rely on colors you need to be explicit.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
