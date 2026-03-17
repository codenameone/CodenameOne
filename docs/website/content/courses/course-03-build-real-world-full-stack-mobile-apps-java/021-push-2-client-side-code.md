---
title: "Push 2 - Client Side Code"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Push and In-App Purchase"
module_key: "08-push-and-in-app-purchase"
module_order: 8
lesson_order: 2
weight: 21
is_course_lesson: true
description: "Register the app for push, store the push key, and route incoming notifications into normal application logic."
---
> Module 8: Push and In-App Purchase

{{< youtube BwWhFcHc6LM >}}

Once the platform setup is done, the client side of push becomes a lifecycle problem. The app needs to register when it starts, store the push key when registration succeeds, and react sensibly when a message arrives.

The first thing to get right is where the callback lives. In Codename One, push callbacks belong in the main application class, the one that owns `init()` and `start()`. That is not just convention. Push is tied to the application lifecycle, so the framework expects the callback implementation to live in the object that represents that lifecycle.

Registration should happen every time the app runs. The video mentions that `registerPush()` had changed and no longer needed the old extra arguments, and that is still the modern direction. The registration call itself is simple. The useful information arrives later through the callback flow.

When registration succeeds, the app can obtain the actual push key and send it to the server. That is the value the server needs if it wants to target this device later. One of the easiest mistakes here is confusing the native device identifier with the push token. They are not interchangeable. If the server is going to send a push notification, it needs the push key.

Receiving a push is a little subtler than many developers expect. Push is not a guaranteed background RPC channel. If the app is running, the callback can handle the message directly. If the app is not running, behavior depends on the platform and on the push type. That is why this lesson routes the incoming notification into a shared `onBuildResult()` path instead of putting all the business logic inside the push callback itself.

That is still the right design. Push should act as a signal that new work is available. The actual business logic, such as downloading a built artifact or refreshing state, should live in ordinary application code that can also be triggered by fallback transports.

The different push types matter mostly because they influence how much information reaches the app and when. A visible push is useful when the user needs to see a notification. An invisible push can sometimes be used as a signal, but it is not a good foundation for general networking logic. The mixed approach shown in the lesson is the pragmatic one: send a human-readable notification and include a machine-readable payload the app can act on.

In the original code that payload is a URL. That is perfectly reasonable. It could just as well be JSON or another structured message format. What matters is that the app can distinguish between text that is just meant for the user and data that should trigger behavior.

The lesson also touches on registration failures, and that part is worth updating. A failed registration should usually be logged and reflected in app state, not shown as an alarming early-launch toast. Some environments will never produce a perfectly clean failure signal, which is exactly why the rest of the module introduces HTTP and WebSocket fallbacks. A production app should keep working even when push is unavailable.

## Further Reading

- [Push 1 - Initial Registration Process](/courses/course-03-build-real-world-full-stack-mobile-apps-java/020-push-1-initial-registration-process/)
- [Push 3 - The Server Side and Build Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/022-push-3-the-server-side-and-build-logic/)
- [Push WebSockets Fallback](/courses/course-03-build-real-world-full-stack-mobile-apps-java/024-push-websockets-fallback/)
- [Push Notifications](/push-notifications/)
