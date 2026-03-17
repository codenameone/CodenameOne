---
title: "Case Study: Performance Problems in the Kitchen Sink"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Performance and Memory Tuning"
module_key: "12-performance-and-memory-tuning"
module_order: 12
lesson_order: 7
weight: 44
is_course_lesson: true
description: "See how small architectural mistakes turn into real performance problems and how to fix them methodically."
---
> Module 12: Performance and Memory Tuning

{{< youtube qGJaVaQ_MNY >}}

Performance advice is easiest to remember when it is attached to a real mistake. That is why this case study is valuable. Instead of repeating general rules, it shows how ordinary-looking decisions in a real application created noticeable problems and how those problems were fixed.

The first example is a perfect reminder not to trust first impressions. The contacts demo seemed slow for an obvious reason, but profiling and inspection uncovered a different issue entirely: the same resource file was being loaded repeatedly in places where the code looked superficially innocent. That kind of bug is common because the expensive part is often hidden behind constructors, helpers, or convenience APIs.

The second example is equally important because it shows the limits of the "just move it to a background thread" reflex. Loading contact images off the EDT sounded like the right fix, and in one sense it was. But the background work still competed with the user experience during scrolling. The real improvement came from coordinating the work with user activity so the app deferred expensive updates until interaction settled down.

That is a strong pattern to remember. Performance is often not about doing less work overall. It is about doing work at a better time. If the UI is busy, defer the optional work. If the user is idle, use that moment to fill in detail. This is one of the cleanest ways to improve perceived and actual responsiveness at the same time.

So the real lesson from the case study is methodological. Do not assume. Measure. Look for hidden repeated work. And when a background task still harms the experience, ask whether the issue is not the work itself but its timing relative to user interaction.

## Further Reading

- [What is Performance? Breaking Down the Problem](/courses/course-02-deep-dive-mobile-development-with-codename-one/038-what-is-performance-breaking-down-the-problem/)
- [EDT, Threading, Caching and Soft References](/courses/course-02-deep-dive-mobile-development-with-codename-one/039-edt-threading-caching-and-soft-references/)
- [Profiling on the Desktop, Using the Performance Monitor Tool](/courses/course-02-deep-dive-mobile-development-with-codename-one/042-profiling-on-the-desktop-using-the-performance-monitor-tool/)
