---
title: "2. Creating the Project and CSS"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 2
weight: 79
is_course_lesson: true
description: "Create the project, define the visual direction, and set up a CSS-driven styling workflow for a large data-rich app."
---
> Module 13: Creating a Facebook Clone

{{< youtube 3rLf9A9RYPY >}}

With the product direction defined, the next step is to create a project that can actually support it. This lesson is less about raw project generation and more about choosing the right styling and structural foundation for an app that will grow quickly.

The video contrasts Facebook and Uber in a useful way. One app leans into a more native platform look, the other uses a more unified cross-platform visual style. The goal of this clone is not blind accuracy. It is to take inspiration from Facebook’s flow while still making architectural choices that age well.

That is why CSS is such an important decision here. The video talks about CSS as something new and in transition, which was true at the time. Today that part is out of date. CSS is the recommended styling path for new Codename One work, and this is exactly the kind of project where it pays off. A large app with many screens, repeated patterns, and evolving visual rules is much easier to maintain when styling lives in CSS instead of in older designer-driven workflows.

The icon-font setup is also a practical choice. A social app needs many small symbolic elements, and icon fonts remain a simple way to keep those assets scalable, lightweight, and easy to recolor. The lesson’s Fontello-based workflow is one way to build that bundle of icons, but the broader idea is the important one: gather the symbols the app will need early and make them part of the styling system instead of scattering image files everywhere.

The CSS details in the lesson are the beginning of a theme system, not just a few cosmetic tweaks. Constants, default gaps, transparent backgrounds, icon fonts, and base styling decisions all become easier to manage once they are centralized. That gives the rest of the clone a cleaner starting point as the UI expands.

## Further Reading

- [1. Introduction](/courses/course-03-build-real-world-full-stack-mobile-apps-java/078-1-introduction/)
- [Working with CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
- [CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/006-css/)
- [CSS Changes](/courses/course-02-deep-dive-mobile-development-with-codename-one/013-css-changes/)
