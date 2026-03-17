---
title: "Introduction and Installation"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Maps"
module_key: "10-maps"
module_order: 10
lesson_order: 1
weight: 30
is_course_lesson: true
description: "Choose the right map approach in Codename One and understand the setup required for native maps."
---
> Module 10: Maps

{{< youtube HRTLnLecoMA >}}

Maps are one of those features that look simple in a product brief and become surprisingly nuanced once implementation begins. The first decision is not how to place a marker. It is which kind of map integration you actually want and how close that integration needs to be to the native experience on each platform.

The older lesson does a useful job of separating the available map approaches. Some are native, some are browser-backed fallbacks, and some represent older technology that is no longer the preferred path. That distinction still matters. For most current projects, the native map implementation plus a reasonable fallback is the right mental model. Older tiled-map approaches belong to legacy compatibility, not to the default recommendation for new work.

This lesson also highlights a practical truth about map integrations: they are not self-contained. You usually need provider keys, platform-specific configuration, and some awareness of quota or billing implications. That means map support is partly a UI feature and partly an operational setup task.

One place where the older material is still directionally right is key handling. Hard-coding keys into a demo may be acceptable for a tutorial, but production applications should treat provider keys as operational assets and protect them appropriately. The exact best practice depends on the provider and the architecture, but the principle is the same: keep the production path more careful than the tutorial path.

So the right starting point for maps in Codename One is not "drop in a map widget." It is "pick the right implementation strategy, understand the native and fallback story, and configure the provider side correctly before building map-heavy features on top."

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Native Device Functionality, Invoke Native Interfaces](/how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/)
- [Communicating with the Server](/courses/course-02-deep-dive-mobile-development-with-codename-one/025-communicating-with-the-server/)
