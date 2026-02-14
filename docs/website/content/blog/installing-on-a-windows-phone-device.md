---
title: Installing On A Windows Phone Device
slug: installing-on-a-windows-phone-device
url: /blog/installing-on-a-windows-phone-device/
original_url: https://www.codenameone.com/blog/installing-on-a-windows-phone-device.html
aliases:
- /blog/installing-on-a-windows-phone-device.html
date: '2012-10-23'
author: Shai Almog
---

![Header Image](/blog/installing-on-a-windows-phone-device/codename-one-charts-1.png)

We recently added Windows Phone support to Codename One, this allows you to build your applications as a Windows XAP application for installation on a Windows Phone device. Unfortunately of all the platforms we support (including J2ME and iOS) MS is the only company that chose not to allow standard OTA distribution so you will literally need a PC in order to install the application with a cable. 

MS has a sort of beta distribution option which might alleviate the problem but we didn’t get a chance to try it out.

Sending a build for Windows Phone is similar to sending it to any other platform, with the latest distribution just right click and send a build for Windows Phone, its just that simple.  
  
Unlike most other platforms MS didn’t burden us with the silly need to sign the distribution (they can sign it themselves when we upload to the store, makes MUCH more sense!).

Installing said build requires that you enable your device for development for which you need to pay Microsoft. The instructions for doing all of this are all  
[  
here  
](http://msdn.microsoft.com/en-us/library/windowsphone/develop/gg588378%28v=vs.92%29.aspx)  
. 

Notice: This post was automatically converted using a script from an older blogging system. Some elements might not have come out as intended…. If that is the case please let us know via the comments section below.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Anonymous** — January 22, 2015 at 4:42 pm ([permalink](https://www.codenameone.com/blog/installing-on-a-windows-phone-device.html#comment-21607))

> Anonymous says:
>
> When you’re testing your app on the emulator, leave the emulator open between debugging sessions so you can run your app again quickly.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
