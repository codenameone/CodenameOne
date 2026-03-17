---
title: "Abstraction and Architecture"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Adapting to Tablets and Desktops"
module_key: "10-adapting-to-tablets-and-desktops"
module_order: 10
lesson_order: 1
weight: 31
is_course_lesson: true
description: "Refactor the app so tablet and desktop layouts feel intentional instead of like oversized phone screens."
---
> Module 10: Adapting to Tablets and Desktops

{{< youtube Dh1bYqiKBQo >}}

Running a phone UI on a tablet is easy. Making it feel like it belongs there is not. That is the problem this module solves.

The lesson begins with the right observation: a phone-oriented app often looks merely enlarged on a tablet. Images feel oversized, empty space is wasted, navigation patterns become awkward, and the whole application looks like it was stretched rather than designed.

The fix is not a pile of tablet-specific special cases. The fix is to step back and change the abstraction. On a phone, it is natural for each major screen to be its own form. On a tablet or desktop, that often becomes clumsy. A better pattern is to keep one stable outer shell and replace the content inside it as the user moves through the app.

That architectural decision is the heart of the module. Instead of deriving everything directly from `Form`, the lesson introduces a higher-level UI abstraction that can behave like a normal form on phones while participating in a single-shell layout on larger displays. That lets the same application logic support very different presentation patterns without duplicating the whole app.

The OK/cancel discussion in the video is a good example of why this matters. On a phone, toolbar commands may make sense. On a tablet, large action buttons placed naturally in the layout can feel much better. Once that choice is expressed in the abstraction layer, individual screens stop caring about which form factor they are on and can focus on their own job.

This is the real benefit of the module. It is not just about making one screen wider. It is about moving form-factor differences into an architectural seam where they can be controlled deliberately.

## Further Reading

- [The UIAbstraction Class](/courses/course-03-build-real-world-full-stack-mobile-apps-java/032-the-uiabstraction-class/)
- [The TabletUI Class](/courses/course-03-build-real-world-full-stack-mobile-apps-java/033-the-tabletui-class/)
- [Putting it all Together](/courses/course-03-build-real-world-full-stack-mobile-apps-java/034-putting-it-all-together/)
- [Adapting a UI Design](/courses/course-01-java-for-mobile-devices/009-adapting-a-ui-design/)
