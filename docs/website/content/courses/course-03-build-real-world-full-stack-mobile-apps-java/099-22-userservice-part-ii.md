---
title: "22. UserService Part II"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 22
weight: 99
is_course_lesson: true
description: "Extend UserService with avatar updates, friend requests, and contact-upload logic that grows the social graph."
---
> Module 13: Creating a Facebook Clone

{{< youtube JZvrsZzdays >}}

If the first half of `UserService` is about identity, the second half is about relationships. This is where the service starts earning its place as the social core of the app rather than just the entry point for account creation.

Avatar retrieval and upload are a good warm-up because they clearly separate public and authenticated behavior. Anyone who knows a user’s public identity can fetch the avatar. Only the authenticated owner should be able to replace it. That is exactly the sort of permission distinction the service layer should make explicit.

Friend requests raise the stakes a little more because they mutate two users’ relationship state and also trigger notifications. That is a strong reminder that many social operations are not local edits to one record. They are workflows that span multiple entities and often require side effects in other parts of the system.

The acceptance path is especially important because it has to verify that the underlying request actually exists. Social operations are full of these asymmetry problems: one user can request, the other can accept, and the system has to make sure those actions line up instead of trusting the client blindly.

The contact-upload logic then moves even deeper into social-graph construction. Once users can upload contact information, the service can start reconciling that data against known users and shadow-user records to improve the “people you may know” suggestions. The implementation here is intentionally simpler than a production social graph would be, but the architectural idea is sound: relationship-building often starts from imported hints rather than explicit in-app actions.

This lesson also quietly reinforces a pattern that matters across the whole backend: batch operations such as `saveAll()` are often worth using when the service is processing a lot of related data. Social features can create large cascades of small updates, and the service layer is where you get to decide whether those updates happen efficiently.

## Further Reading

- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [20. Notification, Newsfeed and ShadowUser Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/097-20-notification-newsfeed-and-shadowuser-entities/)
- [23. NotificationService and MediaService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/100-23-notificationservice-and-mediaservice/)
- [13. Friends Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/090-13-friends-container/)
