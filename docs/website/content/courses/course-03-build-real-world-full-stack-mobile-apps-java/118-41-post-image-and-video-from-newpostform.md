---
title: "41. Post Image and Video from NewPostForm"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 41
weight: 118
is_course_lesson: true
description: "Teach the post composer to handle media uploads, disable posting until they complete, and create image or video posts from the same form."
---
> Module 13: Creating a Facebook Clone

{{< youtube U0Ms65vcVr4 >}}

This is where the post composer stops being text-only and starts behaving like a real social composer. That means it has to own more state: attachment ID, MIME type, upload progress, and whether the post action is currently safe to enable.

The lesson handles that by moving from one generic constructor to factory-style entry points for different composer modes. That is a good shift. Once the UI for image posts, video posts, and plain text posts begins to diverge, pretending they are all the same setup path just makes the form harder to reason about.

Disabling the post action until the upload completes is one of the most important UX details here. Without that guard, the user could create references to media that has not actually finished uploading yet. The composer needs to make that state visible and enforce it, not just hope the timing works out.

The upload progress overlay is also well chosen. The user can see the attachment they are about to post, but the UI still makes it clear that something is happening before the post can be sent. That keeps the composer feeling responsive without lying about completion.

The lesson also calls out the orphan-media problem when a user abandons the form after uploading but before posting. That is a real systems concern, and it is good that the tutorial surfaces it instead of pretending uploads only succeed in neat end-to-end flows. The app still needs a cleanup story even if the lesson defers the implementation.

## Further Reading

- [40. Post Media Attachments - Client Side Business Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/117-40-post-media-attachments-client-side-business-logic/)
- [42. Images, Videos and Styled Posts in the Newsfeed](/courses/course-03-build-real-world-full-stack-mobile-apps-java/119-42-images-videos-and-styled-posts-in-the-newsfeed/)
- [39. ImagePicker - Video and Custom Support](/courses/course-03-build-real-world-full-stack-mobile-apps-java/116-39-imagepicker-video-and-custom-support/)
- [16. The 'New Post' Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/093-16-the-new-post-form/)
