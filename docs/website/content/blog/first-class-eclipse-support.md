---
title: First Class Eclipse Support
slug: first-class-eclipse-support
url: /blog/first-class-eclipse-support/
original_url: https://www.codenameone.com/blog/first-class-eclipse-support.html
aliases:
- /blog/first-class-eclipse-support.html
date: '2017-01-09'
author: Shai Almog
---

![Header Image](/blog/first-class-eclipse-support/eclipse.jpg)

Our eclipse IDE support has been around for quite a while now but has never stood up to the quality and update pace of NetBeans. Recently even our IntelliJ/IDEA support has surpassed the quality of our eclipse plugin and the blame should be on us.

One of the problems is that our team doesn’t use eclipse and uses a diverse set of OS’s (Macs/Windows) which is problematic when sharing workspaces. Integrating the eclipse plugin build into our standard release process proved really hard and it ended up eventually as something we need to do but never got around to do it…​

We’ve spent the last couple of weeks trying to get eclipse to work with the release build process and it seems we got it right. Our latest version of the eclipse plugin was released with the other plugins and from now on we hope all the plugins will be released together!

Please try updating the eclipse plugin, it should be 3.6.0 right now and would ideally be in sync with the versions from the other IDE’s.

We integrated the new demos that are already a part of the newer IDE’s and we’ll try to better keep up with the changes from now on. The one major feature that is still missing from the eclipse plugin is support for cn1lib creation which we hope to add as we move forward.

As part of this work I was very impressed by the improvements made in eclipse neon which is **much** better than previous iterations of eclipse (speaking as a Mac user). The IDE has been responsive and the UI has been closer to what one would expect in such a case.

If you run into weird errors or regressions in the new eclipse plugin update please let us know ASAP. These might be related to the codebase transition and it’s important we get a handle on them quickly!

__ |  **Update:** It seems older versions of eclipse didn’t react well to the new plugin so with the new plugin we requite Neon and Java 8   
---|---  
  
### New Updates to All Plugins

We just released new updates to all the plugins for the 3.6 release. We are now in a week long code freeze and if there are no regressions there won’t be an update for the next two weeks. If we do find a regression we’ll push out a new 3.6 release candidate during the week and possibly postpone a weekly update after that.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **dingfelder** — January 12, 2018 at 8:37 am ([permalink](https://www.codenameone.com/blog/first-class-eclipse-support.html#comment-23679))

> dingfelder says:
>
> Hi – I’m using the eclipse version – a quick comment: the build process isn’t copying all the files the way I would expect – for instance, the sql demo requires a db file to be copied to the cn folder – currently this has to be done manually. I suspect this is hitting anyone trying to run the demos. cheers
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Ffirst-class-eclipse-support.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
