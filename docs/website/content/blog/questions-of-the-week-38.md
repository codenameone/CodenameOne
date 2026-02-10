---
title: Questions of the Week 38
slug: questions-of-the-week-38
url: /blog/questions-of-the-week-38/
original_url: https://www.codenameone.com/blog/questions-of-the-week-38.html
aliases:
- /blog/questions-of-the-week-38.html
date: '2017-01-12'
author: Shai Almog
---

![Header Image](/blog/questions-of-the-week-38/qanda-friday2.jpg)

Codename One 3.6 is finally landing early next week, this means that today there is no Friday release and we might skip it next week too so we can rest from this long release process. Once that is out of the way we can finally set our sights on 3.7.  
We already have a long wishlist for that release and I hope we’ll be able to deliver on that.

On stackoverflow there were quite a few questions but I’d like to only focus on one this week…​

[Stefan Eder](http://stackoverflow.com/users/5695429/stefan-eder) asked for [overshadowing](http://stackoverflow.com/questions/41607760/why-is-overshadowing-not-supported-with-codename-one) which is the process of overriding our implementation with his changes.  
This isn’t the first time people asked for that but we won’t deliver it. Doing this creates **huge** problems:

  * Developers don’t file issues or submit fixes instead they make local fixes

  * Developers break things due to complex behaviors then try to get support and blame us for the issues

We have a process of [submitting patches to Codename One](/blog/how-to-use-the-codename-one-sources.html), patches are always accepted quickly when they are valid. If something needs fixing that’s what you need to do. If you need a hack then submit a patch that defines the extension point that you need. That’s why we are open source…​

In the past this might have been painful as you would need to wait until we updated the servers, but since changes go in every week in recent revisions this is no longer an issue. Don’t think of it as “contributing”, think of it as free code reviews where the entire community pulls together to improve your work…​

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
