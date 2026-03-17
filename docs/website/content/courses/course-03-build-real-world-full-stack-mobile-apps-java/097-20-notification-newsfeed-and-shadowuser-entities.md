---
title: "20. Notification, Newsfeed and ShadowUser Entities"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 20
weight: 97
is_course_lesson: true
description: "Add the remaining social entities, including notifications, a ranked newsfeed table, and the shadow-user model behind friend suggestions."
---
> Module 13: Creating a Facebook Clone

{{< youtube Otujb_KyofA >}}

This lesson is where the server model becomes recognizably social. Notifications are the obvious missing piece, but the more interesting work is the introduction of two server-only concepts: a materialized newsfeed entity and the shadow-user model behind “people you may know.”

The notification entity is straightforward and intentionally mirrors the client model closely. That makes sense because notifications are already being rendered on the device and mostly need a clean transport path plus some server-side persistence.

The `Newsfeed` entity is the more ambitious idea. Instead of treating the feed as a fresh query over posts every time, the lesson frames it as something that can be assembled, ranked, stored, and served as its own dataset. That is a stronger design for a real social product because feed ordering is not just raw chronology. It is the result of ranking rules, personalization, and the need for some consistency over time.

Even in this simplified implementation, that architectural move is important. It gives the backend a place to evolve ranking logic without forcing every client request to recompute the whole feed from scratch.

The `ShadowUser` entity is the most revealing social-graph concept in the module. It models information the system knows about people who may not yet be full users, typically because someone uploaded contacts or other identifying data. That is what allows the app to make friend suggestions with more context than the visible user table alone would provide.

Even though the implementation here is intentionally simplified, the lesson is useful because it makes an often invisible product mechanism explicit. Suggestions in social apps do not come from magic. They come from graph-building work and stored relationship hints, and that requires its own model.

## Further Reading

- [14. Notifications Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/091-14-notifications-container/)
- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [22. UserService Part II](/courses/course-03-build-real-world-full-stack-mobile-apps-java/099-22-userservice-part-ii/)
- [13. Friends Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/090-13-friends-container/)
