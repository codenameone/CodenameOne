---
title: Weekly Release, Developer Guide & JavaScript Promotion End
slug: weekly-release-developer-guide-javascript-promotion-end
url: /blog/weekly-release-developer-guide-javascript-promotion-end/
original_url: https://www.codenameone.com/blog/weekly-release-developer-guide-javascript-promotion-end.html
aliases:
- /blog/weekly-release-developer-guide-javascript-promotion-end.html
date: '2016-03-17'
author: Shai Almog
---

![Header Image](/blog/weekly-release-developer-guide-javascript-promotion-end/documention.jpg)

We just made our first weekly release today, in the coming weeks when you send a build or update the client  
libraries you should get a new version every Friday. We hope this will help us provide better consistency between  
the docs/device builds and the simulator.

We finally completed the overhaul of the developer guide which now clocks at 732 pages. This doesn’t mean the  
guide is perfect or covers everything there is but rather that we covered the bigger pieces and reviewed the whole  
document for accuracy and relevance. There are still big pieces we’d like to add e.g. a section about the common  
cn1libs is something that I would personally like to see.

__ |  If there is something you feel isn’t covered properly in the docs let us know in the comments below  
or just edit the wiki directly!   
---|---  
  
Now that the guide is complete and the JavaDocs are far improved we are targeting better videos for the website and  
IntelliJ support. We are still overworked with the new Windows port so that is slowing our general progress on  
everything else.

### JavaScript Promotion Ended

We ended the [JavaScript build promotion](https://www.codenameone.com/blog/a-thank-you-an-important-update-on-android-builds.html)  
we started last month. If you were a pro user and remain a pro user  
you should have that ability into 2017. Everyone else now needs an enterprise license to enjoy that feature.

Notice that if you let your subscription lapse you might lose this ability since we won’t have a way to distinguish your builds  
from everyone else.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Chidiebere Okwudire** — March 18, 2016 at 7:15 am ([permalink](https://www.codenameone.com/blog/weekly-release-developer-guide-javascript-promotion-end.html#comment-22526))

> Chidiebere Okwudire says:
>
> Good to hear there’s progress on the WP port. Is it already possible to predict when we can expect the first release? 🙂
>



### **Shai Almog** — March 18, 2016 at 7:30 am ([permalink](https://www.codenameone.com/blog/weekly-release-developer-guide-javascript-promotion-end.html#comment-22661))

> Shai Almog says:
>
> One of the reasons we didn’t want to go into this whole mess is the level of complexity. We put in a tremendous amount of work and are still at a stage where we might need to throw everything away and start from scratch!
>
> We are trying to take a shortcut that would save us the need to port ParparVM to Windows, if we need to go thru that route it could take quite a while.
>



### **AdAlbert** — April 3, 2016 at 7:29 am ([permalink](https://www.codenameone.com/blog/weekly-release-developer-guide-javascript-promotion-end.html#comment-22789))

> AdAlbert says:
>
> I think you don’t need ParparVM for Windows port because gc is implemented internally in C# and this is main advantage od ParparVM for iOS.
>



### **Shai Almog** — April 4, 2016 at 2:26 am ([permalink](https://www.codenameone.com/blog/weekly-release-developer-guide-javascript-promotion-end.html#comment-21625))

> Shai Almog says:
>
> If we would go with ParparVM (which we probably won’t as we made good progress with iKVM) we would use C and not C#. We had a horrible experience when translating Java bytecode to C#. They differ at just the right amount to make translation darn near impossible.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
