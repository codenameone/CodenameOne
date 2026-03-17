---
title: "Creating a Hello World App"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 2
weight: 2
is_course_lesson: true
description: "Create a first Codename One application using the current Maven-based workflow."
---
> Module 1: Course Lessons

This lesson shows how to build a small but complete Codename One application: an app that starts cleanly, shows a form, responds to input, and is ready to be sent to a device once the basics are working.

{{< youtube 73d65cvyQv4 >}}

The best first project is a very small one. [Create a Maven-based Codename One application using the initializr](/initializr/), open it in your IDE, and spend a few minutes understanding the generated app before you start customizing it. The video covers the same first milestone, but this is one place where it is out of date because it starts with the old IDE plugin flow instead of the current Maven-based setup. At this stage you are not trying to design the final architecture of your product. You are trying to learn the basic rhythm of a Codename One app and confirm that your development environment is working.

One of the first things worth getting right is the package name. In Codename One it becomes part of the app's identity and eventually touches signing, native packaging, and store submission. Choose a stable reverse-domain package name at the beginning instead of treating it as a temporary placeholder.

Once the project is generated, the application class shows you the lifecycle that every Codename One app follows. `init()` is where one-time application setup belongs. `start()` is where the first UI is created and shown. `stop()` is called when the app goes into the background, and `destroy()` is there for shutdown cleanup when needed. That lifecycle is still the backbone of the framework, and understanding it early saves a lot of confusion later.

The actual hello world UI should stay simple. Create a form, add a button, attach an action listener, and show a dialog or some other small response when the user taps it. That single exercise teaches several important things at once. It shows how a form is displayed, how components are added to it, how event listeners are wired, and how the UI responds to interaction. Once that works, you have a real application, even if it is still visually plain.

Run that first version in the simulator and use it as your main feedback loop. The simulator is still the fastest place to confirm that the lifecycle is behaving correctly and that the UI looks sane across different device skins. Only after that loop feels stable should you send a native build. For most teams Android is the easiest first checkpoint. iOS usually comes later because certificates and provisioning need to be in place before the build becomes useful.

Once the app is running, the next step is usually to improve the look and feel. This is one place where the video is out of date. It moves toward the older theme designer and resource-editor workflow. For most current Codename One projects, CSS is the better default for styling and l10n property bundles are the better default for localization. The designer and resource editor still exist, but they are no longer the workflow most new projects should start with.

By the time you have a form on screen, a button responding to taps, and a simulator session you can trust, you already have the foundation you need. From there you can style the app with CSS, add localization with property bundles, and start growing the project without having to relearn the basics later.

## Further Reading

- [Getting Started](/getting-started/)
- [Hello World](/hello-world/)
- [Development Environment](/development-environment/)
- [Build Server](/build-server/)
- [Themeing](/themeing/)
- [Moving To Maven](/blog/moving-to-maven/)
