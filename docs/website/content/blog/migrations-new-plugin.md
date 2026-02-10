---
title: Migrations & New Plugin
slug: migrations-new-plugin
url: /blog/migrations-new-plugin/
original_url: https://www.codenameone.com/blog/migrations-new-plugin.html
aliases:
- /blog/migrations-new-plugin.html
date: '2015-06-03'
author: Shai Almog
---

![Header Image](/blog/migrations-new-plugin/s3-logo.png)

We are in the process of migrating the storage implementation from App Engine to Amazons S3 storage as part  
of our bigger [migration away from App Engine](/blog/migrating-away-from-app-engine.html). If  
you experience issues related to build results please let us know so we can iron out potential regressions.  
We are deploying this change in a way that makes it very easy to toggle this on/off and in case S3 builds prove to  
be an issue we will be able to revert them quickly. 

One of the big benefits in this migration is that S3 has very good uptime, of the few cases where we had App Engine  
downtime issues the toughest were the ones that related to the blobstore API which we are now leaving. Next on  
our agenda would be the migration of most cloud functionality such as push, logs etc. 

One nice feature in S3 is the ability to define object expiration which allows us to keep costs down and prevent  
the bucket from filling up too much. We defined it to 3 days which means that after 3 days an install/preview will  
no longer work, in the past this expired when you sent additional builds which is something we might also apply  
in the future based on storage usage. 

#### NetBeans Plugin Update

It seems the NetBeans verification process which is normally pretty quick has taken a dive. We are waiting for a plugin  
to be approved since the beginning of the week and its still not online with the new fixes. Hopefully it will be out  
sooner rather than later.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
