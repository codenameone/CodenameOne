---
title: GZip Support
slug: gzip-support
url: /blog/gzip-support/
original_url: https://www.codenameone.com/blog/gzip-support.html
aliases:
- /blog/gzip-support.html
date: '2013-06-16'
author: Shai Almog
---

![Header Image](/blog/gzip-support/gzip-support-1.png)

  
  
  
  
![Zip](/blog/gzip-support/gzip-support-1.png)  
  
  
  

We now have new support for GZipInputStream and GZipOutputStream thanks to the great work done by the guys in the  
[  
JZLib  
](https://github.com/ymnk/jzlib)  
project, we ported their work into the project class hierarchy and added a GZConnectionRequest which will automatically unzip an HTTP response if it is indeed gzipped. 

  
By default this class doesn’t request gzipped data but its pretty easy to do so just add the HTTP header  
  
Accept-Encoding: gzip e.g.:  
  
GZConnectionRequest con = new GZConnectionRequest();  
  
  
con.  
  
addRequestHeader(“Accept-Encoding”, “gzip”);

  
Do the rest as usual and you should have smaller responses by potential.  

  
We thought about adding this capability to the global ConnectionRequest but eventually decided not to do so since it will increase the size of the distribution to everyone. If you do not need the gzip functionality the obfuscator will just strip it out during the compile process.  
  
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 17, 2013 at 8:31 am ([permalink](/blog/gzip-support/#comment-21691))

> Anonymous says:
>
> This is great news ! I will update my blog post about compression with this, keep up the good work !
>



### **Anonymous** — February 6, 2014 at 5:47 am ([permalink](/blog/gzip-support/#comment-21434))

> Anonymous says:
>
> Is it also supported on the WebBrowser component when fetching HTML?
>



### **Anonymous** — February 7, 2014 at 3:13 am ([permalink](/blog/gzip-support/#comment-21767))

> Anonymous says:
>
> Since the web browser uses native connections and doesn’t go thru connection request gzip should “just work” for that case.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
