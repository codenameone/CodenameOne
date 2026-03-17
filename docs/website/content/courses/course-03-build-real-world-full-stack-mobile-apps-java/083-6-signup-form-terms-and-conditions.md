---
title: "6. Signup Form - Terms and Conditions"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 6
weight: 83
is_course_lesson: true
description: "Use the shared signup form shell to build the first step of the wizard and establish the visual rules for the rest of the flow."
---
> Module 13: Creating a Facebook Clone

{{< youtube xLa5yeeVsE8 >}}

The first real signup screen is intentionally simple, and that is exactly why it matters. This is where the reusable signup shell proves itself and where the rest of the wizard’s visual language starts becoming concrete.

The form itself is mostly content and framing: a clear title, explanatory text, a prominent next action, and supporting links below. Because the structure was abstracted in the previous lesson, the screen-specific code can stay focused on meaning rather than layout plumbing.

The rich-text component earns its place here by rendering highlighted legal and explanatory copy without dragging in a heavier HTML solution. That makes this lesson a good checkpoint for the earlier architectural decision. The app now has a custom text component that feels native to the design rather than bolted on from outside.

The CSS additions are also important because they define the vocabulary for the rest of the signup wizard: toolbar spacing, title sizing, back-command behavior, off-white backgrounds, and the visual identity of the “next” action. Once those rules exist, later stages can reuse them instead of negotiating their visual hierarchy from scratch.

This is one of those lessons where the code is not flashy, but the payoff is large. A clean first step in a wizard sets expectations for every step that follows.

## Further Reading

- [5. Rich Text View and Signup Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/082-5-rich-text-view-and-signup-form/)
- [7. Signup Form - Name, Birthday and Gender](/courses/course-03-build-real-world-full-stack-mobile-apps-java/084-7-signup-form-name-birthday-and-gender/)
- [Working with CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
- [CSS Changes](/courses/course-02-deep-dive-mobile-development-with-codename-one/013-css-changes/)
