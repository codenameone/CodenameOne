---
title: "12. The Newsfeed Container"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 12
weight: 89
is_course_lesson: true
description: "Assemble the first real social feed by combining infinite scrolling, a post composer stub, and reusable post-rendering helpers."
---
> Module 13: Creating a Facebook Clone

{{< youtube B4QCJRFpG-k >}}

This is the lesson where the Facebook clone starts feeling alive. The news feed is the first screen in the app that combines real scrolling content, structured models, reusable view fragments, and a data source that looks like a real server.

Using `InfiniteContainer` is a natural fit here. A feed is exactly the kind of UI that should not pretend it knows all of its content up front. The container can ask for more entries as the user scrolls and can also support pull-to-refresh without requiring a completely different architecture for reloading.

The lesson handles that well by separating the feed into small pieces. There is a top “what’s on your mind” style post bar, a title/header area for each post, a stats area, and then a final method that assembles those parts into one feed item. That keeps the code readable and makes it much easier to evolve the rendering later.

The time-formatting helper from the previous lesson also pays off here. Social feeds always need human-readable time, and handling that in a utility layer instead of repeating it in every view method is exactly the kind of small discipline that keeps UI code healthy as the app grows.

The feed item assembly code also shows good layout judgment. Buttons that should align are placed in a grid. Repeated structural spacing is centralized in reusable UIIDs. Rich text is used where post content needs it. The lesson is not just rendering data. It is turning several reusable visual rules into one coherent feed entry.

## Further Reading

- [10. Client Data Model - User, Post and Comment](/courses/course-03-build-real-world-full-stack-mobile-apps-java/087-10-client-data-model-user-post-and-comment/)
- [11. ServerAPI Abstraction Mockup](/courses/course-03-build-real-world-full-stack-mobile-apps-java/088-11-serverapi-abstraction-mockup/)
- [13. Friends Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/090-13-friends-container/)
- [How Do I Create a List of Items the Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
