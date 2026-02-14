---
title: Bluetooth Support
slug: bluetooth-support
url: /blog/bluetooth-support/
original_url: https://www.codenameone.com/blog/bluetooth-support.html
aliases:
- /blog/bluetooth-support.html
date: '2016-05-29'
author: Shai Almog
---

![Header Image](/blog/bluetooth-support/bluetooth.jpg)

Bluetooth is one of those specs that makes me take a step back…​ It’s nuanced, complex and multi-layered.  
That isn’t necessarily bad, it solves a remarkably hard problem. Unfortunately when people say the words “bluetooth  
support” it’s rare to find two people who actually mean the same thing!

So while we did have a lot of requests for bluetooth support over the years most of them were too vague and when  
we tried to follow thru we usually reached a dead end where the customers themselves often didn’t really know  
the part of the spec they wanted.

Normally when something is so huge and vague we try to look at what the native OS’s did, but iOS and Android took  
very different routes to bluetooth.

Another approach is to look at what other companies in the field did to support bluetooth and historically there  
wasn’t much. Most “Write Once Run Anywhere” solutions just ignored that feature.

However, recently we became aware of [this bluetooth plugin for Cordova](https://github.com/randdusing/cordova-plugin-bluetoothle)  
which Chen and Steve adapted to a Codename One cn1lib [here](https://github.com/chen-fishbein/bluetoothle-codenameone).

Check it out and let us know what you think…​
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved below for historical context. New discussion happens in the Discussion section._


### **Nick Koirala** — June 2, 2016 at 2:55 am ([permalink](https://www.codenameone.com/blog/bluetooth-support.html#comment-22554))

> Nick Koirala says:
>
> Nice, just built the demo and found it picking things up. Can it scan in the background and call back to the app? I.e., for discovering beacons.
>


---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
