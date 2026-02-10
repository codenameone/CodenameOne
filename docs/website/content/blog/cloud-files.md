---
title: Cloud Files
slug: cloud-files
url: /blog/cloud-files/
original_url: https://www.codenameone.com/blog/cloud-files.html
aliases:
- /blog/cloud-files.html
date: '2014-03-09'
author: Shai Almog
---

![Header Image](/blog/cloud-files/cloud-files-1.png)

  
  
  
[  
![Map](/blog/cloud-files/cloud-files-1.png)  
](/img/blog/old_posts/cloud-files-large-2.png)  
  
  

Cloud files are a great pro feature that we didn’t emphasize enough, its remarkably useful. It allows you to upload a file into the cloud which you can then transfer to anyone thru a simple “obfuscated” URL. The URL is long so the probability of someone guessing it is low, hence its pretty secure for private file transfer (if its really private you should use  
[  
bouncy castle  
](http://www.codenameone.com/3/post/2013/06/bouncy-castle-crypto-api.html)  
). The API couldn’t be simpler:  
  
  
  
  
  
  
String fileId = CloudStorage.getInstance()  
  
  
.uploadCloudFile(mimeType, fileName);  
  
  
  
  
  
This will block to upload the file so you might want to display an infinite progress indicator or something, once its done and the file was uploaded you can just call:  
  
  
  
  
String url = CloudStorage.getInstance().  
  
  
getUrlForCloudFileId  
  
(  
  
fileId);  
  
  
  
  
That URL will provide you with the file download and you can delete the file via deleteCloudFile(fileId). In the upcoming version we are also adding deleteAllCloudFilesForUser() and deleteAllCloudFilesBefore(timestamp, developerAccount, developerPassword)  
  
  
which will allow you to purge some of your quota.  
  
  
  
  
You can use this for image exchange and other such tricks but one of my favorite concepts is sharing data between devices e.g. you can upload your application state as a file to the cloud and expose it via a QR code like this:  
  
  
int size = Math.min(Display.getInstance().getDisplayWidth(), Display.getInstance().getDisplayHeight());  
  
  
  
  
  
  
Label qrCode = new Label(new URLImage(“http://chart.apis.google.com/chart?cht=qr&chs=” + size + “x” + size “&chl=” + fileId + “&chld=H|0”));  
  
  
  
  
  
  
Now on the other device just scan the QR code and download the file to import the data, trivial synchronization between devices without typing a single word into the device!  
  
  
  
  
The image you see on the right is a bit unrelated but you might find it interesting…  
  
A corporate account requested  
  
native Google Maps support for iOS/Android, we already have a prototype on iOS although Android is a bit more of a challenge. We hope to have something to show soon though.  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — March 10, 2014 at 7:01 pm ([permalink](https://www.codenameone.com/blog/cloud-files.html#comment-22012))

> Anonymous says:
>
> Hey Shai, 
>
> what about Windows Phone? will it have native maps support too?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcloud-files.html)


### **Anonymous** — March 11, 2014 at 5:02 am ([permalink](https://www.codenameone.com/blog/cloud-files.html#comment-21658))

> Anonymous says:
>
> That isn’t planned at the moment. On Windows Phone we will currently fallback to MapComponent. Google Maps doesn’t have a version for Windows Phone so supporting something like here maps etc. might be a pain.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fcloud-files.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
