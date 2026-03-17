---
title: "Profiling on Devices iOS and Android"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Performance and Memory Tuning"
module_key: "12-performance-and-memory-tuning"
module_order: 12
lesson_order: 6
weight: 43
is_course_lesson: true
description: "Move from simulator measurements to on-device profiling when the real bottleneck only appears on hardware."
---
> Module 12: Performance and Memory Tuning

{{< youtube YwkwqXkHGwg >}}

The simulator gets you far, but not all the way. Some performance problems only become visible on the real device, where GPU behavior, memory pressure, input latency, and platform-specific implementation details finally line up the way your users will experience them.

That is why device profiling matters. Android Studio and Xcode provide the next level of evidence once the simulator has stopped being enough. The goal is not to abandon the desktop tools. It is to move to hardware when you need to confirm what is really happening under mobile conditions.

The Android guidance in the older lesson is still useful because it combines two kinds of observation: profiler data and developer-option visual debugging. GPU overdraw visualization, in particular, is one of those rare tools that can make a rendering problem immediately visible even before you have fully quantified it. If a screen is awash in red, you already know where to start asking questions.

On iOS, the tooling differs, but the principle is identical. Use the platform profiler to isolate expensive methods, understand where time is being spent on device, and confirm whether the suspected bottleneck is real. The purpose is not just to collect screenshots of charts. It is to narrow the problem until you can act on it.

The broader lesson is that performance work should move from general to specific. Start with conceptual classification, then use simulator tools, then escalate to device profiling when the evidence demands it. That progression keeps you from over-investing in low-level profiling too early while still giving you a path to real answers when desktop measurements are no longer enough.

## Further Reading

- [Profiling on the Desktop, Using the Performance Monitor Tool](/courses/course-02-deep-dive-mobile-development-with-codename-one/042-profiling-on-the-desktop-using-the-performance-monitor-tool/)
- [What is Performance? Breaking Down the Problem](/courses/course-02-deep-dive-mobile-development-with-codename-one/038-what-is-performance-breaking-down-the-problem/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
