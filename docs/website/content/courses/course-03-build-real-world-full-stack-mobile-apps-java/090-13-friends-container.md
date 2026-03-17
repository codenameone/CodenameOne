---
title: "13. Friends Container"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 13
weight: 90
is_course_lesson: true
description: "Render friend requests and suggestions with a simpler container-based approach that still supports refresh and dynamic avatars."
---
> Module 13: Creating a Facebook Clone

{{< youtube LZ_ydua__gY >}}

After the feed, the friends screen feels almost relaxing. It is a simpler UI, but it still has enough moving parts to show how the same data and styling ideas can support a different kind of social surface.

The lesson makes a sensible choice by using a regular scrollable container instead of `InfiniteContainer`. Not every list in an app needs infinite loading just because one important screen does. Friend requests and suggestions are finite enough here that a simpler structure keeps the code easier to understand.

The screen itself is built from two kinds of sections: requests that need action and suggestions that invite exploration. That split is useful because it gives the UI a little product logic instead of flattening all relationships into one undifferentiated list.

The avatar handling is also a good example of being flexible about presentation. The feed used circular identity images because that matched the product language there. This screen uses square images because the visual reference calls for them. The underlying user identity is the same, but the rendering can still adapt to the needs of the specific screen.

The confirm/delete controls then finish the pattern. The screen is not just decorative. It expresses clear actions, and the CSS gives those actions distinct visual weight without needing a large amount of custom code.

## Further Reading

- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [39. Settings Form and Fetching the Avatar Image](/courses/course-03-build-real-world-full-stack-mobile-apps-java/076-39-settings-form-and-fetching-the-avatar-image/)
- [Client Side UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/050-13-client-side-userservice/)
- [How Do I Create a List of Items the Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
