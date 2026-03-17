---
title: "Introduction"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 1
weight: 1
is_course_lesson: true
description: "An introduction to the course and the modern Codename One workflow."
---

> Module 1: Course Lessons

This course is about learning how mobile applications are built in Codename One without treating the framework as magic. The goal is not just to get an app on screen. It is to understand what mobile development asks of you, how Codename One fits into that world, and how to build habits that will still make sense once the project grows beyond a toy example.

If you are starting fresh today, the right place to begin is with the current Maven-based workflow. Create projects with [Initializr](/initializr/), open them in the IDE you already use, and rely on the generated project structure instead of the legacy plugin-based setup shown in some of the older course material. That modern setup is simpler, easier to version, and much closer to the way current Codename One projects are maintained.

The early lessons in this course focus on the fundamentals that keep coming back throughout real projects: how mobile devices differ from desktop environments, how a Codename One application is structured, how layouts and styling work, why the event dispatch thread matters, and how device builds fit into the development loop. Those are the things that make the rest of the framework easier to understand.

You should expect some of the videos in this course to show older tooling. When that happens, use the written lesson as the current source of truth. The important concepts are still worth learning, but the practical workflow has evolved. In particular, modern projects usually start with Maven, use CSS as the primary styling workflow, and use l10n property bundles for localization instead of treating the designer and resource editor as the center of the project.

The best way to use this course is to keep a project open while you read. Run the simulator often, change one thing at a time, and make sure you understand why the framework behaves the way it does before moving on. That feedback loop is what turns the lessons into working knowledge.

## Further Reading

- [Getting Started](/getting-started/)
- [Initializr](/initializr/)
- [Developer Guide](/developer-guide/)
- [Hello World](/hello-world/)
