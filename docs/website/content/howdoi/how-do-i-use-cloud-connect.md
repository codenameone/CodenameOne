---
title: USE CLOUD CONNECT
slug: how-do-i-use-cloud-connect
url: /how-do-i/how-do-i-use-cloud-connect/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-cloud-connect.html
tags:
- basic
- io
description: Instantly preview your work on multiple devices
youtube_id: NVPIXnkoxv0
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-15.jpg
---
{{< youtube "NVPIXnkoxv0" >}}

Cloud Connect was designed to solve a very specific problem: seeing UI changes on real devices immediately while working in the GUI builder. It synchronized the builder's saved UI state to connected devices so that you could feel the screen on hardware instead of relying only on the desktop preview.

That idea still makes sense as a workflow lesson even though the exact builder-centered flow is much less central to modern Codename One development. The value is in shortening the feedback loop between design changes and device reality. The more quickly you can see spacing, text length, transparency, and interaction feel on real hardware, the better your UI decisions tend to be.

The old feature accomplished that by pushing builder XML and resources through the cloud to a preview app on device. The practical takeaway today is broader: fast on-device iteration is valuable, but the current direction of most projects is much less GUI-builder centric than the video assumes. In a modern CSS-and-code workflow, the exact tools may differ, but the goal is the same: do not trust only the desktop preview for visual decisions that need to survive on real devices.

So while the original Cloud Connect workflow is mostly historical now, the reason it existed is still worth remembering. A UI that looks fine in a builder or simulator can feel very different in the hand. Real device preview is where touch target size, keyboard overlap, spacing, and overall feel become obvious.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Themeing](/themeing/)
- [Hello World](/hello-world/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
