---
title: Cloud Object Viewer
slug: cloud-object-viewer
url: /blog/cloud-object-viewer/
original_url: https://www.codenameone.com/blog/cloud-object-viewer.html
aliases:
- /blog/cloud-object-viewer.html
date: '2013-04-29'
author: Shai Almog
---

![Header Image](/blog/cloud-object-viewer/cloud-object-viewer-1.png)

  
  
  
[  
![Picture](/blog/cloud-object-viewer/cloud-object-viewer-1.png)  
](/img/blog/old_posts/cloud-object-viewer-large-2.png)

Working with the  
[  
Cloud Object API  
](/javadoc/com/codename1/cloud/package-summary.html)  
can sometimes be difficult. The data isn’t tabular and understanding the concepts such as indexes and scopes for such objects is pretty hard. 

  
To help alleviate this difficulty Chen built a tool right into the Codename One simulator that allows you to query the cloud storage for the current application and helps you review some of the complexities involved.  

  
You can query various application scopes, if you don’t enter an object type you will receive all objects but only for the private/application scopes (otherwise you would just get too much information). Notice that when you sort based on an index if the index is missing an entry just won’t appear so keep that in mind.  

  
We have great plans for this API, including features for map-reduce like functionality to allow you to build complex data manipulation logic without having to setup any server infrastructure.  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
