---
title: "The UIAbstraction Class"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Adapting to Tablets and Desktops"
module_key: "10-adapting-to-tablets-and-desktops"
module_order: 10
lesson_order: 2
weight: 32
is_course_lesson: true
description: "Use a shared UI abstraction so screens can behave like forms on phones and like content panels on tablets."
---
> Module 10: Adapting to Tablets and Desktops

{{< youtube bU6pZPs3uto >}}

Once the architectural decision is made, the next question is how to encode it in code without making every screen harder to write. The answer in this course is `UIAbstraction`, a layer that hides whether the current screen is really a form or just content living inside a larger tablet shell.

That is a strong design choice because most screens do not actually care. They want to add components, bind actions, show the next screen, validate an OK button, and go back when needed. They should not have to know whether they are running inside a phone-style navigation stack or a tablet-style container swap.

The class therefore acts as a small compatibility layer. In phone mode it delegates to a real form. In tablet mode it works with a container that can be embedded into the persistent tablet shell. The application code above it gets a stable API either way.

The OK/cancel handling is a good example. Instead of forcing every screen to decide how its action buttons should be rendered on each form factor, the abstraction turns that into a capability. If a screen says it needs OK/cancel behavior, the abstraction can implement that in the form-appropriate way for the current device.

The same idea appears in navigation, validation, and floating action button handling. The app code asks for behavior, and the abstraction adapts that behavior to the current presentation model. That keeps the screen classes readable while still allowing the larger-device UI to diverge meaningfully from the phone layout.

The video goes fairly deep into wrapper containers and decorated containers, and that is worth understanding at a conceptual level even if the exact implementation later changes. Once a screen can live in more than one kind of outer structure, it becomes useful to distinguish between the core content and the final wrapped container that includes shared controls or extra layout layers.

This kind of abstraction can become overengineered very quickly, so the best thing about the version in the course is that it stays tightly tied to a concrete product need. It is not trying to become a universal UI framework. It is solving one application's phone/tablet split in a way that keeps the rest of the code mostly unchanged.

## Further Reading

- [Abstraction and Architecture](/courses/course-03-build-real-world-full-stack-mobile-apps-java/031-abstraction-and-architecture/)
- [The TabletUI Class](/courses/course-03-build-real-world-full-stack-mobile-apps-java/033-the-tabletui-class/)
- [Putting it all Together](/courses/course-03-build-real-world-full-stack-mobile-apps-java/034-putting-it-all-together/)
- [Base Navigation Form and Shape Effects](/courses/course-03-build-real-world-full-stack-mobile-apps-java/005-base-navigation-form-and-shape-effects/)
