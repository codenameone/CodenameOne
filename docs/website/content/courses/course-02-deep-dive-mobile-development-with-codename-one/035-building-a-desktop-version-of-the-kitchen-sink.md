---
title: "Building a Desktop Version of the Kitchen Sink"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Building from the Open Source Project Offline Without Build Servers or Plugin"
module_key: "11-building-from-the-open-source-project-offline-without-build-servers-or-plugin"
module_order: 11
lesson_order: 3
weight: 35
is_course_lesson: true
description: "Wrap a Codename One app for desktop execution and understand what the desktop bootstrap is responsible for."
---
> Module 11: Building from the Open Source Project Offline Without Build Servers or Plugin

{{< youtube PbRbzCGQCQE >}}

Running a Codename One app as a desktop application is a useful way to understand what the simulator and Java SE port are really doing for you. The central idea is straightforward: Codename One still needs a bootstrap environment, and on desktop that environment is responsible for creating the host window, initializing the port, and handing lifecycle control to the application.

That is why the lesson introduces a small desktop stub. The stub is not application logic in the normal sense. It is the code that embeds the Codename One runtime into a standard Java desktop container and gives the app somewhere to live.

The specific Swing-based bootstrap in the older lesson is less important than the pattern. Desktop support in this context means: create a native host window, initialize the Codename One environment against it, forward lifecycle events properly, and package the runtime pieces the app needs. Once you see it that way, the "desktop app" stops looking like a mystery and starts looking like an ordinary host-shell problem.

This lesson is also useful because it shows how different the concerns are from normal portable application code. The portable app should not care about Swing setup, storage path heuristics, or simulator-only flags. Those belong in the desktop-specific bootstrap layer, and keeping them there preserves the clean separation between the app and the host platform.

So the main value here is architectural. A desktop wrapper is just one more example of Codename One's layering: portable app logic inside, host-specific bootstrap outside.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Running the Kitchen Sink in the Simulator](/courses/course-02-deep-dive-mobile-development-with-codename-one/034-running-the-kitchen-sink-in-the-simulator/)
- [How Do I Use Desktop Javascript Ports](/how-do-i/how-do-i-use-desktop-javascript-ports/)
