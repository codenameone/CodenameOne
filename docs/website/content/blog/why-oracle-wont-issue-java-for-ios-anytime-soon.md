---
title: Why Oracle won't issue Java for iOS anytime soon
slug: why-oracle-wont-issue-java-for-ios-anytime-soon
url: /blog/why-oracle-wont-issue-java-for-ios-anytime-soon/
original_url: https://www.codenameone.com/blog/why-oracle-wont-issue-java-for-ios-anytime-soon.html
aliases:
- /blog/why-oracle-wont-issue-java-for-ios-anytime-soon.html
date: '2013-01-20'
author: Shai Almog
---

![Header Image](/blog/why-oracle-wont-issue-java-for-ios-anytime-soon/why-oracle-wont-issue-java-for-ios-anytime-soon-1.jpg)

  
  
  
[  
![Picture](/blog/why-oracle-wont-issue-java-for-ios-anytime-soon/why-oracle-wont-issue-java-for-ios-anytime-soon-1.jpg)  
](/img/blog/old_posts/why-oracle-wont-issue-java-for-ios-anytime-soon-large-2.jpg)

They say that today all companies are software companies. In less than 5 years all companies will be mobile companies. So why isn’t Oracle “getting it”, why aren’t they on the iPhone, on Android and even Windows Phone? 

This  
[  
OTN thread  
](https://forums.oracle.com/forums/thread.jspa?threadID=2448461&&t)  
has been going on for some time now with people constantly chiming in with various uninformed opinions regarding their desire for an official Java for all platforms from Oracle.  
  
This won’t happen in the next two years or so, don’t delude yourselves.  
  
No we don’t have a vested interest here, if Oracle issues Java FX for iOS it will be great for us. We will be able to use their implementation (if it were any good, more on that below) and still provide value.

Read below for more detailed explanation from a former insider as to why this just won’t happen.

Chen used to be a Sun employee and I was a Sun contractor for quite a few years, we were there for the Oracle takeover as well. I can’t talk too much about what I know from within, its not so much a contractual issue as a personal moral issue (respect the privacy of the guys paying your bills). However, there is one thing that I think no one will mind me disclosing that explains perfectly well why Oracle removed all sessions about the iPhone from Java One (except for our session which wasn’t an Oracle sponsored section). 

One of the early feelings we got before the merger completed was the basic difference between Sun and Oracle and it boiled down to this:  
  
With Sun we used to go to customers/trade shows and show the cool stuff we had that wasn’t yet a product. E.g. showing off cool new JavaFX tools that didn’t have an ETA yet. This was ingrained in the hacker mentality and core to Sun, why show people the stuff that’s available to download?  
  
Don’t they know already?

Oracle is the exact opposite, they NEVER show something that isn’t a product or won’t be a product very soon. This is actually quite clever, people aren’t aware of lots of the stuff that’s available and if you talk about your pipe dream (which is cool) there is no “action item” to download a try it. You need people’s attention focused on what they can buy (obvious why Oracle was profiting while Sun was losing).

So Oracle’s removing of the JavaFX on iOS talks from Java One is simply a matter of them not having a concrete product in the pipeline (update: just to clarify, this is an educated guess not a statement of fact).

Now you may ask: Why not?

That’s actually a much easier answer and as usual it divides into several different answers:  

  1. There is no business there – Oracle released a Java based solution for iOS backend for the ADF team. This is a tool that’s only useful if you buy a server license (minimum 20k USD), so there is a clear business here.  
  
Java is generally an odd duck in Oracle’s tools, they just don’t build stuff that doesn’t make business sense (like Java itself). 
  2. This is a consumer product – Oracle is an enterprise company. Yes they are trying to break some of that mold but old habits die hard, they don’t really understand the business and they don’t really know how to build consumer products.  
  
Sun also sucked with user facing projects so really this isn’t very different (and keep in mind, that Sun never made a Java for iPhone release either despite Apple removing the restrictions WELL before the acquisition). 
  3. It will suck – the problem here is Apple. Apple disallows JIT’s in its license (self modifying code), mostly for security reasons but probably also to block things like this. That is why you can’t ship a custom built webkit with your application (no V8 JavaScript engine for Chrome on iOS which is why it sucks on iOS).  
  
We get around it by translating the Java bytecode to C and compiling it, this gives us native (or better) performance.  
  
I don’t know about Oracle here, but this sort of architecture would never fly at Sun. Java is a virtual machine with a JIT, that is a religion within Sun and I assume the same is true for the Sun engineers who stayed at Oracle. 
  4. It will suck worse – not only will it be slow because of the VM, it will be slow because of JavaFX (here we can actually help if Oracle chooses to do option 3 well).  
  
Adobe with its amazing skills in vector graphics programming is finding it remarkably hard to build a high performance vector graphics rendering engine in iOS. They complain that Apple doesn’t expose the internal GPU behavior.  
  
Frankly, I understand Apple here. Documenting the GPU on the level Adobe needs is REALLY hard. Supporting this against potential driver issues and attacks isn’t simple, that’s why they have Core Animation.  
  
Java FX can’t use Core Animation (just like Adobe can’t) and will run into the exact same problems Adobe hit. I have a great deal of respect for the engineers on the Java FX team, they are pretty clever. But that’s not good enough.  
  
We don’t run into those pitfalls since we are pretty used to device limitations, we pre-render everything important as raster images (which is what most mobile developers do anyway). This might not have the same “cool” graphics geek sheik, but it actually provides amazing looks because prerendering often looks better. Sure there are compromises about what you can do, but you will find pre-rendered graphics in most of the leading iOS apps despite the availability of vector graphics. Its easier, faster and flexible enough. 

So if you are looking for JavaFX on iOS, Android or Windows Phone then sorry. Just won’t happen.  
  
We are trying to help, but Java FX is a dead end technology as illustrated by the famous graph at the top of this article. Everyone will be on mobile which will exceed everything WinTel ever was and FX isn’t well suited for today’s mobile devices. It probably can’t be fixed either since it relies on a Scene-Graph approach which just isn’t very portable to device specific Scene-Graphs. It would be possible to implement it over OpenGL ES but that has many issues. 

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 27, 2013 at 7:36 am ([permalink](https://www.codenameone.com/blog/why-oracle-wont-issue-java-for-ios-anytime-soon.html#comment-21773))

> Anonymous says:
>
> I’ve always liked Swing, but JavaFX seemed to me an answer to a question nobody asked – I don’t think the world really needed another graphics scripting language. Sun didn’t do themselves any favours either by appearing to favour JavaFX over Swing for a while. 
>
> As you say, now it’s pretty much moot.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwhy-oracle-wont-issue-java-for-ios-anytime-soon.html)


### **Anonymous** — February 11, 2013 at 8:17 pm ([permalink](https://www.codenameone.com/blog/why-oracle-wont-issue-java-for-ios-anytime-soon.html#comment-24250))

> Anonymous says:
>
> I guess you were proven wrong in less that one month: [http://fxexperience.com/201…](<http://fxexperience.com/2013/02/february-open-source-update/>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwhy-oracle-wont-issue-java-for-ios-anytime-soon.html)


### **Anonymous** — February 12, 2013 at 2:04 am ([permalink](https://www.codenameone.com/blog/why-oracle-wont-issue-java-for-ios-anytime-soon.html#comment-21806))

> Anonymous says:
>
> Cool. I don’t mind being wrong. 
>
> This isn’t a binary solution/product which is the main thing covered by this article. To be frank I didn’t think Oracle would ever do even something such as an Open Source code dump so that is surprising.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwhy-oracle-wont-issue-java-for-ios-anytime-soon.html)


### **Anonymous** — May 14, 2014 at 3:50 am ([permalink](https://www.codenameone.com/blog/why-oracle-wont-issue-java-for-ios-anytime-soon.html#comment-21797))

> Anonymous says:
>
> Now every one knows why. They were waiting to sue Android. Either they will get a continuous revenue or a one time big fine.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fwhy-oracle-wont-issue-java-for-ios-anytime-soon.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
