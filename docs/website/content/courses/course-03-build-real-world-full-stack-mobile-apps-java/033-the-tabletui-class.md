---
title: "The TabletUI Class"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Adapting to Tablets and Desktops"
module_key: "10-adapting-to-tablets-and-desktops"
module_order: 10
lesson_order: 3
weight: 33
is_course_lesson: true
description: "Build the persistent tablet shell that keeps navigation stable while the center content changes."
---
> Module 10: Adapting to Tablets and Desktops

{{< youtube DyzEgAGyRcA >}}

If `UIAbstraction` is the contract, `TabletUI` is the concrete shell that makes the larger-screen experience real.

The key idea is that a tablet version of the app does not need to recreate the entire screen on every navigation step. Instead, it can keep one stable outer form, leave navigation permanently available, and replace only the main content area. That makes the product feel much more natural on tablets and desktops because the app stops behaving like a sequence of full-screen phone cards.

This lesson takes that pattern seriously. The tablet shell owns the outer layout, the persistent side menu, the title treatment, and the central content area. Navigation then becomes a matter of replacing the content in that center region instead of constructing and showing a brand-new form for every move.

That is why the side menu design changes so much here. On a larger screen the menu does not need to hide behind a command. It can stay present and act like a real part of the application structure. The code also uses grouped selectable components so the current section remains visually obvious, which is exactly the kind of small polish that makes a desktop or tablet UI feel intentional.

The `showContainer()` behavior is the heart of the lesson. The first screen is added into the shell. Later screens replace the current content in the center. That is a small implementation detail with a large user-experience effect because it changes the mental model from “move to another form” to “work within one application frame.”

The video also shows some duplication between phone-specific and tablet-specific navigation code. That is acceptable here. Once the tablet experience diverges enough from the phone experience, a little explicit duplication can be cleaner than forcing both models through one overcomplicated generic API.

## Further Reading

- [Abstraction and Architecture](/courses/course-03-build-real-world-full-stack-mobile-apps-java/031-abstraction-and-architecture/)
- [The UIAbstraction Class](/courses/course-03-build-real-world-full-stack-mobile-apps-java/032-the-uiabstraction-class/)
- [Putting it all Together](/courses/course-03-build-real-world-full-stack-mobile-apps-java/034-putting-it-all-together/)
- [How Do I Create Gorgeous SideMenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
