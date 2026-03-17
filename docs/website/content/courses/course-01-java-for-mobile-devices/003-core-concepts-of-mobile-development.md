---
title: "Core Concepts of Mobile Development"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 3
weight: 3
is_course_lesson: true
description: "Understand the mobile constraints that shape layout, images, memory usage, and device builds."
---
> Module 1: Course Lessons

{{< youtube hwsWdhJHl8Q >}}

Mobile development feels different from desktop development because the constraints are different. Screens vary wildly in size and density, memory is tighter, interaction is touch-first, and native platforms impose rules around packaging, signing, and distribution. If those constraints feel annoying at first, that is normal. The key is to design with them instead of fighting them.

The first concept to get comfortable with is the difference between resolution and density. Resolution tells you how many pixels are on a screen. Density tells you how tightly packed those pixels are. Two devices can have similar resolutions and still look very different because one has far more pixels per inch. That is why a UI that seems fine on one device can suddenly look too small, blurry, or badly balanced on another. In Codename One, this is one of the reasons layouts matter so much. Hard-coded sizes age badly across real devices.

Images are where density becomes expensive. If you ship only one low-resolution asset, it will look soft or pixelated on modern devices. If you ship only very large assets, the app pays in download size and memory use. The practical answer is to be selective. Use vector-friendly approaches where you can, especially for icons, and use raster images only where they genuinely add value. Codename One's multi-image support exists to help match assets to device density, but that should not become an excuse to dump huge image libraries into the app.

For icons and simple symbolic graphics, built-in material icons and font-based icons are often the better choice. They scale cleanly, work well with theme colors, and avoid the asset explosion that comes from exporting multiple bitmap versions of the same shape. For photos and more detailed artwork, regular image assets still make sense, but you should think carefully about where the sharpest version is really needed.

Another mobile concept that surprises people is that shipping an app is not just a matter of compiling code. Native platforms require signing and, in some cases, provisioning. On Android, signing establishes the identity of the app and controls who can publish updates to it. On iOS, certificates and provisioning profiles are part of both development and distribution. That complexity is not specific to Codename One. It is part of mobile development itself, and understanding it early makes later build and release work much less mysterious.

The video spends a fair amount of time on signing and provisioning, and that material is still relevant. What has changed is the workflow around the project. Today you will usually create a Maven project first and then send builds through the current Codename One tooling. But the native platform rules have not gone away. If anything, they are the reason Codename One's build tooling is valuable in the first place.

The broader lesson is that mobile work rewards adaptability. You do not control the screen, the density, the store requirements, the operating system policies, or the amount of memory on the user's device. A good mobile framework helps you live with those constraints. A good mobile developer learns to expect them.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Build Server](/build-server/)
- [How Do I Fetch An Image From The Resource File, Add A Multiimage](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)
- [How Do I Create A 9 Piece Image Border](/how-do-i/how-do-i-create-a-9-piece-image-border/)
- [How Do I Create An iOS Provisioning Profile](/how-do-i/how-do-i-create-an-ios-provisioning-profile/)
