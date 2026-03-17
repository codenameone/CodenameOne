---
title: "What is Performance? Breaking Down the Problem"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Performance and Memory Tuning"
module_key: "12-performance-and-memory-tuning"
module_order: 12
lesson_order: 1
weight: 38
is_course_lesson: true
description: "Separate rendering, logic, memory, and perception problems before trying to optimize anything."
---
> Module 12: Performance and Memory Tuning

{{< youtube x-mUTz23Cd4 >}}

Performance problems become much easier to solve once you stop treating them as one category. "The app feels slow" can mean several very different things, and each one points to a different fix.

This lesson makes that distinction well. Rendering problems are not the same as business-logic delays. Memory pressure is not the same as a blocking network call. A UI bug caused by nested scrollables or conflicting focus behavior is not really a performance issue at all, even if the user experiences it as sluggishness.

That separation matters because performance work is one of the places where developers burn time most easily. If you start optimizing without identifying which kind of problem you actually have, you usually end up making the code harder to maintain without meaningfully improving the user experience.

The lesson also makes an important point about perception. Users do not experience performance as a profiler chart. They experience it as feedback, responsiveness, and confidence that the app is doing something sensible. Cached content, progressive loading, and visible movement toward a result often matter as much as raw timing numbers.

Another idea here that deserves repeating is the warning against premature optimization. That advice is not an excuse to ignore performance. It is a reminder to optimize based on evidence. In real applications, a small fraction of the code usually drives most of the measurable pain. Find that fraction first.

So the best way to start performance work is to classify the problem. Is it rendering? Is it logic? Is it memory churn? Is it perceived slowness caused by feedback and loading design? Once you answer that, the next steps become much more rational.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
