---
title: "11. ServerAPI Abstraction Mockup"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 11
weight: 88
is_course_lesson: true
description: "Create a temporary server API layer so the feed can be built against stable application-level calls before the real backend exists."
---
> Module 13: Creating a Facebook Clone

{{< youtube PERdJq3I-As >}}

Once the client data model exists, the next architectural step is to stop letting the UI imagine that data comes from nowhere. Even if the real backend is not ready yet, the app should already talk to an abstraction that looks like a server boundary.

That is what this lesson gets right. The mock `ServerAPI` is not just fake data for convenience. It is a way to force the UI to depend on application-level operations instead of hard-coded inline sample objects.

That decision pays off later because the UI can be written once against methods such as “fetch timeline posts” or “get current user,” and the implementation behind those methods can later switch from mock data to real network calls without rewriting every screen.

The mock data itself is also more intentional than a typical placeholder. It includes realistic users, timestamps, avatars, and simple relationships such as friends and suggestions. That makes the upcoming feed and friends UI feel like they are attached to a real product even before the server work is done.

The utility methods added alongside this abstraction are part of the same story. Formatting “just now” style timestamps and creating reusable separators may feel small, but they keep repeated UI logic out of the container classes where it would otherwise start to pile up.

This is exactly the right stage to introduce a server boundary. The app is complex enough to need one, but still small enough that putting it in place now will simplify everything that follows.

## Further Reading

- [10. Client Data Model - User, Post and Comment](/courses/course-03-build-real-world-full-stack-mobile-apps-java/087-10-client-data-model-user-post-and-comment/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [17. Spring Boot Server Architecture and the User Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/094-17-spring-boot-server-architecture-and-the-user-entity/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
