---
title: Drop It – Introducing Dropbox Integration
slug: drop-it-introducing-dropbox-integration
url: /blog/drop-it-introducing-dropbox-integration/
original_url: https://www.codenameone.com/blog/drop-it-introducing-dropbox-integration.html
aliases:
- /blog/drop-it-introducing-dropbox-integration.html
date: '2013-05-12'
author: Shai Almog
---

![Header Image](/blog/drop-it-introducing-dropbox-integration/drop-it-introducing-dropbox-integration-1.png)

  
  
  
[  
![Dropbox](/blog/drop-it-introducing-dropbox-integration/drop-it-introducing-dropbox-integration-1.png)  
](https://code.google.com/p/dropbox-codenameone-sdk/)  
  
  

We are working on something exciting, more on that next week (hopefully). One of the things we needed was a way to access files on a device, e.g. images, etc. however this is a painful and fragmented subject. 

  
Most of my files are still on my laptop and not on my tablet or phone and moving them back and forth isn’t  
  
convenient… Luckily we have  
[  
Dropbox  
](https://code.google.com/p/dropbox-codenameone-sdk/)  
, this neat tool has really helped us collaborate as a startup and has made many painful things remarkably easy.

  
So Chen came up with an amazing plugin that allows us to treat drop box as yet another file system we can use to extract files from!  
  
  
(He made use of some excellent work contributed to the community by  
[  
Eric Coolman  
](http://gadgets.coolman.ca/)  
for Oauth 1.x support!).  
  
  
  
  
  
This is absolutely spectacular and hugely convenient!  
  
  
You can download his source code  
  
[  
here  
](https://code.google.com/p/dropbox-codenameone-sdk/)  
  
its a Codename One Library project so you just compile it in NetBeans then add it to the lib directory of either Eclipse or NetBeans. Then right click the project  
  
and press refresh libs and it should “just work”.  
  
This is a brand new project and Chen would love some contributions and collaborations there so feel free to contribute.

  
So how do you use it?  

  
First you need to create a Dropbox core application  
[  
here  
](https://www.dropbox.com/developers/apps)  
, this will give you the two keys you need below. Keep in mind that in order to use it for more than one account you will need to select an option during the process and in order to use it in production you will need specific approval.  

  
**  
Notice:  
**  
for this code to work properly you will need a “proper” browser component, so you will need Java 7 to be configured properly with FX so the browser component will work as expected on the simulator.  
  
  
  
  
  
  
First you just need to login:  
  

* * *

  
  
  
The code above logs in to Dropbox then invokes the Dropbox file picker code which I’ll show you below using our tree component.  
  
  
The code below completes the picture by creating a file picker tree allowing  
  
us to download a file from dropbox and do whatever we please with it!  

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — May 16, 2013 at 9:20 pm ([permalink](https://www.codenameone.com/blog/drop-it-introducing-dropbox-integration.html#comment-21751))

> Anonymous says:
>
> This is all great. Also I want to know when are you going to support the new Asha software platform? Or are you going to support it at all?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdrop-it-introducing-dropbox-integration.html)


### **Anonymous** — May 17, 2013 at 6:19 pm ([permalink](https://www.codenameone.com/blog/drop-it-introducing-dropbox-integration.html#comment-21737))

> Anonymous says:
>
> J2ME platform is supported, what feature are you missing?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdrop-it-introducing-dropbox-integration.html)


### **Anonymous** — October 31, 2013 at 11:43 am ([permalink](https://www.codenameone.com/blog/drop-it-introducing-dropbox-integration.html#comment-21771))

> Anonymous says:
>
> Dear Chen, 
>
> I would like to discuss with you in detail features of your plugin and its possible use in the application we are developing. Please contact me ASAP. 
>
> Best regards, 
>
> Ibrokhim
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdrop-it-introducing-dropbox-integration.html)


### **Anonymous** — January 24, 2014 at 11:44 am ([permalink](https://www.codenameone.com/blog/drop-it-introducing-dropbox-integration.html#comment-21642))

> Anonymous says:
>
> Is it possible to upload a file as well?
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdrop-it-introducing-dropbox-integration.html)


### **Anonymous** — January 24, 2014 at 3:40 pm ([permalink](https://www.codenameone.com/blog/drop-it-introducing-dropbox-integration.html#comment-22037))

> Anonymous says:
>
> Not at the moment although it should be doable in theory.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fdrop-it-introducing-dropbox-integration.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
