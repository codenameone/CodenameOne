---
title: Don't Block The UI
slug: dont-block-the-ui
url: /blog/dont-block-the-ui/
original_url: https://www.codenameone.com/blog/dont-block-the-ui.html
aliases:
- /blog/dont-block-the-ui.html
date: '2014-11-16'
author: Shai Almog
---

![Header Image](/blog/dont-block-the-ui/dont-block-the-ui-1.png)

  
  
  
  
![Picture](/blog/dont-block-the-ui/dont-block-the-ui-1.png)  
  
  
  

  
  
  
  
  
  
  
  
I’ve talked with many end users about their badly written apps  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
grievances  
  
  
  
  
  
and I’ve come to the conclusion that it isn’t a matter of native vs. cross platform or even HTML. Its a frustration issue, driven by unintuitive apps (hidden gestures etc.) and slow performance.  
  
The slow performance bit is the most misunderstood, there is a “feeling” of sluggishness that people complain about but when users complain about performance its usually not related to that but rather something far more mundane… When users complain about performance its often over the infinite rotating wheel covering the entire UI (InfiniteProgress dialog in Codename One), which makes them stand around like idiots waiting for their phone to let them touch it again! 

This is annoying, frustrating and I can name quite a few native apps that do this for routine data! 

When we added the InfiniteProgress dialog it was intended for use only with truly blocking operations e.g. login. However, looking back I can see that we too abused this capability in our demos and this might have inspired some developers to do the same.

One of the advantages native apps have is the ability to cache everything, we can always show locally cached data and fetch in the background. We can indicate the data is stale in some way or that we haven’t connected when working with live data but we shouldn’t block the user when possible. You can do something trivial to indicate progress e.g.:  

  
  
  
  
  
  
  
  

* * *

  
  
  
  
  
  
  
  
This is naturally trivial but we can go much further than this by caching data locally and writing code that works seamlessly when running online/offline. Classes such as  
[  
URLImage  
](http://www.codenameone.com/3/post/2014/03/image-from-url-made-easy.html)  
and ImageDownloadService can become very handy since they seamlessly handle such cases. 

You can display a “stale” indicator on top of a component to show that you are in offline mode or that the component contains changes that weren’t yet synchronized to the server, a trick we sometimes do is to place an InfiniteScroll on top of a component to indicate that it is being synchronized. Its important not to overdo this since the InfiniteScroll triggers repaints that will slow the responsiveness of your application!  
  
E.g:  
  

  
  
  
  
  
  
  

  
  
  
  
  
  
  
  
Even if your application is very server bound, you can show the last UI forms from the last server connection. This is crucial since often a user would check his device to see the last thing that was open and if the application was suspended since the last run he wouldn’t like to restart the process of digging deeper into the device. 

This isn’t a new concept, the Palm Pilot was remarkably successful thanks to its amazing performance despite having relatively weak hardware even compared to its contemporaries. Palm demanded that the software act like hardware, when you press a physical button you expect instant gratification and they wanted apps to act in the same way (admittedly apps at that time usually didn’t do networking).  
  

  
  
  
  
  
  

  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Javier Anton** — August 17, 2020 at 8:15 am ([permalink](/blog/dont-block-the-ui/#comment-24329))

> [Javier Anton](https://lh3.googleusercontent.com/a-/AAuE7mDRjHkEvAZNXquh9p7Oo00ey1yOwNzZ0SrFwD0IVA) says:
>
> I will add here my 2 cents… blocking the UI of a BrowserComponent will result in the underlying JS being stopped by Safari. So it is very bad to block a Form containing a BrowserComponent on iOS  
> It has taken me a long time to realise this… and from my own tests I can see that Apple has restricted the bg JS even more recently
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
