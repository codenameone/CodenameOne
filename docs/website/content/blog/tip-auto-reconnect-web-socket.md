---
title: 'TIP: Auto Reconnect Web Socket'
slug: tip-auto-reconnect-web-socket
url: /blog/tip-auto-reconnect-web-socket/
original_url: https://www.codenameone.com/blog/tip-auto-reconnect-web-socket.html
aliases:
- /blog/tip-auto-reconnect-web-socket.html
date: '2018-09-10'
author: Shai Almog
---

![Header Image](/blog/tip-auto-reconnect-web-socket/tip.jpg)

WebSockets changed the way I do networking code. I combine them with WebServices to get the best of both worlds. But they still suffer in terms of reliability. With WebServices we have retries and a mostly transactional model. There is no permanent connection that should be re-established.

With WebSockets a disconnect can be painful, up until recently I used a rather elaborate strategy of error detection and timers. With the latest update to the [WebSocket cn1lib](https://github.com/shannah/cn1-websockets/) we now have a better solution: `autoReconnect(int)`.

It’s exactly as it sounds, once you create a websocket you can invoke `autoReconnect(5000)` on it to retry the connection ever 5 seconds in case of a disconnect.

If you have an existing WebSocket app you should probably add this call.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
