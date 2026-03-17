---
title: "Build an iOS Native Version of the Kitchen Sink"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Building from the Open Source Project Offline Without Build Servers or Plugin"
module_key: "11-building-from-the-open-source-project-offline-without-build-servers-or-plugin"
module_order: 11
lesson_order: 5
weight: 37
is_course_lesson: true
description: "Understand the source-built iOS path and the translation/bootstrap steps it relies on."
---
> Module 11: Building from the Open Source Project Offline Without Build Servers or Plugin

{{< youtube RsVOanGYjqo >}}

The source-built iOS path makes the underlying Codename One architecture especially visible because it forces you to confront the translation step directly. Unlike the simulator or Java SE path, this is not just "compile the app." It is "prepare the classes, translate them into the form the iOS runtime expects, and then wrap the result in an Xcode project that can actually run on Apple tooling."

That is why the older lesson spends so much time on translation, stubs, generated output, and Xcode setup. The value is not in memorizing those exact commands. The value is in understanding that the iOS path depends on an explicit bootstrap process and a generated native project, not on some hidden magic.

The font-registration and project-setting details in the older video are historical, but the broader message is still current: once you are operating at this layer, you are responsible for the native platform's packaging requirements. iOS expects declared resources, native project metadata, and the right build settings. The standard Codename One build flow handles that for ordinary projects. A source-built path makes you do it yourself.

So the lesson here is the same as the Android one, but even more visible. Working from source is useful when you want to understand or modify the lower layers of the platform. It is not the recommended day-to-day workflow for building applications. Its value is educational and infrastructural, not ergonomic.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Introduction and Setup](/courses/course-02-deep-dive-mobile-development-with-codename-one/033-introduction-and-setup/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)
