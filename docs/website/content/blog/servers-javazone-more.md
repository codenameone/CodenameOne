---
title: Servers, JavaZone & More
slug: servers-javazone-more
url: /blog/servers-javazone-more/
original_url: https://www.codenameone.com/blog/servers-javazone-more.html
aliases:
- /blog/servers-javazone-more.html
date: '2014-09-06'
author: Shai Almog
---

![Header Image](/blog/servers-javazone-more/servers-javazone-more-1.jpg)

  
  
  
[  
![JavaZone Picture](/blog/servers-javazone-more/servers-javazone-more-1.jpg)  
](/img/blog/old_posts/servers-javazone-more-large-2.jpg)  
  
This represents JavaZone to me  
  

I’ll be flying to Oslo this morning to speak at JavaZone and meet with some local community members, I’m very excited about this! If you are attending JavaZone and haven’t yet signed up for the workshop please do so now.  
  
  
  
We also updated our home page and gallery with some up to date statistics  
  
  
  
  
  
We just pushed out new build servers for Android/Blackberry & J2ME that  
  
use SSD disks to speed up compilation even further, since the build on these platforms is already pretty fast I’m not sure you will see a noticeable difference but we are concerned that we might have mistakes in the setup of the new server images. If you get suspicious build failures on those 3 platforms that only happen sometimes let us know. 

  
We also pushed out an experimental fix for a long standing  
  
  
  
IDEA issue that has been around for a while, if you are one of the developers affected by the issue please let us know if this is still happening and if so please provide a log for this so we can debug it.

  
Chen has made some interesting performance improvements for the graphics pipelines, they are still not up on the build servers but hopefully they will be noticeable. We also added a new API to get all the contacts in one go, this works much faster on Android then extracting the contacts one by one but is still pretty expensive so a thread is still essential. This new API effectively consists of two methods in Display:  
  
  
  
  
  
Contact[] getAllContacts(boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress);  
  
isGetAllContactsFast();  
  
  
  
  
  
You might not want to invoke this method unless  
  
  
  
isGetAllContactsFast() returns true, otherwise it will fallback to code that effectively loops all the contacts to create an array.  
  
  
  
  
We also added a callback method to ConnectionRequest:  
  
  
protected void cookieReceived(Cookie c)  
  
  
  
  
This should make cookie handling much simpler.  
  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
