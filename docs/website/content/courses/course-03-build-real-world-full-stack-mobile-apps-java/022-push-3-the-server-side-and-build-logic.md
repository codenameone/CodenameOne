---
title: "Push 3 - The Server Side and Build Logic"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Push and In-App Purchase"
module_key: "08-push-and-in-app-purchase"
module_order: 8
lesson_order: 3
weight: 22
is_course_lesson: true
description: "Send push notifications from the server when the build pipeline finishes and return a usable payload to the client."
---
> Module 8: Push and In-App Purchase

{{< youtube qaYExTzcCVY >}}

On the server side, push is part of the result-delivery pipeline. The build finishes, the server decides what happened, and then it sends a short notification to the device so the app can react.

That framing matters because this lesson is not really about building a generic push utility. It is about closing the loop in a product where the server builds an app on behalf of the user and needs a clean way to report the result back to the device.

The first practical concern is credential management. The server needs Codename One push credentials, Android server-side credentials, and iOS push certificate information. The original lesson stores these values in a local properties file instead of hard-coding them into source. That is still the right approach. Secrets and environment-specific configuration should live outside application code so they can be rotated and deployed safely.

The second concern is the build mode itself. The lesson uses a synchronous automated build flow so the server can wait for the result, inspect the generated artifacts, and decide what message to send. That works well for this kind of product because a build request is not fire-and-forget. The user expects a concrete answer: success, failure, and where the result can be downloaded.

Once the build is complete, the push itself is conceptually simple. The server posts to the Codename One push service with the device key and the credentials needed for the target platforms. In this app the message includes both a visible notification for the user and a machine-readable payload for the client code. That makes the notification useful immediately while still giving the app enough information to continue the flow automatically.

That mixed-message approach is one of the better ideas in this module. Push is used for what it is good at: signaling a state change and drawing attention to something that is ready. The real work, such as downloading or opening the generated result, still happens in normal application logic where it can be validated and retried.

The iOS warning from the video also deserves to survive in updated form. Development and production push environments are different, and they do not share credentials. A debug build installed directly on a device is not the same thing as a production build distributed through the App Store. If push works in one environment and fails in the other, mismatched certificates or environment configuration are a likely cause.

Finally, log the push response on the server. Even in a small project, delivery failures are much easier to diagnose when the server records what it attempted to send, which credentials were used, and what response came back from the push service.

## Further Reading

- [Push 1 - Initial Registration Process](/courses/course-03-build-real-world-full-stack-mobile-apps-java/020-push-1-initial-registration-process/)
- [Push 2 - Client Side Code](/courses/course-03-build-real-world-full-stack-mobile-apps-java/021-push-2-client-side-code/)
- [Push Notifications](/push-notifications/)
- [In-App Purchase](/courses/course-03-build-real-world-full-stack-mobile-apps-java/025-in-app-purchase/)
