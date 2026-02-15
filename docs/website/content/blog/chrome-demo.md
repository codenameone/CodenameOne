---
title: Chrome Demo
slug: chrome-demo
url: /blog/chrome-demo/
original_url: https://www.codenameone.com/blog/chrome-demo.html
aliases:
- /blog/chrome-demo.html
date: '2016-05-15'
author: Shai Almog
---

![Header Image](/blog/chrome-demo/chrome-demo.png)

This week we chose to modernize the very outdated [Chrome Demo](/chrome/). This demo  
is one of our early demos developed during the iOS 4.x era. We licensed it’s original design from  
[app design vault](http://www.appdesignvault.com/shop/chrome/) and created a Codename One version  
of that original template. While the guys in app design vault modernized most of their templates to iOS 7  
flat design they didn’t do this for the Chrome demo.

**Check out a live preview of the demo on the right here thanks to our JavaScript port!**

We were a bit conflicted on whether this demo should be kept or discarded but we eventually decided to keep  
it due to two major reasons:

  * The calculator portion of the demo is pretty cool

  * It customizes the calendar widget and makes it look pretty decent.

### GUI Builder

The Chrome demo was built using the GUI builder which we are now de-emphasizing in favor of the upcoming  
new GUI builder. We converted the demo to use the new GUI builder code using the  
[migration wizard](/blog/terse-syntax-migration-wizard-more.html) and this was a relatively smooth process  
although we needed to do some work to cleanup the old state machine and event handling. Since the demo  
didn’t have much navigation logic and is relatively small the process was pretty easy.

### Design

We removed a lot of the brushed metal effects in the design to make the app feel more modern, we updated the  
fonts and replaced some of the icons with icon fonts. Specifically we used the builtin material design icons.

This improved the design considerably but this is probably not our most refined demo.

### The Source

Check out the full source code for the demo in the  
[github repository for the Chrome demo](https://github.com/codenameone/Chrome).

This demo will be integrated into the upcoming new project wizards in the various IDEs.

### Up Next

So far so good for our demo walkthru but these are still relatively simple demos, we hope to start tackling the  
more challenging and interesting demos that might be harder to adapt to newer conventions.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Ross Taylor** — May 17, 2016 at 4:35 pm ([permalink](https://www.codenameone.com/blog/chrome-demo.html#comment-22782))

> Ross Taylor says:
>
> Nice demo! However I wonder why it takes a while to load the app (around 15 – 30 seconds) is it line speed (I have 10Mbps), browser (Firefox v46) or your Javascript Port itself? What happens if the app is large around 23MB? Will it affect the load time in the browser?
>



### **Shai Almog** — May 18, 2016 at 3:54 am ([permalink](https://www.codenameone.com/blog/chrome-demo.html#comment-22702))

> Shai Almog says:
>
> The demo runs locally so once it’s downloaded everything is here. There are several large files it needs to download in advance e.g. the theme is 800kb but the biggest problem is [http://codenameone.com/demo…](<http://codenameone.com/demos/Chrome/teavm/classes.js>) which is 1.7mb.
>
> I think our current server doesn’t gzip the JavaScript file so that might be a problem.
>
> 1.7MB is pretty small for what is effectively a full application with the JVM included but it does have a startup time overhead. This isn’t huge when compared to the many existing sites on the internet today in terms of data volume, but unlike those sites we need the whole thing to download before we can startup the VM.
>



### **Shai Almog** — May 18, 2016 at 5:09 am ([permalink](https://www.codenameone.com/blog/chrome-demo.html#comment-22669))

> Shai Almog says:
>
> Actually looking at this again it seems our CDN does gzip the file so it’s really 300kb or so which is pretty impressive… Looking at the firefox logs I think this might be the time taking to load the javascript and the resource files which are pretty large for this application (around 1.2mb). I think a lot of the resources can be optimized to reduce startup time further.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
