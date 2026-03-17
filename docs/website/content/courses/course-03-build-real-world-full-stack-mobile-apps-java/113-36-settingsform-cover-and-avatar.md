---
title: "36. SettingsForm - Cover and Avatar"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 36
weight: 113
is_course_lesson: true
description: "Build a profile/settings screen that lets the user update cover and avatar images while keeping the image-picking logic reusable."
---
> Module 13: Creating a Facebook Clone

{{< youtube _JCsyyIH02E >}}

The settings screen is where the Facebook clone starts feeling personal instead of anonymous. This lesson focuses on the two profile images users care about most: the avatar and the cover.

The dedicated `ImagePicker` helper is a good architectural move. The actual picking behavior is simple right now, but wrapping it in a small class means the metadata, chosen file, preview image, and eventual upload path can stay together. That keeps the settings form from absorbing low-level image-picking concerns directly.

The UI itself is mostly about layered composition. Cover image, avatar, and their change buttons need to overlap in a way that looks intentional rather than accidental. The layered-layout approach is a good fit because it lets the form place those controls relative to the images they actually modify.

The lesson also keeps a clear distinction between local and remote updates. When the user picks a new image, the UI updates immediately enough to feel responsive, but the backend still receives a real media upload and the user record is updated so the change persists.

This is one of the better examples in the course of progressive feature design. The screen does not try to solve every settings problem at once. It gets the most visible identity-editing actions working cleanly, then leaves room for the next lesson to handle more generic profile attributes.

## Further Reading

- [37. Generic Settings using InstantUI - Automatic Dynamic UI Generation](/courses/course-03-build-real-world-full-stack-mobile-apps-java/114-37-generic-settings-using-instantui-automatic-dynamic-ui-generation/)
- [39. ImagePicker - Video and Custom Support](/courses/course-03-build-real-world-full-stack-mobile-apps-java/116-39-imagepicker-video-and-custom-support/)
- [40. Edit User - UI Binding and Multipart Image Upload](/courses/course-03-build-real-world-full-stack-mobile-apps-java/077-40-edit-user-ui-binding-and-multipart-image-upload/)
- [18. Media Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/095-18-media-entity/)
