---
title: Live Streaming with Codename One and Wowza
slug: live-streaming-wowza
url: /blog/live-streaming-wowza/
original_url: https://www.codenameone.com/blog/live-streaming-wowza.html
aliases:
- /blog/live-streaming-wowza.html
date: '2020-01-06'
author: Shai Almog
---

![Header Image](/blog/live-streaming-wowza/wowza.jpg)

Two months ago I published the CN1Lib “Wowza Live Streaming Events”, as usual you can install that by the Extension Manager.  
The purpose of this CN1Lib is to add live streaming capabilities to iOS and Android Codename One apps, hiding all the complexities and reducing the effort.

__ |  The Wowza cn1lib has been deprecated since the publication of this post   
---|---  
  
However, live events are not trivial. That’s why you should read this README carefully: <https://github.com/jsfan3/CN1Libs-WowzaLiveStreaming/blob/master/README.md>

This CN1Lib lets you to broadcast a live video streaming event from a mobile app. The streaming is identified by a unique id which is automatically assigned. You can then play the live video stream with a given id and an adaptive bitrate;

You can also record live streams to watch later.

All streaming operations (storage, processing, adapting to multi-bitrate, broadcasting to multiple devices, etc.) are automatically backed by the Wowza Cloud Service. This includes accurate logging and monitoring.

More specifically, this CN1Lib integrates and makes use of GoCoder SDK for iOS, GoCoder SDK for Android, and it performs RESTful requests to the Wowza Streaming Cloud service (live event plan).

Please consider this CN1Lib in alpha stage, not ready yet for production environments. I’d appreciate contributions/pull requests to improve this CN1Lib.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
