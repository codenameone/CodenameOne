---
title: "40. Post Media Attachments - Client Side Business Logic"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 40
weight: 117
is_course_lesson: true
description: "Prepare the client-side post and upload pipeline so attachment uploads can be tracked and integrated into post creation cleanly."
---
> Module 13: Creating a Facebook Clone

{{< youtube H7WW4GwXK8o >}}

This lesson is mostly infrastructure, but it is important infrastructure. Once posts can reference attachments, the client has to stop treating media upload as an invisible prelude and start managing it as a real step in the posting workflow.

The first part is straightforward: the post model gains an attachments field so the client can receive and send attachment metadata without special-case parsing.

The more important change is in the upload API. If attachments may be large or slow to upload, the client needs access to the underlying request object so it can show upload progress and coordinate posting behavior around that state. Returning a bare callback result is not enough anymore.

That is the correct direction. The upload process is no longer just a helper that vanishes into the background. It is now part of the user-visible experience, and the UI needs hooks for progress, enablement, and error handling.

The lesson also highlights a very practical detail: sometimes the app has raw bytes, and sometimes it only has a file path. The API layer should make room for both instead of forcing every caller to normalize the data in exactly the same way before it can upload anything.

## Further Reading

- [41. Post Image and Video from NewPostForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/118-41-post-image-and-video-from-newpostform/)
- [39. ImagePicker - Video and Custom Support](/courses/course-03-build-real-world-full-stack-mobile-apps-java/116-39-imagepicker-video-and-custom-support/)
- [16. The 'New Post' Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/093-16-the-new-post-form/)
- [18. Media Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/095-18-media-entity/)
