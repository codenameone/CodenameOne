---
title: New Website Now Live!
slug: new-website-now-live
url: /blog/new-website-now-live/
original_url: https://www.codenameone.com/blog/new-website-now-live.html
aliases:
- /blog/new-website-now-live.html
date: '2015-03-14'
author: Shai Almog
---

![Header Image](/blog/new-website-now-live/newWebsiteLarge.png)

The new Codename One website is finally live now and you can check it out in all its glory at [codenameone.com](http://codenameone.com/).  
There are several things you need to know in order to make the transition smooth. First is that the new website supports https thruout so you can just navigate  
to <https://www.codenameone.com/> to get a secure site. This is forced implicitly when you  
go to the build server. In the old site we used an iframe on top of https for the build server, while this was secure it didn’t convey the trust  
level that should be conveyed. 

While the new build server UI should be pretty complete, there are some features we didn’t migrate yet, the old interface is still available if you  
need it right [here](https://codename-one.appspot.com/) and will be for quite some time. We are still working hard to improve the website  
but if you have any issues/broken links or suggestions please post them here.

We tried to migrate all the blog posts from the old blog, this worked with partial success specifically:

  * Dates/times on some of the older posts are incorrect
  * Hierarchy got broken
  * Emails are missing so you won’t get notified on responses to old comments.
  * Some comments were “just lost” – its hard to know which ones though…

However, the vast majority of the information (close to 1k comments) was preserved and provides a lot of important information  
about our older blog posts. We migrated all the older posts but some formatting might be broken, we couldn’t go over all of them  
(there are hundreds from just the past two years) but if we missed something please let us know!

## Migration Part II

Since Google killed Google Code over the weekend we are now faced with another migration to github. We started originally working  
with github and quickly moved to Google Code which in our opinions still has a lot of advantages despite its clunky and outdated  
interface. Specifically the biggest problem is migrating this amount of issues and a workspace of this size… 

Even simple projects that we tried to migrate such as the Google Native Maps project had failures in the migration process  
and this doesn’t leave us in a state of confidence about github fixing their problems.

Now before people jump to conclusions about “us not understanding “distributed version controls advantages” a brief history  
lesson about git… Do you know it was actually inspired by Sun and that we used a distributed versioning system there for years  
that IMO was FAR superior to git?

If you recall some of the [history of git](http://en.wikipedia.org/wiki/Git_%28software%29) it  
was inspired by [Bit Keeper](http://en.wikipedia.org/wiki/BitKeeper) which is a proprietary  
tool built by an ex-Sun guy based on the work he did at Sun. So at Sun we used an older tool that was pretty great and far  
better than git. It didn’t have size restrictions and even had a halfway decent gui. It had its problems (NFS reliance) but  
git has a whole set of other far greater problems such as painful conflict resolutions and assumptions about the  
way a workspace should be.

So to workaround the issues with github and remove the dependencies on google code we are now moving all the  
binary files that we have in Google code to our servers with some exceptions. The JavaDocs are already hosted  
in this site, we will also migrate the Eclipse repository here so if you added the old URL you will need to update it to  
<http://codenameone.com/eclipse/site.xml>. 

Since other IDE’s (NetBeans & IntelliJ) both have a great central repository we will retire our own  
repositories and you should get your updates from there. Make sure to do that since updated libs in the  
old repository will no longer work…
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **ftp27** — March 17, 2015 at 3:12 pm ([permalink](/blog/new-website-now-live/#comment-22081))

> ftp27 says:
>
> Very sad bugs in new build server. Builds automatically deleted after build.


### **Shai Almog** — March 17, 2015 at 6:01 pm ([permalink](/blog/new-website-now-live/#comment-22091))

> Shai Almog says:
>
> The build server wasn’t replaced, its just a UI on top of the old server which you can still see here: [https://codename-one.appspo…](<https://codename-one.appspot.com/>) so all your builds should be perfectly fine!  
> I’m not sure if I understand what you mean, did you try reloading the page?  
> I see a build in progress here. Old builds are only kept up to 5 past builds and only for paying users. This was always the case.


### **ftp27** — March 19, 2015 at 10:57 am ([permalink](/blog/new-website-now-live/#comment-22136))

> ftp27 says:
>
> It’s works. Thanks!


### **Chidiebere Okwudire** — May 20, 2015 at 10:25 am ([permalink](/blog/new-website-now-live/#comment-22400))

> Chidiebere Okwudire says:
>
> Is it on purpose that the new website no longer gives a clear overview of all the platforms supported by CN1? I personally find that a sad omission. Can the list of supported platforms be re-introduced?
>
> Note, this information is still available via wikipedia at [http://en.wikipedia.org/wik…](<http://en.wikipedia.org/wiki/Codename_One>)


### **Shai Almog** — May 20, 2015 at 5:15 pm ([permalink](/blog/new-website-now-live/#comment-22193))

> Shai Almog says:
>
> Why is that necessary?  
> The wikipedia article is actually out of date since we now support a JavaScript build target so our platform support is wider.


### **Chidiebere Okwudire** — May 21, 2015 at 7:08 am ([permalink](/blog/new-website-now-live/#comment-22230))

> Chidiebere Okwudire says:
>
> Suppose a new potential subscriber stumbles on your website, won’t it be very handy to see an overview of what platforms are supported? Right now all one sees is ‘Write Once Run Anywhere’ and the question that immediately comes to mind is: “Where is ‘anywhere’? I couldn’t find any single page on the website that answers this basic question. And when one googles for “CodenameOne supported platforms”, the first hit is that wiki page which you said is out-dated so that doesn’t help.
>
> Like I’ve mentioned to you before, I still think the marketing of this great product can/should be better 😉

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
