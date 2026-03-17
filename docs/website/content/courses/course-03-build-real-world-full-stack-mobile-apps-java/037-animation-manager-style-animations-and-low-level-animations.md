---
title: "Animation Manager, Style Animations and Low Level Animations"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Animations"
module_key: "11-animations"
module_order: 11
lesson_order: 3
weight: 37
is_course_lesson: true
description: "Coordinate style animation safely and understand the lower-level animation hooks that power custom effects."
---
> Module 11: Animations

{{< youtube qUJ4FwZMEQY >}}

By the time you reach this lesson, the easy animation APIs are no longer enough. You start needing orchestration: style changes that happen in sequence, animations that should not collide with each other, and low-level control for custom graphical effects.

That is where the animation manager becomes useful. Style animation does not fit as neatly into the older one-method convenience APIs because it has to coordinate state changes on components that may already be participating in other updates. The animation manager gives the form one place to serialize and supervise those operations.

This is more important than it sounds. Without coordination, the UI can become fragile when users trigger structural changes while an animation is still running. The manager exists partly to make these interactions safe by postponing conflicting operations until the current animation finishes.

The style animation example in the video is a good one because it shows both the power and the limit of this approach. Some style properties animate beautifully, especially values such as colors. Others are too discrete to produce satisfying motion. The point is not to animate every style attribute you can find. It is to choose the ones that meaningfully communicate change.

The lesson then moves all the way down to the low-level animation hooks, and that material is worth keeping because it explains how Codename One animation really works. The EDT advances in ticks, animated components opt into receiving animation callbacks, and repainting happens only when the framework is told something visual actually changed.

That design is powerful, but it comes with responsibility. A component that stays registered for animation unnecessarily can waste battery and CPU. An `animate()` method that does too much work or returns true too often can quietly become a performance problem. This is one of those APIs where understanding the cost model matters as much as understanding the signatures.

So the real lesson here is not “here is one more animation trick.” It is that Codename One gives you both high-level convenience APIs and low-level rendering hooks, and the farther down you go, the more disciplined you need to be about performance, registration, and repaint frequency.

## Further Reading

- [Transitions](/courses/course-03-build-real-world-full-stack-mobile-apps-java/035-transitions/)
- [Layout Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/036-layout-animations/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
- [What Is Performance? Breaking Down the Problem](/courses/course-02-deep-dive-mobile-development-with-codename-one/037-what-is-performance-breaking-down-the-problem/)
