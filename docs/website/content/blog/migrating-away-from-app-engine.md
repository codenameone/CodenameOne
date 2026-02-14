---
title: Migrating Away From Google App Engine
slug: migrating-away-from-app-engine
url: /blog/migrating-away-from-app-engine/
original_url: https://www.codenameone.com/blog/migrating-away-from-app-engine.html
aliases:
- /blog/migrating-away-from-app-engine.html
date: '2015-05-24'
author: Shai Almog
---

![Header Image](/blog/migrating-away-from-app-engine/gaeburn.png)

Google has just announced that it is deprecating cloud storage and effectively a major part of App Engine that  
we are relying on. To make matters worse the window of time to its removal is quite short so we don’t have enough  
time to rewrite and adapt all the various API’s and tools that rely on this API.  
We have already started the process of migrating off App Engine completely both due to rising costs and Googles  
horrible service/support. This will also allow us to finally support many long standing user requests such as more powerful  
push API’s etc. since we will no longer be held back by App Engines limitations. 

In fact our choice to leave App Engine completely was sealed last month as our App Engine expenses skyrocketed…  
The App Engine console simply stated a cryptic “datastore reads” number that was very high/expensive. We normally cache  
everything in memcache but still it seems that the number was really high. Unfortunately, this was the only number we had!  
Google doesn’t provide any way of knowing which of our queries was responsible for the large number and to this day we have  
no idea what is the actual trigger for this. When we opened a service call they decided that this was a “justified” charge without  
providing us with any itemized listing detailing what we are charged for. 

Due to all of that we decided to start a migration process even before the last announcement, this means a lot  
will change in the backend for Codename One but most of these changes will be seamless and will be made in  
pieces that won’t be noticeable. However we have 2 big features that would be very hard to migrate off App Engine:  
`CloudStorage` and `CloudFile`.   
We already worked with the big users of these API’s to help them migrate away as we started planning.  
However, we might have missed some smaller users. If you rely on one of these API’s and  
haven’t been in contact with us please let us know ASAP. 

Notice that as we migrate some services might require that you build a new version of the app, e.g. sending  
a log from the device (part of crash protection feature) would migrate seamlessly (and probably improve)  
however, you would need to upgrade your users to a new version of the app in order to keep using the feature…  
We will also need you to update URL’s for services such as push notification etc. as new URL’s become available.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **James David Low** — May 26, 2015 at 7:39 am ([permalink](https://www.codenameone.com/blog/migrating-away-from-app-engine.html#comment-22160))

> James David Low says:
>
> Isn’t is just the Files API for Google Cloud Storage and the Blobstore that is deprecated? I know this may still effect things, but just clarifying before people get the idea that Google Cloud Storage is going away. A solution I was thinking of doing was calling blobstoreService.createUploadUrl server side and uploading files server side, abstracting away that side of thing from the client.
>
> “The Files API feature used here to write files to Blobstore has been deprecated and is going to be removed at some time in the future, in favor of writing files to Google Cloud Storage and using Blobstore to serve them.” – [https://cloud.google.com/ap…](<https://cloud.google.com/appengine/docs/java/blobstore/#Java_Writing_files_to_the_Blobstore>)
>



### **Shai Almog** — May 26, 2015 at 2:59 pm ([permalink](https://www.codenameone.com/blog/migrating-away-from-app-engine.html#comment-24198))

> Shai Almog says:
>
> Its not 100% clear. In my last “civil” talk with guys from Google they stressed that we should migrate away from Blobstore (naturally pushing for cloud storage which is even worse).
>
> We had a few regressions with it in the past which demonstrate that Google doesn’t really do much QA for that API. The basic blobstore API is pretty opaque and we had some issues that we had no way of debugging. Because there is quite literally, no one to talk to at Google the safe thing to do is migrate away ASAP. We can’t take any risks with this since a failure in this API will cause builds to fail.
>



### **Youssef** — May 31, 2015 at 6:51 pm ([permalink](https://www.codenameone.com/blog/migrating-away-from-app-engine.html#comment-21939))

> Youssef says:
>
> I was about to use APP ENGINE for my new project. Now that i read this, i’m looking for some good alternatives.  
> Do you have any suggestions ?
>



### **Shai Almog** — June 1, 2015 at 9:30 am ([permalink](https://www.codenameone.com/blog/migrating-away-from-app-engine.html#comment-22093))

> Shai Almog says:
>
> For storage we will probably migrate to Amazon S3, it seems pretty simple and ubiquitous. For everything else we will probably use self hosted servers on digital ocean. We played a bit with Jelastic which seems nice but I’m not sure if we are the target demographic for that since our deployment is rather complex.
>
> We are moving to a microservices architecture which should make these decisions much simpler and easier to fix in the future.
>



### **chachan** — June 30, 2015 at 8:29 pm ([permalink](https://www.codenameone.com/blog/migrating-away-from-app-engine.html#comment-22344))

> chachan says:
>
> We started a project with App Engine but the more we want to add features, the more we find obstacles. In fact, we’re going to code a few more features and we’ll start the migration soon to Digital Ocean. Sad but needed
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
