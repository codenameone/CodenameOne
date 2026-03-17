---
title: "39. ImagePicker - Video and Custom Support"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 39
weight: 116
is_course_lesson: true
description: "Extend the image picker so it can represent videos and custom-captured media without forcing the rest of the app to care about file-source details."
---
> Module 13: Creating a Facebook Clone

{{< youtube wp6gqYLD-XM >}}

As soon as post attachments stop being image-only, the picker stops being a trivial helper. This lesson expands it into something that can represent either images or videos while still keeping the rest of the app insulated from the exact source of the file.

That is the most valuable idea here. The rest of the app should not need one code path for gallery images, another for gallery videos, and yet another for low-level camera captures. A picker abstraction that can carry the chosen file, preview image when applicable, and enough metadata for upload is exactly the right seam.

The lesson also makes a pragmatic distinction between images and videos. Images benefit from being decoded, resized, previewed, and cached as UI-ready content. Videos are more transient and more expensive, so the picker mostly needs to preserve file access and enough metadata to upload or preview them sensibly.

The custom-factory methods are also useful. Once the app later introduces low-level camera capture, it will already have a way to create a picker-like object from an existing file or byte array without pretending the media came through the ordinary gallery selection path.

This is one of those small infrastructure lessons that pays off disproportionally later because it keeps every feature that touches media from reinventing file-source logic on its own.

## Further Reading

- [40. Post Media Attachments - Client Side Business Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/117-40-post-media-attachments-client-side-business-logic/)
- [41. Post Image and Video from NewPostForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/118-41-post-image-and-video-from-newpostform/)
- [43. Low Level Camera Integration](/courses/course-03-build-real-world-full-stack-mobile-apps-java/120-43-low-level-camera-integration/)
- [How Do I Take a Picture with the Camera](/how-do-i/how-do-i-take-a-picture-with-the-camera/)
