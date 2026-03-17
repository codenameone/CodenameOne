---
title: "Push WebSockets Fallback"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Push and In-App Purchase"
module_key: "08-push-and-in-app-purchase"
module_order: 8
lesson_order: 5
weight: 24
is_course_lesson: true
description: "Use WebSockets as a more responsive fallback channel when push is unavailable."
---
> Module 8: Push and In-App Purchase

{{< youtube FSek5IOQ0IE >}}

If HTTP polling is the safety net, WebSockets are the more polished fallback. They preserve the event-driven feel of push without depending on the mobile push infrastructure, which makes them a good fit for workflows like build completion, job status changes, and other lightweight live updates.

The reason WebSockets work well here is simple. After the connection is established, the server can push short messages back to the client immediately. That avoids the repeated request loop of polling and gives the app a much more direct way to learn that something changed.

The comparison in the video between HTTP, raw sockets, and WebSockets is still useful. HTTP is excellent for normal request-response operations and large payloads, but it is wasteful when the client keeps asking whether anything has changed. Raw sockets are powerful but awkward to operate through common web infrastructure. WebSockets sit in the middle: they begin as ordinary web traffic, then remain open so client and server can exchange small messages efficiently.

On the client side, the structure is straightforward. Open the WebSocket connection, wait until the connection is actually open, send whatever identifying information the server needs, and then react to incoming messages. In this app that identifying information is the restaurant secret, which allows the server to bind the socket to the correct builder session.

That sequencing matters. One easy mistake with asynchronous communication is assuming the socket is ready immediately after construction. It is not. The handshake has to complete first, which is why the first outgoing message belongs in the `onOpen` callback instead of immediately after creating the object.

Incoming messages can then be routed into the same result-processing code used by push and HTTP fallback. That reuse is the strongest part of the design. WebSockets change how the notification arrives, but they do not change what the notification means.

The server side is where WebSockets become architectural rather than just mechanical. The app needs a handler for the WebSocket endpoint, a way to associate each open connection with the right logical user or restaurant, and a way for the rest of the server code to find that connection when an asynchronous event finishes. The lesson uses a shared map keyed by the restaurant secret to do exactly that.

The implementation shown in the video is intentionally pragmatic and not especially scalable. That is fine for understanding the pattern. The idea to keep is not the exact static-field implementation. The important part is the binding step: when the client opens the socket, the server records which logical entity that connection belongs to so later server-side work can publish a message to the right place.

The video also shows WebSocket support being installed through the old settings UI as a CN1Lib. That workflow is outdated now. In a modern Maven-based Codename One project, dependency management should live in Maven rather than in the legacy extension flow.

## Further Reading

- [Push Http Fallback](/courses/course-03-build-real-world-full-stack-mobile-apps-java/023-push-http-fallback/)
- [How Do I Use HTTP, Sockets, Webservices & WebSockets](/how-do-i/how-do-i-use-http-sockets-webservices-websockets/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
- [Push Notifications](/push-notifications/)
