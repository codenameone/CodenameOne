---
title: "Putting it all Together"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Adapting to Tablets and Desktops"
module_key: "10-adapting-to-tablets-and-desktops"
module_order: 10
lesson_order: 4
weight: 34
is_course_lesson: true
description: "Integrate the abstraction layer into the real screens so the app adapts cleanly across form factors."
---
> Module 10: Adapting to Tablets and Desktops

{{< youtube AMlnslUn1bA >}}

The value of an abstraction is not that it looks clever in isolation. It is that real screens become easier to adapt. This final lesson in the module is where that claim gets tested.

The encouraging thing in the original walkthrough is how little many screens actually need to change once the abstraction layer exists. Base navigation screens can move from `Form` to `UIAbstraction` with relatively small edits, and more specialized screens can opt into the specific behaviors they need such as OK/cancel handling or form-specific theming.

That is exactly what good application architecture should do. It should move the difficult decision to one place and reduce the amount of code that needs to know about it everywhere else.

The address form example in the video shows this clearly. Validation still happens. The submission flow still happens. Back and OK actions still happen. What changes is where those controls come from and how they are rendered on different form factors. The business logic is not rewritten just because the app now has a larger-screen presentation.

The lesson also makes a fair point about framework design. It would be tempting to say that this whole abstraction should just be part of Codename One itself. The reason not to rush that is that framework APIs are expensive to commit to. A product-specific abstraction can be tuned aggressively for one app. A general-purpose framework abstraction has to survive many different apps and many years of backwards compatibility.

So the right takeaway here is not that every app needs exactly this abstraction layer. It is that tablet and desktop adaptation becomes much more manageable when you first identify which parts of your UI are truly form-factor-specific and then isolate them behind a stable application-level API.

## Further Reading

- [Abstraction and Architecture](/courses/course-03-build-real-world-full-stack-mobile-apps-java/031-abstraction-and-architecture/)
- [The UIAbstraction Class](/courses/course-03-build-real-world-full-stack-mobile-apps-java/032-the-uiabstraction-class/)
- [The TabletUI Class](/courses/course-03-build-real-world-full-stack-mobile-apps-java/033-the-tabletui-class/)
- [Adapting a UI Design](/courses/course-01-java-for-mobile-devices/009-adapting-a-ui-design/)
