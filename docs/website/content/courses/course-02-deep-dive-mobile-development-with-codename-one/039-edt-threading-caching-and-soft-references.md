---
title: "EDT, Threading, Caching and Soft References"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Performance and Memory Tuning"
module_key: "12-performance-and-memory-tuning"
module_order: 12
lesson_order: 2
weight: 39
is_course_lesson: true
description: "Avoid the most common performance mistakes by respecting the EDT and caching deliberately."
---
> Module 12: Performance and Memory Tuning

{{< youtube 85MyytvMS9I >}}

If there is one performance rule that deserves to be repeated until it becomes reflex, it is this: do not block the EDT. Most performance advice in Codename One eventually comes back to that point, because the EDT owns interaction and painting. When you make it wait, the whole application feels worse no matter how efficient the rest of the code may be.

That does not mean "move everything to another thread and hope for the best." Background work still competes for CPU time. On mobile hardware especially, a badly behaved background task can starve the UI just as effectively as work on the EDT if it burns enough processor time. So good threading is not just about moving work away from the UI. It is about moving it away responsibly.

The lesson's discussion of `invokeAndBlock`, `callSerially`, and idle-time work points to the real trade-off: convenience versus control. Sometimes a blocking abstraction makes code easier to write. Sometimes a cleaner callback or explicit background thread is the better choice. The right answer depends on how much work is being done and how sensitive the interaction needs to feel.

Caching is the other half of the story. In practice, many of the biggest performance wins come from caching the right thing in the right place. But caching also creates memory pressure, and memory pressure causes its own performance problems. That is why there is no universal answer to questions like "should I cache this image or this component?" The cost depends on what gets duplicated, how often it is reused, and how aggressively memory needs to be reclaimed on target devices.

This is where soft references and other reclaimable caches become useful. They let the application benefit from reuse when memory is available without insisting on holding everything forever. The point is not to be clever for its own sake. It is to keep the application responsive while avoiding wasteful re-creation of expensive objects.

The lesson's advice on form reuse is also still relevant. Reusing a form is often simpler and smoother than rebuilding it constantly, but that stops being true if the form drags along a huge memory footprint full of heavyweight images or other costly state. Performance work is always about choosing where to pay.

So the practical takeaway is this: keep the EDT lean, use threads deliberately, and treat caching as a measured optimization rather than a reflex. Most performance tuning becomes easier once those three habits are in place.

## Further Reading

- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
- [What is Performance? Breaking Down the Problem](/courses/course-02-deep-dive-mobile-development-with-codename-one/038-what-is-performance-breaking-down-the-problem/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
