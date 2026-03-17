---
title: "16. The 'New Post' Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 16
weight: 93
is_course_lesson: true
description: "Create the first version of the post-composer UI and let users switch between several visual post styles."
---
> Module 13: Creating a Facebook Clone

{{< youtube 0a6mPI412C4 >}}

The post composer is where the Facebook clone stops being a read-mostly app and becomes participatory. Even in this first simplified version, that changes the feel of the whole product.

The form is intentionally scoped to text-first posting, and that is the right call. Images, video, and richer media behavior can come later. The important step here is establishing the composer as its own screen with clear identity, visibility controls, and a way to preview different post styles.

The style picker at the bottom is the most interesting part of the lesson. Instead of treating post styling as a bag of unrelated options, the UI presents a small set of named visual modes. That makes the experience fast and tactile. The user is not editing raw styling properties. They are choosing among recognizable post treatments.

The use of radio-style selection for those styles is a good fit because only one visual mode should be active at a time. The composer can then translate that choice into a combination of UIIDs and content treatment without exposing that machinery to the user.

This lesson also shows a useful boundary between content and presentation. The post text stays the same, but the surrounding style changes. That separation matters later when the app needs to store style choices alongside the post rather than flattening them into one opaque blob of rendered content.

## Further Reading

- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [10. Client Data Model - User, Post and Comment](/courses/course-03-build-real-world-full-stack-mobile-apps-java/087-10-client-data-model-user-post-and-comment/)
- [Working with CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
- [Style Customization 1 - Introduction and Basics](/courses/course-03-build-real-world-full-stack-mobile-apps-java/016-style-customization-1-introduction-and-basics/)
