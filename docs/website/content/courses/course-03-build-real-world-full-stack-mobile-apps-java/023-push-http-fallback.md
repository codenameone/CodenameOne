---
title: "Push Http Fallback"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Push and In-App Purchase"
module_key: "08-push-and-in-app-purchase"
module_order: 8
lesson_order: 4
weight: 23
is_course_lesson: true
description: "Keep the app working when push is unavailable by falling back to a simple HTTP polling path."
---
> Module 8: Push and In-App Purchase

{{< youtube VDyltmk_Hu8 >}}

Push is useful, but it should never be the only transport a feature depends on. Users can disable it, devices can fail to register, and platform delivery is not reliable enough to be the single mechanism that keeps an app functional.

That is why this lesson adds an HTTP fallback. It is not elegant, but it is dependable and easy to understand. The server stores the latest build result somewhere the client can fetch it later, and the client polls only when push is unavailable.

In this app the existing restaurant entity is used as a convenient place to store the most recent build result. That is a pragmatic choice. The app already has a server-side record for the current builder state, so attaching the latest build status to that record avoids introducing extra infrastructure just to support a fallback notification path.

On the client side, the logic is simple and that simplicity is a strength. If push registration succeeded and the app has a usable push key, push remains the preferred notification mechanism. If not, the client periodically sends a normal HTTP request until the server reports a changed result.

That is the right priority order. Push gives a better user experience, but HTTP polling is easier to reason about and easier to debug. The important thing is that both paths eventually call into the same result-handling code. The transport changes, but the meaning of the result does not.

This is also a good example of a broader rule in mobile development: if an event-driven channel is optional, always keep a plain request-response path available. It may be slower and less efficient, but it prevents an entire feature from failing just because one delivery mechanism is missing.

The video correctly presents polling as a fallback rather than the ideal design. Polling creates unnecessary traffic and introduces delay because the client only learns about changes on the next timer tick. It is acceptable as a safety net, but not as the best long-term solution. The next lesson shows how WebSockets provide a cleaner event-driven fallback when you want real-time updates without relying on push.

## Further Reading

- [Push 2 - Client Side Code](/courses/course-03-build-real-world-full-stack-mobile-apps-java/021-push-2-client-side-code/)
- [Push WebSockets Fallback](/courses/course-03-build-real-world-full-stack-mobile-apps-java/024-push-websockets-fallback/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
- [How Do I Use HTTP, Sockets, Webservices & WebSockets](/how-do-i/how-do-i-use-http-sockets-webservices-websockets/)
