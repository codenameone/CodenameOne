---
title: Storage Migration
slug: storage-migration
url: /blog/storage-migration/
original_url: https://www.codenameone.com/blog/storage-migration.html
aliases:
- /blog/storage-migration.html
date: '2015-05-05'
author: Shai Almog
---

![Header Image](/blog/storage-migration/iOS.png)

Our iOS port has some pieces that are pretty old and haven’t been touched since we started, one of those things  
is the IO code which mostly works as we wrote it when we started Codename One. Unfortunately it seems that  
Storage in iOS is mapped to the iOS caches directory, this directory can be wiped by the iOS device if space  
on the device is running low. That’s a very rare occurrence which is why we didn’t pick that up until a  
[bug report was filed on it this week](https://github.com/codenameone/CodenameOne/issues/1480)… 

Unfortunately fixing Storage to point at the right directory would mean breaking compatibility and your app  
losing all the data it kept in storage… So we decided to go about this in a rather creative way.  
We defined a new build argument which will be on by default for all new projects: `ios.newStorageLocation`

This build argument effectively means that we should use the documents directory as storage and the app is ready  
to deal with it. Its useful for apps that aren’t already in users hands. If you don’t define that flag we will automatically  
migrate the app to the documents directory on the first usage of `Storage`, we will detect if our  
storage directory exists under documents and if not we will move all files to that directory. This should maintain  
compatibility with a small performance overhead on the first activation for new installs and possibly the first  
time this code occurs in a pre-existing app.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Maaike Z** — May 6, 2015 at 5:58 pm ([permalink](/blog/storage-migration/#comment-24189))

> Maaike Z says:
>
> Is it a boolean? So ios.newStorageLocation = true when I want to use the new location?
>



### **Shai Almog** — May 7, 2015 at 4:09 am ([permalink](/blog/storage-migration/#comment-22215))

> Shai Almog says:
>
> Yes it should be true/false (notice its not yet on the servers and will be there before the weekend).  
> We already documented it in the manual section: [http://www.codenameone.com/…](<http://www.codenameone.com/manual/advanced-topics.html#_sending_arguments_to_the_build_server>)
>



### **kazza186** — August 2, 2016 at 9:59 am ([permalink](/blog/storage-migration/#comment-22780))

> kazza186 says:
>
> Has there been a change with this? I’ve just found this because I’m having the exact problem where data is being wiped on iPhones with low storage space, but I never defined this hint so shouldn’t it be using Documents directory? My files are being stored in Library/caches when I use the codename one file storage. Is that correct? How do I switch it to use the Documents? Thanks.
>



### **Shai Almog** — August 3, 2016 at 4:54 am ([permalink](/blog/storage-migration/#comment-21451))

> Shai Almog says:
>
> No. The build hint should be defined there by default. Which IDE are you using?  
> When did you create the project and which project type did you select?  
> Check the FileSystemStorage.getRoots values on the device. Print them to a dialog or log. They should be arranged as documents first and caches second. If not then the build hint isn’t turned on for some reason.
>



### **kazza186** — August 4, 2016 at 4:21 am ([permalink](/blog/storage-migration/#comment-22895))

> kazza186 says:
>
> Printed it to a Dialog without the build hint added. Cache was first, then Documents. Have added the build hint now and it’s using Documents instad of Cache which is good.  
> I’m using Eclipse, created the project in January 2016. I can’t remember which project type I used sorry.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
