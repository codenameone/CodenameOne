---
title: "19. Post and Comment Entities"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 19
weight: 96
is_course_lesson: true
description: "Define the server-side post and comment entities so the feed can move from mock data toward persistent social content."
---
> Module 13: Creating a Facebook Clone

{{< youtube tjgyigzxo5U >}}

Once users and media exist on the server, posts and comments are the next unavoidable step. These are the entities that turn the app from a profile system into a real social product.

The lesson follows a sound pattern by keeping the server-side models close to the client-side ones while still allowing the server versions to carry extra persistence and relationship detail. That overlap is a feature, not a flaw. Both sides are describing the same social concepts.

The `Post` entity brings together authorship, timing, content, visibility, styling, comments, and likes. That is a lot of responsibility, but it is the right bundle because those features are what the feed needs to display and what the backend needs to query.

The choice to store individual likes rather than only a count is especially important. Counts are convenient for rendering, but a social app usually needs to know who liked something, not just how many people did. Once that requirement exists, the model has to reflect it.

The `Comment` entity then rounds out the conversation layer. Supporting a parent comment reference early is a good move because it leaves room for nested threads later even if the first client UI stays simpler.

The repository discussion is also useful because this is where paging starts to matter. Social content is exactly the kind of data that should not be loaded all at once. Building pageable queries into the persistence layer early keeps later service code cleaner and keeps the app aligned with the feed-style UX it already has on the client.

## Further Reading

- [20. Notification, Newsfeed and ShadowUser Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/097-20-notification-newsfeed-and-shadowuser-entities/)
- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [10. Client Data Model - User, Post and Comment](/courses/course-03-build-real-world-full-stack-mobile-apps-java/087-10-client-data-model-user-post-and-comment/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
