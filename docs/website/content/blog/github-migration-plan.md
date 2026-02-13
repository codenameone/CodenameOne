---
title: Github Migration Plan
slug: github-migration-plan
url: /blog/github-migration-plan/
original_url: https://www.codenameone.com/blog/github-migration-plan.html
aliases:
- /blog/github-migration-plan.html
date: '2015-03-23'
author: Shai Almog
---

![Header Image](/blog/github-migration-plan/github-logo.jpg)

As you may know Google is ending support for Google code effectively forcing us to migrate to github. While  
we like a lot of things about github their 1gb per workspace restriction makes the migration process rather difficult  
since we have to manually delete some histories for binary files we committed into SVN.  
Making matters worse, Google’s so called “automated tools” are ridiculously simplistic and don’t support anything  
like this or migration of more than 1000 issues! 

This effectively means we will need to perform the migration manually, we will probably discard the existing tags  
but we will try to preserve workspace history and the issues. This Friday we will close the issue tracker, all new issues  
will be ignored, we will then move all the issues to a new github project that we will create for this purpose.  
We will also move the code base and all future commits to the new repository but since commits are currently limited  
to Codename One team members this shouldn’t be a problem. 

Once we will finish the migration we will leave the existing project in Google code for reference purposes. 

Hopefully we will finish the migration process over the weekend but from past experience I’m rather pessimistic.  
The svn2git tools were pretty badly broken the last time we checked them.  
Hopefully our prior experience with such migration attempts will soften the blow and make the process easier  
this time around.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Alexandre Richonnier** — March 28, 2015 at 1:24 pm ([permalink](https://www.codenameone.com/blog/github-migration-plan.html#comment-22248))

> Alexandre Richonnier says:
>
> Maybe you could give a chance to bitbucket?
>
> For open source projects, there ‘s no limit.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgithub-migration-plan.html)


### **Shai Almog** — March 28, 2015 at 3:01 pm ([permalink](https://www.codenameone.com/blog/github-migration-plan.html#comment-22261))

> Shai Almog says:
>
> We looked at that and several others. While I’d love that it does seem that pretty much everyone is going to Github.  
> I like Github in general, the site and tools are amazing. I think the main problem is git itself but since most people are moving in that direction we should probably align rather than fight it.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgithub-migration-plan.html)


### **cvconover** — August 11, 2015 at 12:58 pm ([permalink](https://www.codenameone.com/blog/github-migration-plan.html#comment-22435))

> cvconover says:
>
> Shai,
>
> Just wondering what you do not like about git. It is the first vcs that I actually enjoyed working with from the command line.
>
> Cheers  
> Craig
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgithub-migration-plan.html)


### **Shai Almog** — August 11, 2015 at 2:02 pm ([permalink](https://www.codenameone.com/blog/github-migration-plan.html#comment-21545))

> Shai Almog says:
>
> Actually I like GIT much better now with the improved tooling (3 years made a big difference).  
> I love distributed versioning and loved it at Sun where it was pioneered (the guy that inspired Linus to write GIT and effectively outlined its architecture == ex Sun guy).  
> The main issue I had with GIT back then was that its conflict resolution approach was HORRIBLE, this has been greatly improved and is now in SVN level. The secondary issue was that it encrypts everything, which is great for a commercial project but for an open source project its not very useful. We had a conflict and merge broke down badly, unfortunately because everything was encrypted we effectively lost the data and couldn’t find it in history. With the old Sun unencrypted tools (SCCS) and even with SVN everything is stored as text files and if something goes wrong you can just look thru that.
>
> Overall, I’m much happier with Git today and I’m generally happy we made the move.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fgithub-migration-plan.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
