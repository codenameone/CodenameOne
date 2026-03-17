---
title: "35. Threaded Comments UI - CommentsForm"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 35
weight: 112
is_course_lesson: true
description: "Add a focused comments screen with limited nesting so conversation stays readable while still supporting replies."
---
> Module 13: Creating a Facebook Clone

{{< youtube OnZIaGXDZSo >}}

Comments are one of those features where technical possibility and product usability diverge quickly. The lesson handles that well by intentionally limiting nesting depth instead of chasing arbitrary thread depth just because the model can support it.

That is the right choice here. Infinite nesting looks powerful on paper, but on a phone it quickly becomes hard to read and harder to interact with. A single level of replies captures most of the conversational value without letting the UI collapse into ever-shrinking indentation.

The form structure is also sensible. Comments live in a scrollable center area, while the input field and send action stay fixed at the bottom. That keeps the interaction model familiar and lets replying feel immediate instead of hidden behind another screen.

The reply flow itself is lightweight: choose a parent comment, remember its ID, and send the new comment to the server with that relationship encoded. Once the response succeeds, the client inserts the new comment into the right place in the visible hierarchy and animates the layout back into a stable state.

This is another good example of the course finding the right level of product realism. The comment bubbles, reply action, avatar rendering, and like/comment wiring all make the feature feel real, but the implementation stays small enough that the design decisions are still easy to follow.

## Further Reading

- [24. PostService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/101-24-postservice/)
- [19. Post and Comment Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/096-19-post-and-comment-entities/)
- [29. Newsfeed and Posts From Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/106-29-newsfeed-and-posts-from-server/)
- [Layout Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/036-layout-animations/)
