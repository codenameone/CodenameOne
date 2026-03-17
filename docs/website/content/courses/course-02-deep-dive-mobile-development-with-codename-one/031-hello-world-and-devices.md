---
title: "Hello World and Devices"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Maps"
module_key: "10-maps"
module_order: 10
lesson_order: 2
weight: 31
is_course_lesson: true
description: "Build the simplest useful map example and understand how native and fallback behavior differ across targets."
---
> Module 10: Maps

{{< youtube m43A68sNcvg >}}

The first useful map example is not complicated. Create a map, make it render on the supported targets, and verify that the fallback behavior is acceptable where native maps are not available. That sounds simple, but it teaches an important lesson: a map feature is only as good as its weakest platform path.

The older lesson compares several rendering outcomes and shows why some map implementations feel much more modern than others. That is still the right instinct. If one path gives you an outdated or visually inconsistent experience while another produces a much better fallback, choose the path that keeps the feature coherent across devices.

This lesson also exposes a subtle but practical issue: not every runtime environment behaves like a real user device. Build-time screenshot generation, simulators, and special platform modes may not support every embedded component. When a feature depends on browser-backed or heavyweight behavior, the application should have a graceful fallback instead of assuming the full environment is always available.

That is why even a "hello world" maps lesson matters. It is where you verify native keys, fallback configuration, and platform-specific behavior before you build markers, overlays, routing, or location-driven flows on top. If the base map setup is fragile, every map feature after that becomes harder to debug.

So the real goal here is not just to get a map on screen. It is to make sure the basic map experience is acceptable on the targets you care about and that your application degrades cleanly where full support is not present.

## Further Reading

- [Introduction and Installation](/courses/course-02-deep-dive-mobile-development-with-codename-one/030-introduction-and-installation/)
- [Developer Guide](/developer-guide/)
- [Markers, Lightweight Overlays and Map Layout](/courses/course-02-deep-dive-mobile-development-with-codename-one/032-markers-lightweight-overlays-and-map-layout/)
