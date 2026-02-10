---
title: 'TIP: Use Tethering to Simulate Slow Network Connections'
slug: tip-use-tethering-to-simulate-slow-network-connections
url: /blog/tip-use-tethering-to-simulate-slow-network-connections/
original_url: https://www.codenameone.com/blog/tip-use-tethering-to-simulate-slow-network-connections.html
aliases:
- /blog/tip-use-tethering-to-simulate-slow-network-connections.html
date: '2017-01-29'
author: Shai Almog
---

![Header Image](/blog/tip-use-tethering-to-simulate-slow-network-connections/tip.jpg)

I recently had to debug some code on Android Studio and was reminded how awful that IDE really is. IntelliJ is a pretty good IDE but Android Studio is remarkably slow even for trivial projects…​ One of the things that make it slow (besides RAM usage) is the approach of downloading everything it needs dynamically.

This might be unnoticeable on Googles fast networks where this probably runs instantly. But on my fast home network this was painfully slow.

With mobile apps this problem is often just as bad, we debug on the device in the office where the device is connected to wifi or 4G networks. This might be OK for some cases but it doesn’t represent flaky on-the-road conditions that exist even in the western hemisphere in some regions.

We have an option in the simulator to disable networking or slow it down but these don’t reproduce the same result as one would get with a real slow connection. One of the tricks I picked over the years is to tether my laptop to my phone with a portable hotspot, I then limit the phone to 3g or even 2g networking. At this point the internet connection in the simulator will literally use a slow mobile connection and I’m able to debug/profile the performance accurately.

You can reproduce this on the device too but that won’t allow for easy debugging or profiling.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
