---
title: USE HTTP, SOCKETS, WEBSERVICES AND WEBSOCKETS
slug: how-do-i-use-http-sockets-webservices-websockets
url: /how-do-i/how-do-i-use-http-sockets-webservices-websockets/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-http-sockets-webservices-websockets.html
tags:
- basic
- io
description: Networking options explained
youtube_id: -M957AAi-vk
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-17.jpg
---
{{< youtube "-M957AAi-vk" >}}

Networking on mobile is not the same as networking on the desktop or server. Connections disappear, latency changes abruptly, background execution is limited, and platform security rules can reject traffic that would look perfectly normal elsewhere. If you start with that assumption, the rest of the networking choices in Codename One make much more sense.

The default tool for most network work in Codename One is `ConnectionRequest` together with `NetworkManager`. That combination exists because mobile code benefits from a higher-level, portability-aware request pipeline. It handles threading better, integrates cleanly with the UI lifecycle, and is the right place to start for normal HTTP and REST communication.

HTTPS should also be the default expectation. The video is correct that plain HTTP is increasingly restricted, especially in the Apple ecosystem. Treat unsecured HTTP as the exception that must be justified, not as the baseline.

When you send a request with `ConnectionRequest`, the next question is how you want to process the response. Reading the response directly on the network thread is often the most efficient option when you want full control and do not need to touch the UI immediately. Response listeners are simpler when UI updates are the next step, because they run in a friendlier context for UI work. Blocking requests can still be useful in limited cases, but they should be used carefully because the convenience comes with responsiveness tradeoffs.

The older `URL`-style APIs exist mostly for portability of existing Java code, but they are not the best default for new Codename One networking. Once you drop to lower-level APIs, you inherit more threading and platform-behavior differences yourself. For new code, `ConnectionRequest` or the higher-level REST utilities are usually the better choice.

Sockets are a different category entirely. They are lower level, harder to support across mobile environments, and more sensitive to NAT, connectivity shifts, and platform behavior. They can be the right answer for specialized protocols, but they are not the place most mobile applications should begin.

WebSockets sit in the middle. They are a good fit when the server needs to push events to the client continuously, as in chat, live dashboards, or presence-style features. They are usually much more appropriate than crude polling loops when you need ongoing server-to-client communication. Even then, they should be chosen because the problem really needs that shape of communication, not because they sound more modern than HTTP.

The modern decision tree is fairly simple. If you are calling a normal backend API, use HTTP with `ConnectionRequest` or the higher-level REST helpers. If you need real-time bidirectional messaging, consider WebSockets. If you need a special low-level protocol and understand the operational costs, use sockets deliberately. Do not pick the lowest-level tool first and then try to rebuild mobile-friendly behavior on top of it.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Terse REST API](/blog/terse-rest-api/)
- [How Do I Access Remote Webservices? Perform Operations On The Server?](/how-do-i/how-do-i-access-remote-webservices-perform-operations-on-the-server/)
- [Performance Network Monitors](/performance-network-monitors/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
