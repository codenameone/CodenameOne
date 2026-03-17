---
title: "38. Circular Floating Action Button Animation"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 38
weight: 75
is_course_lesson: true
description: "Build a lightweight progress animation around a floating action button without turning it into a generic loading spinner."
---
> Module 12: Creating an Uber Clone

{{< youtube YE7OQFQE0yA >}}

Some animations are worth keeping product-specific. This lesson is a good example. The circular highlight around the floating action button is not a general-purpose UI pattern you would add to every app. It is a small piece of branding and feedback that fits the ride-hailing experience the course is cloning.

The useful idea here is not really the Uber imitation itself. It is the decision to build a focused animation for one concrete interaction instead of trying to solve “all progress indication” with one giant generic component.

The implementation works by styling the floating action button directly and animating the stroke of its round border. That keeps the effect local. The button keeps its identity, but it gains a sense of motion and ongoing work while the app is waiting for the next step.

The lesson also uses a UI timer rather than a plain background timer. That is the right choice for a visual effect that updates component styles because the callbacks need to run on the EDT. A progress effect that quietly hops across threads is exactly the kind of thing that becomes glitchy for no good reason.

Another good detail is the cleanup step. The animation stores its instance in a client property and restores the original UIID when it stops. That matters because a temporary visual effect should leave the component exactly as it found it. If an animation modifies style state but never restores it properly, the UI gradually becomes harder to reason about.

This lesson sits in a good middle ground between high-level animation APIs and fully custom rendering. It is custom enough to feel specific to the product, but still simple enough that the code stays understandable.

## Further Reading

- [Transitions](/courses/course-03-build-real-world-full-stack-mobile-apps-java/035-transitions/)
- [Layout Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/036-layout-animations/)
- [Animation Manager, Style Animations and Low Level Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/037-animation-manager-style-animations-and-low-level-animations/)
- [Enter Password Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/072-35-google-login-process/)
