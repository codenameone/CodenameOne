---
title: "Running the Kitchen Sink in the Simulator"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Building from the Open Source Project Offline Without Build Servers or Plugin"
module_key: "11-building-from-the-open-source-project-offline-without-build-servers-or-plugin"
module_order: 11
lesson_order: 2
weight: 34
is_course_lesson: true
description: "Use the simulator as the first proof that a direct source build is assembled correctly."
---
> Module 11: Building from the Open Source Project Offline Without Build Servers or Plugin

{{< youtube l1TPKuHYr9c >}}

If you are building Codename One directly from source, the simulator is the first place to prove that your environment is actually coherent. Before you worry about native packaging, you want one unambiguous success signal: the framework builds, the demo project links against it correctly, and the application runs in the Java SE simulator path.

That is why the Kitchen Sink demo is a good target. It exercises a wide range of framework behavior, so if it launches successfully you have more confidence than you would from a toy example. It is not a guarantee that every platform path is correct, but it is the right first checkpoint.

The older lesson necessarily spends time on the mechanics of assembling jars, copying the right pieces into place, and working around the absence of the usual build client tooling. The exact commands are historical, but the practical lesson remains: once you operate outside the normal project-generation path, you have to understand which artifacts the simulator actually expects and which pieces of the usual workflow you are now responsible for yourself.

This is another reason direct-source work should be treated as an advanced mode. The simulator run is no longer "click run." It becomes "verify that the platform pieces, demo project, and launch path all line up." That can be valuable knowledge, but it is different from normal application development.

## Further Reading

- [Introduction and Setup](/courses/course-02-deep-dive-mobile-development-with-codename-one/033-introduction-and-setup/)
- [Developer Guide](/developer-guide/)
- [Hello World](/hello-world/)
