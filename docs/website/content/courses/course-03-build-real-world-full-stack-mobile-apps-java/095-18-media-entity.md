---
title: "18. Media Entity"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 18
weight: 95
is_course_lesson: true
description: "Define a reusable media model that can hold uploaded files now and still leave room for more scalable storage later."
---
> Module 13: Creating a Facebook Clone

{{< youtube W_1S2Rzgff8 >}}

A social app quickly runs into the question of where media should live. This lesson answers that by introducing a dedicated media entity instead of burying file data inside unrelated objects.

That is the right first step regardless of whether the actual bytes stay in the database forever. Separating media into its own entity means the application can reason about uploads, ownership, visibility, and purpose independently from the records that reference them.

The lesson is also honest about the tradeoff. Storing blobs in a database can be convenient, especially in a simple deployment or clustered environment, but it is not always the final answer. The more durable architectural lesson is that the app should already have a media abstraction, so moving later to object storage or another file service does not require redesigning the whole domain model.

Fields such as filename, timestamp, role, visibility, and owner are doing real work here. They turn the media record from “just bytes” into something the rest of the application can reason about. An avatar image is not the same thing as a post attachment, and visibility rules matter long before the app has every UI feature built.

The DAO stays intentionally simple, which is also a good sign. Media transport should be explicit and predictable rather than full of hidden side effects.

## Further Reading

- [19. Post and Comment Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/096-19-post-and-comment-entities/)
- [23. NotificationService and MediaService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/100-23-notificationservice-and-mediaservice/)
- [39. Settings Form and Fetching the Avatar Image](/courses/course-03-build-real-world-full-stack-mobile-apps-java/076-39-settings-form-and-fetching-the-avatar-image/)
- [40. Edit User - UI Binding and Multipart Image Upload](/courses/course-03-build-real-world-full-stack-mobile-apps-java/077-40-edit-user-ui-binding-and-multipart-image-upload/)
