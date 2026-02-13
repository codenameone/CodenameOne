---
title: Web App Build Target
slug: web-app-build-target
url: /blog/web-app-build-target/
original_url: https://www.codenameone.com/blog/web-app-build-target.html
aliases:
- /blog/web-app-build-target.html
date: '2015-01-25'
author: Shai Almog
---

![Header Image](/blog/web-app-build-target/web-app-build-target-1.png)

  
  
  
  
![Picture](/blog/web-app-build-target/web-app-build-target-1.png)  
  
  
  

  
  
In the past we made several attempts at compiling Codename One applications to webapps, these were only partially successful. On the surface this seems relatively simple: just use something like GWT and the Canvas API to generate a web app. However, Codename One requires threads (for the EDT) and that’s just not something you can really hide. GWT is also problematic since it requires the source code rather than the bytecode of the application… 

A couple of years ago I attended a session from Tony Epple and Jaroslav Tulach at JavaOne where they discussed the work they were doing on  
[  
bck2brwsr  
](http://wiki.apidesign.org/wiki/Bck2Brwsr)  
. This looked very interesting and I instantly broached the subject of threads with Jaroslav. Fast forward to today and Jaroslav is interested in implementing threads on bck2brwsr and looking for sponsorship to do that… 

That’s where we can step in… We would like to sponsor Jaroslav in his efforts which will allow us to do something grand!

We will allow you to just right click a project and select “Build a web app”. You will get a self contained app that doesn’t need a server side and would be implemented entirely in JavaScript… You would still be able to use threads and most of the standard things Codename One has to offer, some limitations might apply but you should be able to create a completely portable app!  
  
One of the nice things is that due to the architecture of Codename One we belive it would be faster than typical HTML5 framworks since it won’t be suseptible to the reflow problems that typical HTML5 code is suseptible to.

To pay for this work we will make this feature enterprise only and  
**  
would only implemement it if we get 2 annual enterprise subscribers!  
**

This will effectively pay for most of the initial effort required to build this feature, if you are interested in this feature make sure to  
[  
signup for an enterprise account right now  
](/pricing.html)  
! 

Notice that since this feature would require an enterprise account anyway sponsoring this work will cost you nothing.  
  

  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 26, 2015 at 6:51 pm ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22108))

> Anonymous says:
>
> This sounds interesting. I looked at bck2brwsr a while ago, but it wasn’t really ready for prime time. I’ve done a lot of work with GWT, and really like it, except for the reliance (like most all Javascript frameworks) on the DOM. GWT has some nice features though, like code splitting. 
>
> I’m wondering what the use case is you see for this ? As CN1 is touch device oriented, would you see the ability to have a keyboard/mouse app ?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 26, 2015 at 10:11 pm ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22260))

> Anonymous says:
>
> Take a look on Dragome ([http://www.dragome.com/)](<http://www.dragome.com/>)). It’s also a production ready bytecode to js compiler. It can be interesting for you.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 27, 2015 at 3:44 am ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22284))

> Anonymous says:
>
> Codename One works well with non-touch and we already have a desktop target which pro users are using. Since desktop apps are moving to look more like tablet apps this isn’t a big leap. 
>
> The main use case is enterprise requirements, sometimes just having a webapp is a starting point and if you don’t have it then its a problem. This can also provide support for niche platforms like Windows Phone, Firefox OS, tizen, Jolla etc. and allow governments/agencies that are required to support “everyone” to claim that they do.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 27, 2015 at 3:46 am ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22033))

> Anonymous says:
>
> Thanks, there are several. However those guys just use XMLVM for the actual heavy lifting, that’s not a very ideal solution performance wise and doesn’t solve the thread problem that we need solved.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 28, 2015 at 6:22 am ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22147))

> Anonymous says:
>
> That sounds like a really good idea! I’m very curious about performance. Unfortunately I can’t afford an enterprise account. Is it maybe a good idea to start some ‘crowdfunding’, to make this available for all subscribers if there’s enough money collected? I will certainly donate!
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 28, 2015 at 2:53 pm ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22177))

> Anonymous says:
>
> Crowdfunding seems unreliable, it works well for those with marketing skills but for something as niche as this I doubt it would provide value. 
>
> Performance should be good since we won’t have the overhead of reflows but obviously its something we can only prove when its fully operational.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 28, 2015 at 7:12 pm ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22302))

> Anonymous says:
>
> Yes I was not thinking about a crowdfunding platform but hoping that current users would like to contribute. Bit I don’t really know if there are enough users to do such a thing.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 28, 2015 at 7:22 pm ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-21477))

> Anonymous says:
>
> Like Maaike, I can’t justify an enterprise account, but would be prepared to tip in for new features. If you got 50 people contributing, say, $100, that very nearly covers your goal. Maybe you need a “Make a Donation” button so people can make a one-off contribution that can be used however you see fit.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)


### **Anonymous** — January 29, 2015 at 4:04 am ([permalink](https://www.codenameone.com/blog/web-app-build-target.html#comment-22320))

> Anonymous says:
>
> Organizing something like that is just not feasible without a platform like kickstarter and doing it in those platforms isn’t viable. 
>
> I understand the cost issue, having reviewed the financials I can’t see any other way we can “make this work” other than bolstering our enterprise developers. We need more personnel to maintain more platforms and a one time expense just isn’t enough. Having more enterprise developers will allow us to hire more employees and thus maintain this (and other ports).
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fweb-app-build-target.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
