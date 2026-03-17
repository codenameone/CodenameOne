---
title: "What is Codename One"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 4
weight: 4
is_course_lesson: true
description: "Understand Codename One as a cross-platform Java application framework and build system."
---
> Module 1: Course Lessons

{{< youtube XSztvFzQAr0 >}}

The shortest description of Codename One is that it lets you build cross-platform applications in Java. The longer and more useful answer is that Codename One is a combination of framework APIs, platform ports, build tooling, and a development workflow that turns one codebase into applications that run on multiple targets.

That combination matters because Codename One is not just a widget library. It provides a portable UI toolkit, portable device APIs, a simulator, native build infrastructure, and the glue that keeps those parts working together. When you create a project, write Java code, run it in the simulator, and later send a native build, you are using several layers of the system at once even if you do not think about them separately.

At the application level, you mostly work with the public Codename One API. That is the part that gives you forms, layouts, networking, storage, media, and the rest of the framework surface. Underneath that is a porting layer that maps the framework behavior onto each platform. That is one of the reasons Codename One can behave consistently across platforms while still producing native builds.

The video goes into the history from LWUIT through the early Codename One years, and that background helps explain some design decisions that are still visible in the framework. It also spends time on the old IDE plugin flow and older toolchain assumptions. That part is out of date. Today the practical entry point is a Maven-based project created with [Initializr](/initializr/), not a legacy IDE plugin wizard. The underlying idea is the same, though: the tooling exists to assemble the framework, your application code, and the native build pipeline into one working system.

Another important part of the Codename One model is that the framework is versioned with the application. That means the app you ship carries the framework code it was built with. In practice this gives shipped apps stability and lets you choose when to adopt framework changes. It also makes versioned builds and controlled upgrades much more practical than they would be if every running app depended on one mutable shared runtime.

Codename One also makes a deliberate trade-off in its UI architecture. It uses a lightweight component model so the framework can keep behavior more consistent across targets and so the simulator can behave meaningfully. That design choice is a big part of why the same application logic can run across the supported targets without being rewritten for each platform.

If you are trying to decide whether Codename One is "just Java for mobile", the answer is no. It is a full cross-platform application stack with Java at the center. The more accurate way to think about it is this: Codename One gives you one framework, one language, and one application model, then handles the platform-specific work needed to turn that into real applications.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Getting Started](/getting-started/)
- [Build Server](/build-server/)
- [Moving To Maven](/blog/moving-to-maven/)
