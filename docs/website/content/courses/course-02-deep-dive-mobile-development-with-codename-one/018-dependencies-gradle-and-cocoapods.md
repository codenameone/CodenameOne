---
title: "Dependencies - Gradle and CocoaPods"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Native Interfaces - Billing"
module_key: "06-native-interfaces-billing"
module_order: 6
lesson_order: 2
weight: 18
is_course_lesson: true
description: "Understand how native SDK dependencies are expressed and resolved when building Codename One integrations."
---
> Module 6: Native Interfaces - Billing

{{< youtube d1T25sbFUKE >}}

Most native integrations fail before the first line of native code ever runs. The usual problem is dependency setup: the Java wrapper may be correct, but the native platform build is missing the SDK, targeting the wrong platform level, or pulling in conflicting configuration.

That is why dependency handling deserves its own lesson. On Android, third-party native SDKs usually arrive through Gradle coordinates. On iOS, they often arrive through CocoaPods. Codename One sits above those build systems, so the practical question becomes how to express those native dependencies in a way the platform builds can consume.

The older lesson focuses heavily on Braintree-specific build hints and version mismatches. The specific versions in that video are no longer the important part. What still matters is the troubleshooting pattern. When a native dependency fails to build, look for the first meaningful error, not the giant wall of build-tool stack trace below it. Most of the time the real issue is a platform-level mismatch such as minimum SDK version, deployment target, or an incompatible transitive dependency.

This is also one of the places where modern Codename One development is easier if you stay disciplined. Keep build hints explicit, document why they are needed, and prefer maintained integrations when available. If an old lesson suggests unpacking and editing a library artifact by hand, treat that as a temporary workaround from another era, not as a normal workflow. Today the better approach is usually to fix the library, update the dependency declaration, or isolate the workaround in a clearly documented build step.

Native dependency management is not glamorous, but it is part of the cost of crossing the platform boundary. If you understand that Gradle and CocoaPods are simply the native package managers on the other side of your Codename One build, the problem becomes much easier to reason about.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Native Device Functionality, Invoke Native Interfaces](/how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/)
- [How Do I Get Repeatable Builds, Build Against A Consistent Version Of Codename One & Use The Versioning Feature](/how-do-i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature/)
