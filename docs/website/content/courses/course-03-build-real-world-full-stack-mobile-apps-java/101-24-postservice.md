---
title: "24. PostService"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 24
weight: 101
is_course_lesson: true
description: "Implement the service-layer logic behind posting, commenting, likes, and the ranked user newsfeed."
---
> Module 13: Creating a Facebook Clone

{{< youtube UxATbrfWveU >}}

`PostService` is where the social app starts acting like a social app instead of a collection of account features. This service owns the operations that make the feed move: creating posts, inserting them into newsfeeds, adding comments, and tracking likes.

The first useful distinction in the lesson is between a user’s own posts and the newsfeed they consume. Those are not the same thing. A profile post list can often be queried directly from posts. A feed is more contextual. It blends content from multiple sources and needs its own ordering rules.

That is why the separate `Newsfeed` entity introduced earlier matters so much here. `PostService` can write newly created posts into the right feed records when the post is created instead of trying to reconstruct the whole ranking story every time the client asks for page one again.

The posting path is also a good example of service-layer responsibility. A new post is not just one database save. It creates the post record, decides who should see it, inserts it into the author’s feed, inserts it into friends’ feeds where appropriate, and returns the information the client needs to keep going.

Comments and likes follow the same pattern. These are not just property changes on one row. They are social events with permission checks, relationship checks, and notification side effects. The lesson keeps them in one service, which is the right place because this is where the rules about who can interact with what actually belong.

The simplifications are also sensible. There is no full reactions model yet, and unlike is omitted even though it would be easy to add later. That restraint keeps the service focused on the core feed interactions instead of widening the scope unnecessarily.

## Further Reading

- [20. Notification, Newsfeed and ShadowUser Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/097-20-notification-newsfeed-and-shadowuser-entities/)
- [23. NotificationService and MediaService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/100-23-notificationservice-and-mediaservice/)
- [29. Newsfeed and Posts From Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/106-29-newsfeed-and-posts-from-server/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
