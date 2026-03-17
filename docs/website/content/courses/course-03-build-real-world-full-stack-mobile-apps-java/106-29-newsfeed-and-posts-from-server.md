---
title: "29. Newsfeed and Posts From Server"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 29
weight: 106
is_course_lesson: true
description: "Switch the feed from mock data to the real backend and wire the post composer into the live posting flow."
---
> Module 13: Creating a Facebook Clone

{{< youtube AIsD5MXEK-c >}}

This lesson is where the feed finally becomes real. The UI was already structured as though it were loading actual data; now that assumption becomes true.

The key simplification is that paging no longer has to be faked on the client. Once the server defines the paging strategy, the client can stop inventing its own timestamp-based workaround and just request the next page. That is a good example of the architecture improving both sides at once: the server owns feed delivery rules, and the client becomes simpler because of it.

The new-post form also crosses an important line here. Posting is no longer just a visual action that returns to the previous screen. It now creates a real `Post` object, sends it to the server, and relies on the backend to add it into the feed. That is the right separation of responsibilities. The client describes what the user wants to publish. The server decides how that content enters the social graph.

The note about refresh is also honest and useful. At this stage the feed will not update magically after a new post unless the user refreshes. That is acceptable for now, and it sets up the later discussion about push and more reactive update paths.

The friend-list refresh change follows the same pattern. Once user state is cached locally, the app needs a way to refresh it when relationship data changes. Pull-to-refresh becomes the practical bridge between cached identity state and server-side truth.

## Further Reading

- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [16. The 'New Post' Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/093-16-the-new-post-form/)
- [24. PostService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/101-24-postservice/)
- [27. Client Side ServerAPI](/courses/course-03-build-real-world-full-stack-mobile-apps-java/104-27-client-side-serverapi/)
