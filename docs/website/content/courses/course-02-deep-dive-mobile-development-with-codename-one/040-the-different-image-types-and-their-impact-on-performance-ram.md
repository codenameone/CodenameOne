---
title: "The Different Image Types and Their Impact on Performance/RAM"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Performance and Memory Tuning"
module_key: "12-performance-and-memory-tuning"
module_order: 12
lesson_order: 3
weight: 40
is_course_lesson: true
description: "Choose image representations with a clear understanding of their memory and rendering trade-offs."
---
> Module 12: Performance and Memory Tuning

{{< youtube DWifLQZyPDE >}}

Images are often the biggest single contributor to both application size and runtime memory pressure. That means image choices are not just aesthetic or API-level decisions. They are core performance decisions.

The key lesson here is that not all image representations cost the same thing. Some image types are fast to draw but expensive in memory. Others are compact until decoded, but pay a cost when they need to expand into drawable form. Still others exist mainly to support specific low-level use cases and should not be your default tool.

Encoded images are especially important in Codename One because they are the workhorse of practical memory management. The image can stay compact while idle and expand when actually needed. That makes them a strong fit for many UI situations, especially when the alternative is keeping a large decoded image resident all the time. But that benefit comes with a new responsibility: understand when an image should stay locked in memory and when it should be allowed to fall out of cache.

This is where many performance issues become subtle. A decoded image may render smoothly because it stays warm in memory, but too many of those images can cause memory pressure and cache churn elsewhere. An encoded image may be smaller overall, but if it is constantly being re-decoded during active use, the app can feel sluggish. The right answer depends on how often the image is used and whether it is central to the current screen.

The broader point is that image optimization is not just about file size. Runtime footprint matters just as much, and the simple width-times-height-times-four mental model is still a useful way to estimate the cost of keeping decoded images alive.

For modern projects, this lesson pairs well with the general advice to keep styling in CSS and use icon fonts or vector-friendly approaches where possible. Every place where an image can be replaced by a cheaper or more flexible representation is one less place where you have to pay this memory/performance trade-off.

## Further Reading

- [Themeing](/themeing/)
- [How Do I Fetch An Image From The Resource File, Add A Multiimage](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)
- [What is Performance? Breaking Down the Problem](/courses/course-02-deep-dive-mobile-development-with-codename-one/038-what-is-performance-breaking-down-the-problem/)
