---
title: "39. Settings Form and Fetching the Avatar Image"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 39
weight: 76
is_course_lesson: true
description: "Turn the side menu into real account navigation and load the user's avatar with proper local caching."
---
> Module 12: Creating an Uber Clone

{{< youtube tBBtpdz-JJg >}}

Up to this point the Uber clone mostly proves that the core ride flow works. This lesson starts turning the surrounding app chrome into something more believable by making the side menu lead to real account-oriented screens.

That shift matters because a convincing application is not just its flagship interaction. Once a user can open settings or tap their avatar, the app starts feeling like a complete product instead of a narrow demo.

The settings form itself is mostly straightforward UI work, but the more interesting part of the lesson is avatar loading. Fetching a user image sounds simple until you care about round masking, correct sizing, local caching, and the fact that the client is the only place that really knows how large the final image needs to be.

That last point is still the right way to think about it. The server should not hard-code every display size the client might need. The client decides how the image will appear, and the client can then cache the processed result so future loads are fast.

The lesson updates the avatar API so the image can be delivered asynchronously through a callback. That is a natural fit for remote image loading because the UI can continue rendering while the image is fetched from storage or downloaded from the server. If the image is already cached locally, the callback can complete quickly without making the app feel network-bound.

This is one of those small service-layer improvements that pays off all over the UI. Once avatar loading is centralized and cache-aware, the app can reuse it for settings, side-menu identity, profile editing, and eventually any place where another user’s image should appear.

## Further Reading

- [40. Edit User - UI Binding and Multipart Image Upload](/courses/course-03-build-real-world-full-stack-mobile-apps-java/077-40-edit-user-ui-binding-and-multipart-image-upload/)
- [Client Side UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/050-13-client-side-userservice/)
- [Storage, Filesystem and SQL](/courses/course-01-java-for-mobile-devices/010-storage-filesystem-and-sql/)
- [How Do I Fetch an Image from the Resource File / Add a MultiImage](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)
