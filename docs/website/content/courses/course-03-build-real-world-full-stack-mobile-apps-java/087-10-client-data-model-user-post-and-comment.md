---
title: "10. Client Data Model - User, Post and Comment"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 10
weight: 87
is_course_lesson: true
description: "Define the client-side business objects that the feed, comments, profiles, and later server code will all rely on."
---
> Module 13: Creating a Facebook Clone

{{< youtube PHb5YO77OQk >}}

There is a moment in every UI-heavy project when the mockup stops being “just screens” and starts forcing you to define the underlying data. This is that moment for the Facebook clone.

The lesson introduces `User`, `Post`, and `Comment` as property-based business objects, and that is the right direction for this app. A social product is full of structured data that needs to move between storage, server communication, caching, and UI. Codename One properties fit that problem well because they reduce a lot of repetitive plumbing.

The `User` model comes first because so much of the app depends on it: signup, identity, profile images, social connections, and display names. The avatar handling is especially useful because it treats the image not as a dumb URL but as something the client can fetch, cache, resize, and shape into the exact visual form the UI needs.

That is a recurring pattern throughout this course. The raw data is not always the same thing as the UI-ready data. Sometimes the business object should help bridge that gap directly when the transformation is common and predictable.

The `Post` and `Comment` models then complete the minimum vocabulary needed for a social feed. Visibility, likes, comments, author identity, timestamps, and content styling all show up here because the feed UI is about to demand them. This is a good example of building the model close to the product rather than designing a grand universal schema first.

The nested-comment support hinted at in the `Comment` model is also a good sign of foresight. Even before the threaded UI exists, the model is being shaped so the app will not need to undo basic decisions later.

## Further Reading

- [11. ServerAPI Abstraction Mockup](/courses/course-03-build-real-world-full-stack-mobile-apps-java/088-11-serverapi-abstraction-mockup/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
- [Overview and Basic Model](/courses/course-02-deep-dive-mobile-development-with-codename-one/015-overview-and-basic-model/)
