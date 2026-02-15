---
title: Webservice Wizard
slug: webservice-wizard
url: /blog/webservice-wizard/
original_url: https://www.codenameone.com/blog/webservice-wizard.html
aliases:
- /blog/webservice-wizard.html
date: '2014-06-03'
author: Shai Almog
---

![Header Image](/blog/webservice-wizard/hqdefault.jpg)

  

Simplified webservice access has long been on our todo list and we now finally have a tool that significantly simplifies server communications in Codename One to a method call level. We just posted a  
[  
How Do I? video  
](/how-do-i---access-remote-webservices-perform-operations-on-the-server.html)  
for this feature and will launch it with the plugin update next week. The feature is remarkably simple in concept, you define a set of methods and get a client class coupled with some server classes so you can invoke server functionality directly from the mobile.  
  
  
  
  
In order to support SOAP/REST and other standard protocols just use the far superior Java EE webservice capabilities and use the webservice merely as a proxy for that purpose. Since the calls are binary their overhead is remarkably low both on the wire and don’t require any real parsing resulting in much faster communications when compared to SOAP, REST & even to GWT’s RPC.  
  
  
  
  
If you are familiar with GWT’s RPC you should feel pretty much at home, although we did simplify some concepts. One of the cool features we offer in contrast to GWT is the ability to invoke methods synchronously thanks to the fact that we do have access to threads. This makes RPC logic far more intuitive and transactional.  
  
  
  
  
Thanks to all of you who sent valuable feedback in regards to this feature!  
  
  
  
  
  
  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 8, 2014 at 5:57 pm ([permalink](/blog/webservice-wizard/#comment-21984))

> Anonymous says:
>
> Most of my apps use a REST interface to a PHP/MySQL backend. 
>
> I use GZConnectionRequest and read JSON back from the server – sometimes this JSON is quite heavy like 300-400kb other times its 1kb or even less. 
>
> Is there any advantage to adding this system in between that communication and running a java server for this? Or is it just really additional complexity in this case?
>



### **Anonymous** — June 9, 2014 at 2:17 am ([permalink](/blog/webservice-wizard/#comment-21969))

> Anonymous says:
>
> Its hard to tell. REST is relatively efficient but the overhead of gzip/parsing might be significant. OTOH a proxy might also pose an overhead. 
>
> I suggest testing this with just one API and benchmarking. 
>
> Our goal here wasn’t just to make things faster, it was mostly to make things simpler. Since you already implemented the REST calls tht goal should be less of an issue.
>



### **Anonymous** — June 9, 2014 at 6:32 pm ([permalink](/blog/webservice-wizard/#comment-21592))

> Anonymous says:
>
> Simpler is good though. I don’t think I’ll refactor a working project at this stage, but will try this out on new projects.
>



### **Anonymous** — July 1, 2014 at 6:00 am ([permalink](/blog/webservice-wizard/#comment-21446))

> Anonymous says:
>
> Just tried this out. Very slick. Well done !
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
