---
title: Updates and Expansion
slug: updates-expansion
url: /blog/updates-expansion/
original_url: https://www.codenameone.com/blog/updates-expansion.html
aliases:
- /blog/updates-expansion.html
date: '2019-11-23'
author: Shai Almog
---

![Header Image](/blog/updates-expansion/generic-java-2.jpg)

I haven’t blogged in a while. I’ve been busy working with a couple of startups, some enterprise customers and bringing new people on-board. Steve has been great in picking up some of the slack but his plate is too full to blog with the same frequency I had so the blog slowed down a bit during this time. I hope to pick it back up to a weekly post regiment but my schedule is just so tight I barely have time to breath.

The good news is that we’re expanding a bit and recently hired a much needed graphics designer to overhaul everything around here with a new coat of paint. Steve and I spent a lot of time with him and we’re super excited about this!

We’ve also made some hirings in the development area which I’m pretty bullish about, hopefully they will turn out nicely too!

### Location Regression

Over the weekend we had a hard time with the location API on Android. In fact we had that last weekend too and we eventually reverted last weeks release to the one from the prior week. This week we pushed through and hopefully we resolved the issues. However, if you’re experiencing issues with the location API please let us know ASAP.

The reason behind this is the usual flurry with Google. They keep changing the behavior of devices with every API update. This is very problematic for applications that rely on background location, as such it forced us to update to play services version `12.0.0` which caused a cascade of issues across the board. Unfortunately this is inevitable as Google requires updates to newer versions for app submissions.

### Version 7.0

We postponed version 7.0 multiple times by now and I still don’t see how we’ll make the current deadline which is set to Christmas eve of all dates…​ I’m afraid we’ll need to push that back again with our current rate of progress, hopefully our additional developer resources will help us align more closely to these schedules in the future.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Boniface Githinji** — November 25, 2019 at 9:43 am ([permalink](/blog/updates-expansion/#comment-24268))

> [Boniface Githinji](https://lh3.googleusercontent.com/a-/AAuE7mA-hb6mEOeaoepbAdoZXUhwzjcubsocbfFBQwAcaA) says:
>
> Great progress Shai. Happy to hear that you are finally getting a graphic designer. A tool that was already great now will become even better.
>



### **Ch Hjelm** — November 28, 2019 at 1:29 pm ([permalink](/blog/updates-expansion/#comment-24270))

> [Ch Hjelm](https://lh3.googleusercontent.com/-D-GIdg1DASY/AAAAAAAAAAI/AAAAAAAAAAA/AAKWJJMNPAANy-qutSCtrOnc0icrNWiskQ/photo.jpg) says:
>
> Graphics designer, great, great news. Probably what will have the biggest impact on promoting CN1. Want to share any thoughts in what his top prio tasks will be? I’d put really nice, fully developed (all components covered, every detail fine-tuned to ‘Apple level’) themes, so truly beautiful out of the box. And we’ll structured so easy to customize fonts, text size, colors themes etc. And available in CSS as « best practice » examples 🙂
>



### **Shai Almog** — November 29, 2019 at 3:04 am ([permalink](/blog/updates-expansion/#comment-24273))

> Shai Almog says:
>
> I very much agree with that but some of that work (e.g. best practices and CSS examples) is something we need to do. I’m still thinking about the best way to represent CSS snippets/solutions for common UI designs.
>



### **Ch Hjelm** — April 18, 2020 at 7:26 am ([permalink](/blog/updates-expansion/#comment-21394))

> [Ch Hjelm](https://lh3.googleusercontent.com/-D-GIdg1DASY/AAAAAAAAAAI/AAAAAAAAAAA/AAKWJJMNPAANy-qutSCtrOnc0icrNWiskQ/photo.jpg) says:
>
> Hi Shai, any updates on the business front you can share? It is obvious that something has changed (eg fewer blog posts), so it’d be great to hear news directly ‘from the horse’s mouth’
>



### **Shai Almog** — April 18, 2020 at 7:52 am ([permalink](/blog/updates-expansion/#comment-21395))

> Shai Almog says:
>
> Hi,  
> not much has changed. I made an experiment a year or so ago of blogging daily because I wanted to check if it had a business impact.  
> It didn’t.  
> I reduced it to weekly blogging in 2020 but I don’t always have inspiration so I blog even less. 
>
> We’re working hard on the new website, hopefully when that’s up we’ll also have more energy for that. Ideally we’ll have more personal time too when our environment better adopts to covid.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
