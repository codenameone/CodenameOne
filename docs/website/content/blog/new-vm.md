---
title: New VM
slug: new-vm
url: /blog/new-vm/
original_url: https://www.codenameone.com/blog/new-vm.html
aliases:
- /blog/new-vm.html
date: '2014-04-22'
author: Shai Almog
---

![Header Image](/blog/new-vm/new-vm-1.png)

  
  
  
  
![Picture](/blog/new-vm/new-vm-1.png)  
  
  
  

You might have noticed things have been a bit quiet with new features recently. That is because we had a bit of a holiday around here and because we spent a great deal of time working on a new VM for iOS. This work is still ongoing and basic things such as the garbage collection scheme are still incomplete but we are now ready to talk about the motivations and direction with this new VM. 

Initially when we started working on Codename One we assumed we would have to build our own VM for iOS since there were no decent options at the time. However as luck would have it, the XMLVM guys just built a new backend to support iOS which was pretty good and we ended up going with XMLVM for the infrastructure instead of building our own VM. This was a great help since it allowed us to release our initial beta within 3 months of forming Codename One!

However, this had some downsides specifically:  
  
1\. XMLVM is huge and very generic so its remarkably hard to fix.  
  
2\. Its size and generic architecture make the translation process slow which slows down the builds.  
  
3\. It uses the boehm gc which stalls quite a bit.  
  
4\. It uses Harmony for the class libraries which are much larger than what we actually need resulting in slower compilations and larger/slower executables.  
  
5\. It translates dalvik code to iOS instead of bytecode directly which is slightly unintuitive and potentially suboptimial for some cases.

Normally we would have other priorities but Apple has effectively forced our hand to do this when they released xcode 5.1 which broke compatibility with boehm code. Right now this isn’t a problem but Apple might suddenly decide to force all developers to migrate to a new version of xcode (which they did with 5.0) and we don’t want to get caught in such a situation scampering to patch an issue.

We thought long and hard about fixing XMLVM for our needs but eventually came to the conclusion that it would be simpler to start with a clean slate since XSLT is such a painful way to do something of this type. We were also having problems with our usage of XMLVM on Windows Phone so the replacement seemed to make more sense across the board (although we haven’t actually started the Windows Phone work).

We did look extensively at other projects that have sprung up in the past couple of years to address these issues but all seemed to be suffering from the problem of addressing a too large problem space, using harmony (or worse open JDK) and using boehm (which is a fine GC except for the stalls). The exception here is J2ObjC which isn’t really a VM and isn’t intended for this use case at all.

You might be wondering how this will effect you as a Codename One developer.

It won’t really, except for the fact that at some point in the future your builds will become faster and perform better. We chose a source architecture that is very similar to XMLVM since we liked the basic concepts of XMLVM and we think its the right direction going forward to be as compatible with Apple’s way of doing things as possible. All the things you rely on including native interfaces etc. should work just the same and the screenshot you see in this post is taken from the kitchen sink running in the new VM!

Since there are always potential incompatibilities we will offer a build flag that will allow you to build with the old XMLVM backend for quite a while and we will maintain both approaches. We don’t currently have a fixed date for the release of this new VM since we have a lot of other priorities to deal with and this is a pretty big effort but we intend to have it ready before Java One. Ideally sooner.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — April 30, 2014 at 2:04 am ([permalink](https://www.codenameone.com/blog/new-vm.html#comment-21505))

> Anonymous says:
>
> [http://oss.readytalk.com/av…](<http://oss.readytalk.com/avian/>)
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-vm.html)


### **Anonymous** — April 30, 2014 at 2:20 am ([permalink](https://www.codenameone.com/blog/new-vm.html#comment-21924))

> Anonymous says:
>
> Steve Hannah already ported Codename One to Avian and it performed worse than XMLVM while taking up quite a bit of space. It suffers from pretty much all of the problems I illustrated above.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fnew-vm.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
