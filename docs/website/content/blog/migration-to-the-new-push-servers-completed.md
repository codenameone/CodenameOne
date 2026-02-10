---
title: Migration To The New Push Servers Completed
slug: migration-to-the-new-push-servers-completed
url: /blog/migration-to-the-new-push-servers-completed/
original_url: https://www.codenameone.com/blog/migration-to-the-new-push-servers-completed.html
aliases:
- /blog/migration-to-the-new-push-servers-completed.html
date: '2015-11-16'
author: Shai Almog
---

![Header Image](/blog/migration-to-the-new-push-servers-completed/push-megaphone.png)

Today we dealt with some push messages overloading our servers, some of the apps developed in Codename One  
are remarkably successful and as a result our push servers got bogged down.  
  
To mitigate that and prevent service interruptions we moved all push activity to the new servers, this effectively  
means that a push operation on the old servers will map to the new servers seamlessly. This also means  
that we no longer support the null push target even for the old push servers. Its just too expensive to support  
on scale of 150M+ devices. 

Unless you use null pushes this should have no effect on your code. It does mean though that we see the new  
push infrastructure as solid enough for production usage. We now recommend that you migrate  
to the new push infrastructure as that will increase the performance of your push by eliminating a stage  
in the push process and will remove a potential failure point. You should do that anyway in the long term so you  
[might as well do it right now](/blog/new-push-servers.html)… 

#### The Next Step

We intend to change the behavior of the `registerPush` call to return the new push key format  
(cn1-*) which will simplify our server code considerably and should work seamlessly with code that doesn’t  
make assumptions about key length.  
E.g. up until now the servers would return a numeric device ID, so if you relied on this key being numeric or  
having a length below 20 decimals this won’t be the case anymore.  
This is generally a good thing for moving forward as it will simplify the migration process to the new push servers  
even further. 

#### Deprecation Of The Old Push Servers

The obvious question is when we will take down the old push server API support. This will not happen before  
the 3.3 time line and probably not before the 3.4 timeline. When 3.3 rolls out (it is scheduled for January) we  
would deploy server code that will let us know who is still using the old API and we will work with individual users  
of the old push API to migrate their code. 

If you have device code that calls the `Push` API’s then we urge you to start migrating right away since  
you would need to update all the client side calls. If this isn’t the case and you only initiate push from the server  
then the migration process should be really simple. I can attest to that personally as it took us under an hour to  
migrate our old push servers to use the new push service internally.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
