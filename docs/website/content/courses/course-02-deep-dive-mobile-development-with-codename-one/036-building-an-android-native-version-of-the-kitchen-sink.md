---
title: "Building an Android Native Version of the Kitchen Sink"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Building from the Open Source Project Offline Without Build Servers or Plugin"
module_key: "11-building-from-the-open-source-project-offline-without-build-servers-or-plugin"
module_order: 11
lesson_order: 4
weight: 36
is_course_lesson: true
description: "Understand what is involved in assembling a native Android app directly from the Codename One sources."
---
> Module 11: Building from the Open Source Project Offline Without Build Servers or Plugin

{{< youtube 5eMvwRcDcug >}}

Building a native Android application directly from the Codename One sources is a useful exercise in understanding how the Android port is assembled, but it is not a lightweight workflow. The amount of setup in the older lesson makes that point on its own.

The conceptual structure is still worth learning. You need the portable sources, the Android-specific implementation sources, the right resources and bootstrap files in the places Android expects, and an activity layer that maps Android lifecycle behavior onto the Codename One application model. Once all of those are aligned, the app can run as a normal Android application.

The old lesson also spends time on Java 8 support, Gradle configuration, copied implementation files, and Android activity wiring. The specific historical version details are not the important thing now. The important thing is understanding why these steps exist at all: Android needs an actual native project structure, and Codename One's Android port includes implementation classes that intentionally override or extend the portable layer.

This is one more reason the normal hosted or standard build flow remains the right default for most application work. When you bypass that flow, you take on the responsibility of reproducing all the Android-specific packaging and lifecycle glue yourself. That can be educational and sometimes necessary for framework-level work, but it is not a simpler way to build apps.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Introduction and Setup](/courses/course-02-deep-dive-mobile-development-with-codename-one/033-introduction-and-setup/)
- [How Do I Use Offline Build](/how-do-i/how-do-i-use-offline-build/)
