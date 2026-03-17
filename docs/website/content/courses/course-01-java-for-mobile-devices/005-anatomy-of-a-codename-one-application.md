---
title: "Anatomy of a Codename One Application"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 5
weight: 5
is_course_lesson: true
description: "Understand the structure of a modern Codename One project and the files that matter."
---
> Module 1: Course Lessons

{{< youtube 8fxZVc1hw6Q >}}

Every framework has a moment where the generated project stops feeling friendly and starts feeling like a pile of mysterious files. This lesson is about getting past that point. A Codename One project is much easier to work with once you know which files define the application, which files are generated, and which parts are just build output that you can ignore.

The video walks through the old IDE-plugin project layout and spends a lot of time on `build.xml`, `lib`, and other files that were central to the older Ant-based workflow. That part is out of date for new projects. Today you will usually start from a Maven project, so the file and directory structure looks different. Even so, the important ideas from the lesson still translate well: some files define the app, some define the build, some are caches or artifacts, and some exist only to support a specific toolchain.

In a modern Codename One project, the first files worth understanding are the Maven build files and the application source itself. The Java application class still expresses the lifecycle of the app. Resource directories still contain the assets and configuration the app depends on. Build-related files still describe how the project is packaged and how platform-specific work is triggered. The main difference is that Maven now owns the overall build structure instead of the old plugin-generated Ant layout.

One file that remains conceptually important is `codenameone_settings.properties`. It still acts as the central configuration point for many application-specific settings, including identifiers, build hints, and platform-specific options. You do not need to hand-edit it for everything, and in many cases it is better to use the supported tooling, but you do need to know it exists and what role it plays. When something about the application's identity or native configuration changes, this file is often involved.

Build output should be treated as disposable. Whether it is a Maven `target` directory or older `build` and `dist` folders from the plugin era, generated artifacts are there to support a build or a simulator run. They are useful for debugging packaging problems, checking what assets ended up in the final jar, or understanding why an application became unexpectedly large, but they are not the place to make source-level changes.

That distinction becomes especially important when you start troubleshooting. If the final package is too large, inspect the generated artifact and see what actually got bundled. If a build behaves differently from what you expect, check whether the configuration lives in source files, properties, or generated output. A lot of confusion disappears once you stop treating every file in the project tree as equally important.

So the real anatomy of a Codename One application is simpler than it first appears. There is the app code you write, the project configuration that describes how it should be built, the assets it depends on, and the generated output produced along the way. Learn those boundaries early and the rest of the project becomes much easier to reason about.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Getting Started](/getting-started/)
- [Build Server](/build-server/)
- [Hello World](/hello-world/)
- [How Do I Get Repeatable Builds, Build Against A Consistent Version Of Codename One & Use The Versioning Feature](/how-do-i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature/)
