---
title: "23. NotificationService and MediaService"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 23
weight: 100
is_course_lesson: true
description: "Finish the first service layer with dedicated notification and media services that can later grow into push and richer attachment workflows."
---
> Module 13: Creating a Facebook Clone

{{< youtube zJ7L5fi60H8 >}}

These two services are simpler than `UserService`, but they matter because they isolate two concerns that would otherwise spread across the rest of the backend: event delivery and file handling.

`NotificationService` is small on purpose. Right now it persists notifications and serves paged results back to the client. That may not look dramatic, but the separation is valuable because notifications are exactly the kind of feature that tends to accumulate extra delivery mechanisms over time. By giving them their own service now, the app already has the right place to add push, WebSocket fanout, or other delivery channels later.

That is why the send-notification method is more important than it first appears. Even if it currently just writes a record, it defines the one place where “something happened and the user should hear about it” enters the backend.

`MediaService` plays a similar role for uploads and access control. It owns the rule that media creation is authenticated, that timestamps come from the server rather than the client, and that visibility checks determine who can retrieve a given file.

The visibility logic is especially important in a social app. Public media is easy. Friend-only media is where the service layer has to translate social relationships into authorization decisions. That should not be left to the client, and it should not be scattered across controller code.

The little permission and visibility helpers in this lesson may feel unremarkable, but they are the sort of infrastructure that prevents the backend from drifting into a pile of ad hoc access checks.

## Further Reading

- [14. Notifications Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/091-14-notifications-container/)
- [18. Media Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/095-18-media-entity/)
- [19. Post and Comment Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/096-19-post-and-comment-entities/)
- [Push 3 - The Server Side and Build Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/022-push-3-the-server-side-and-build-logic/)
