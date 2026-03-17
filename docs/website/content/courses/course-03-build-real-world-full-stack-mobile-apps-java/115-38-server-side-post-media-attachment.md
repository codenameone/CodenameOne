---
title: "38. Server Side Post Media Attachment"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 38
weight: 115
is_course_lesson: true
description: "Extend posts so they can reference uploaded media and make the server-side attachment flow usable from the client."
---
> Module 13: Creating a Facebook Clone

{{< youtube Xrolmjg8Dr0 >}}

This lesson takes the existing media and post infrastructure and connects them. That sounds small, but it is the step that turns uploads into actual social content.

The most important design choice is that posts do not embed raw media blobs directly. They reference media objects. That keeps the post model lighter, preserves reuse of the media service and entity model, and leaves room for richer attachment behavior later.

Representing attachments to the client as a minimal map of media IDs and MIME types is also a sensible boundary. The client needs enough information to decide how to render or request the media, but it does not need the full internal server-side media object every time it sees a post.

The auth-transport compromise called out in the lesson is worth keeping as an architectural warning. Passing authorization as a query parameter can simplify certain client-side URL-based image-loading paths, but it also raises security risks if those URLs are ever logged, shared, or reused carelessly. The tutorial is right to flag that tradeoff instead of pretending it is harmless.

The more durable takeaway is that media delivery often forces you to choose between transport convenience and stricter security boundaries. Those tradeoffs should always be explicit.

## Further Reading

- [18. Media Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/095-18-media-entity/)
- [23. NotificationService and MediaService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/100-23-notificationservice-and-mediaservice/)
- [40. Post Media Attachments - Client Side Business Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/117-40-post-media-attachments-client-side-business-logic/)
- [42. Images, Videos and Styled Posts in the Newsfeed](/courses/course-03-build-real-world-full-stack-mobile-apps-java/119-42-images-videos-and-styled-posts-in-the-newsfeed/)
