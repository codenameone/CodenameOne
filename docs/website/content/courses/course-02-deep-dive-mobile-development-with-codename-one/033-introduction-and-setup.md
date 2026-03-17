---
title: "Introduction and Setup"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Building from the Open Source Project Offline Without Build Servers or Plugin"
module_key: "11-building-from-the-open-source-project-offline-without-build-servers-or-plugin"
module_order: 11
lesson_order: 1
weight: 33
is_course_lesson: true
description: "Understand when building from source is useful and what pieces of the Codename One toolchain are involved."
---
> Module 11: Building from the Open Source Project Offline Without Build Servers or Plugin

{{< youtube wqcM8pSOGTY >}}

Working directly from the Codename One source tree is not the normal path for most application developers. That is worth saying upfront because this module makes more sense when treated as advanced infrastructure knowledge rather than as the default workflow.

If your real goal is just to build and ship apps, the hosted build flow and modern project tooling are usually the right choice. Even the older lesson says as much. Building everything locally from source is valuable for a different set of reasons: understanding how the platform is assembled, being able to debug the lower layers more confidently, or contributing to the framework itself.

The conceptual model in the lesson is still the useful part. Codename One is not one monolith. It is a combination of APIs, ports, runtimes or VMs on some targets, tooling, and supporting binaries or stubs that make compilation practical across platforms. Once you understand those pieces, the source layout stops feeling arbitrary.

The exact version guidance in the video is obviously dated now, and the recommended setup for ordinary application development has moved on. But the deeper lesson is still current: if you build from source, you are taking responsibility for more of the platform stack. That means understanding what each repository or module contributes and which parts exist as implementation support rather than public API.

So this lesson is best read as an anatomy-of-the-platform walkthrough for advanced users. It is not the new default way to start a project. It is the way to get closer to the internals when you deliberately need that level of control.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Get Repeatable Builds, Build Against A Consistent Version Of Codename One & Use The Versioning Feature](/how-do-i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature/)
- [How Do I Use Offline Build](/how-do-i/how-do-i-use-offline-build/)
