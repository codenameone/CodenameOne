---
title: Hands-on With The Blackberry Tools On The Z10
slug: hands-on-with-the-blackberry-tools-on-the-z10
url: /blog/hands-on-with-the-blackberry-tools-on-the-z10/
original_url: https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html
aliases:
- /blog/hands-on-with-the-blackberry-tools-on-the-z10.html
date: '2013-06-23'
author: Shai Almog
---

![Header Image](/blog/hands-on-with-the-blackberry-tools-on-the-z10/hands-on-with-the-blackberry-tools-on-the-z10-1.jpg)

  
  
  
[  
![Picture](/blog/hands-on-with-the-blackberry-tools-on-the-z10/hands-on-with-the-blackberry-tools-on-the-z10-1.jpg)  
](/img/blog/old_posts/hands-on-with-the-blackberry-tools-on-the-z10-large-2.jpg)  
  
  

Note: skip to the bottom for some instructions on working with the BB Z10.  
  
  
  
  
  
  
Lately we’ve been asked by one of our pro customers to fix some issues which occurred only on the z10 device. I thought this would be an easy task: just grab the tools from the blackberry site and then use them to debug the issue. I was quite wrong… Apparently the emulator can only run in a virtualization environment.  
  
  
  
  
OK that shouldn’t stop me, I grabbed the VMPlayer installed it then installed the BB emulator and had myself a running emulator… Or at least I should have been so lucky! My machine couldn’t run it because the display driver wasn’t supported, took me a while to figure this one out. I tried on another PC instead and was luckier, it worked I had a emulator running. 

  
Blackberry 10 is in many regards more like Android than older blackberries. So it doesn’t support our existing BB apps, however it does support running Android apps if they are packaged specifically for it.  

  
So I started working on  
  
installing the android build of the app, should be an easy task, right?  
  
  
  
Wrong again. Spent a couple of hours understanding the process of converting the .apk to a .bar file and then the signing process. Finally I, got a valid .bar file and installing it wasn’t hard. The app was finally running in the emulator and I got a tingling happy feeling all over.  
  
  
  
  
Then the app crashed which was pretty much what our customer complained about, so lets take a look at the log or console. Should be somewhere, right?  
  
  
  
  
Wrong, you need an ssh client to connect to the VM and then look for the file somewhere… That’s inconvenient but doable, I really wanted to know what the hell happened.  
  
  
  
  
Launching Putty and connecting to get the log then finding the path, trying to get in and it seems that I don’t have permissions to enter the directory for the log. Well that broke me, how do they expect anyone to develop apps for this?  
  
  
  
  
Some more googling and I came to the conclusion that the only way to debug this is with a device.

  
  
Well a few days past and I got myself a limited edition z10 device (thanks to  
[  
Shai Ifrach  
](https://twitter.com/future_soft/)  
), seems like it became a hit device because it’s a limited edition. It even sold for $1799 –   
[  
http://www.ebay.com/itm/Developer-BlackBerry-Z10-Limited-Edition-Red-/321094687957  
](http://www.ebay.com/itm/Developer-BlackBerry-Z10-Limited-Edition-Red-/321094687957)  
  
Checking the problem on the device should be easier, it seemed that the OS version on the device was out of date and since they push the OS updates through the carrier this posed a problem. 

  
Or am I? After  
  
some more googling I found a manual hacking process to manually update the OS. Got the approval from the owner (I didn’t think I could hack a $1799 loaner device without that) and I was able to update the z10 OS, now lets see what’s going on.  
  
  
  
  
After some more research it seems like the easiest way would be to use the eclipse plugin with the device, grabbed the plugin some more setups and got it working and I’m getting the output in the ddms, now finally I managed to debug the issue and to figure out what was the problem. 

  
In  
  
conclusion – Blackberry… Please… show some love for developers, if it’s not practical to develop properly with the emulator don’t ship it and I thought your JDE was awful.  
  
  
  
The device itself is surprisingly decent, it feels like they have a slim chance to maintain some market with this hardware.

For those of you who want to build apps for Blackberry OS 10, you need to start with your Android APK (send an Android build in Codename One) then converting the apk to a bar file which can be done in a few ways:  

  1. Online tool – Blackberry provides an applet on their site where you provide the apk and the signing certs and it allows you to convert and sign your apk file. 
  2. Command line tools – a few script files that will allow you to preform the conversion in command line 
  3. Eclipse plugin – through the plugin you can do the conversion.  

  
  
See also –   
[  
http://developer.blackberry.com/android/tools/  
](http://developer.blackberry.com/android/tools/)  
  
We would like to simplify this process for our developers but we are running into some issues with the Blackberry command line tools and their signing process. It seems signatures aren’t stored in any standard way.   
  
  

* * *

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — June 25, 2013 at 4:57 pm ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-21879))

> Anonymous says:
>
> i hope that RIM can help you more that some time in the future codename one will support B10 devices by default.
>



### **Anonymous** — July 2, 2013 at 7:32 am ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-21870))

> Anonymous says:
>
> I wonder what they were thinking, making development so difficult for their OS, knowing fully well it’s not the best OS or even Hardware in the market.
>



### **Anonymous** — July 4, 2013 at 5:19 pm ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-21756))

> Anonymous says:
>
> Their dev tools were never a pleasant experience, the JDE was also painful but workable. 
>
> The current simulator is a tool for hackers only, but can you blame them? they don’t have much time and the pressure on them is huge, I hope they will improve over time.
>



### **Anonymous** — January 31, 2014 at 3:47 am ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-21672))

> Anonymous says:
>
> where is the online converter can you give me a link?
>



### **Anonymous** — January 31, 2014 at 4:39 am ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-21954))

> Anonymous says:
>
> They change the URL’s all the time in the RIM site. Did you try googling?
>



### **Anonymous** — January 31, 2014 at 5:40 am ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-22002))

> Anonymous says:
>
> yeh i think they removed it…
>



### **Anonymous** — May 1, 2014 at 4:24 am ([permalink](https://www.codenameone.com/blog/hands-on-with-the-blackberry-tools-on-the-z10.html#comment-22086))

> Anonymous says:
>
> Energy, thank you that we find a simple and powerfull solution to build for BB10 and thanks that rim makes it pleasant for developers to build for their OS, appreciate it. amen.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
