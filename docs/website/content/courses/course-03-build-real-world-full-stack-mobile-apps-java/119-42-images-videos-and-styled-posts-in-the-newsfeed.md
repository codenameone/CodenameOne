---
title: "42. Images, Videos and Styled Posts in the Newsfeed"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 42
weight: 119
is_course_lesson: true
description: "Render media attachments and styled text posts inside the feed so the richer composer actually shows up in the timeline."
---
> Module 13: Creating a Facebook Clone

{{< youtube M518p4_Horg >}}

Supporting media in the composer is only half the story. The feed also needs to render those posts convincingly, otherwise the richer posting flow disappears as soon as the user returns to the timeline.

This lesson closes that gap by teaching `NewsfeedContainer` how to display images, videos, and the styled-text post variants that earlier lessons allowed users to create.

The media rendering path is pragmatic. Images are loaded through URL-based media retrieval, while videos are embedded with a size override so they remain visually usable in the feed regardless of the original media dimensions. That is exactly the sort of feed-specific presentation logic the timeline should own.

The lesson is also honest that proper media delivery is a much bigger subject. Real production systems usually transcode video, tailor formats by client capabilities, and optimize bandwidth more aggressively. The simplified approach here is acceptable as long as it is understood as a working foundation rather than a final media pipeline.

The styled-post fixes are just as important. Earlier in the module, style choices existed conceptually, but the feed had not fully respected them. Once rich text and UIID-aware styling are wired together properly, styled posts stop being just a composer novelty and become part of the visible product language.

## Further Reading

- [41. Post Image and Video from NewPostForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/118-41-post-image-and-video-from-newpostform/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [16. The 'New Post' Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/093-16-the-new-post-form/)
- [18. Media Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/095-18-media-entity/)
