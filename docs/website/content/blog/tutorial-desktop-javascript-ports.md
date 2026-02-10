---
title: Tutorial – Desktop and JavaScript Ports
slug: tutorial-desktop-javascript-ports
url: /blog/tutorial-desktop-javascript-ports/
original_url: https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html
aliases:
- /blog/tutorial-desktop-javascript-ports.html
date: '2017-10-04'
author: Shai Almog
---

![Header Image](/blog/tutorial-desktop-javascript-ports/learn-codenameone-2.jpg)

Codename One has ports for Mac, Windows & even for the browser. These are often confused and the introduction of the UWP port which also includes some overlap only made matters worse. Each one of these ports covers a segment that the other ports don’t.

In this tutorial I try to separate the various ports & explain when each one of them should be used. I also dispel common misconceptions about these ports.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Francesco Galgani** — November 5, 2017 at 10:50 am ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-23704))

> Francesco Galgani says:
>
> Where are some working examples of the javascript port of some apps?
>
> Please also note the the link “Developer guide section covering the JavaScript port” in the page “How Do I – Use the Desktop and JavaScript Ports” is broken (the word “manual” in the URL is duplicated).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)


### **Shai Almog** — November 6, 2017 at 2:31 pm ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-23712))

> Shai Almog says:
>
> Thanks! We’ll fix the links.  
> You can look at the demos in the demo section. Each one of them has a JavaScript link among the links in the bottom.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)


### **Francesco Galgani** — November 7, 2017 at 8:03 pm ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-23851))

> Francesco Galgani says:
>
> I’m thinking to upgrade my account to Enterprise because I really interested in the javascript port. My idea is to develop from scratch an app for smartphone and tablet and a web site using the same code: is it possible? Your demos have some problems…
>
> In my opinion, the user experience needs to be improved, because the user expects to have an hand cursor on clickable elements, but this doesn’t happen. Is it possible?
>
> Please note that the PropertyCross demo doesn’t work properly: open the js port and on search write “London”, and then click on Go. You will have a “Searching…” form with an infinitive loop, but nothing more, no results (tested on Chrome and Firefox on Linux and Windows). More over, the Poker demo js port doesn’t work at all: try to open it, you will get a white page (tested on Chrome and Firefox on Linux).
>
> I didn’t tested all the demos, maybe they need a check. At the moment my first impression is not the best, but I hope that all the problems are resolvable.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)


### **Shai Almog** — November 8, 2017 at 5:12 am ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-23859))

> Shai Almog says:
>
> We support cursor behavior in the current version of Codename One (I think it was added for 3.7) but we don’t implicitly change the mouse cursor. This should work on JavaScript and in the simulator see [https://www.codenameone.com…](<https://www.codenameone.com/blog/splitpane-cursors-push-registration.html>)
>
> I just tried the pocker demo and it worked for me, it took a few seconds to load so maybe some hiccup happened during download on your end?
>
> Property cross seems to freeze when accessing the network, we might have an issue in our webserver as we need to proxy server calls to workaround the “same origin” restriction. We’ll have to look into that.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)


### **Francesco Galgani** — November 12, 2017 at 12:04 am ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-23678))

> Francesco Galgani says:
>
> Thank you for the Mouse Cursor tip.
>
> I cannot open the Poker demo on my computer, I tried different browsers.  
> This is the console output of the Poker demo (js port) on my updated version of Google Chrome:
>
> fontmetrics.js:69 [Deprecation] ‘window.webkitStorageInfo’ is deprecated. Please use ‘navigator.webkitTemporaryStorage’ or ‘navigator.webkitPersistentStorage’ instead.  
> (anonymous) @ fontmetrics.js:69  
> (index):1 Uncaught (in promise) DOMException: The requested version (1) is less than the existing version (2).  
> [TString.java](<http://TString.java>):147 Uncaught (in promise) TypeError: Cannot read property ‘J9’ of null  
> at C ([TString.java](<http://TString.java>):147)  
> at $rt_ustr (classes.js:4)  
> at ThB (classes.js:4666)  
> at PJ ([TLogger.java](<http://TLogger.java>):60)  
> at JEB ([TLogger.java](<http://TLogger.java>):130)  
> at Y0 ([HTML5Implementation.java](<http://HTML5Implementation.java>):2390)  
> at Gy ([Storage.java](<http://Storage.java>):171)  
> at BeB ([Preferences.java](<http://Preferences.java>):43)  
> at YRC ([Preferences.java](<http://Preferences.java>):233)  
> at Nj ([CodenameOneImplementation.java](<http://CodenameOneImplementation.java>):211)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)


### **Shai Almog** — November 12, 2017 at 5:17 am ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-23566))

> Shai Almog says:
>
> Thanks for the headsup. I think there might have been a change in chrome that we already adapted the code to but didn’t refresh this demo (I use firefox and it works on it). I just sent a new build and it works, we’ll update the demo a bit later but you can check out a live preview here: [https://codename-one.appspo…]([https://codename-one.appspot.com/getData?m=result&i=6281816835883008&b=67602445-25d7-4bd6-a9c1-5916de35c7f1&n=preview5527779814375524259.html](https://codename-one.appspot.com/getData?m=result&i=6281816835883008&b=67602445-25d7-4bd6-a9c1-5916de35c7f1&n=preview5527779814375524259.html))
>
> Notice that like all Codename One build links this will expire in a couple of days.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)


### **Francesco Galgani** — November 12, 2017 at 12:53 pm ([permalink](https://www.codenameone.com/blog/tutorial-desktop-javascript-ports.html#comment-21531))

> Francesco Galgani says:
>
> Very good news: the live preview you linked works correctly 🙂
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ftutorial-desktop-javascript-ports.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
