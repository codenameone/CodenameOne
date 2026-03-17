---
title: "7. Signup Form - Name, Birthday and Gender"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 7
weight: 84
is_course_lesson: true
description: "Build the middle signup stages and use Codename One components that adapt naturally to platform-specific input conventions."
---
> Module 13: Creating a Facebook Clone

{{< youtube Nv_0NVgCbSk >}}

These middle signup stages are where the wizard starts feeling like a real product instead of a static mockup. The screens are still simple, but they exercise a wider range of input patterns: paired text entry, date selection, and mutually exclusive choices with visual emphasis.

The name form is a good showcase for `TextComponent` and `TextModeLayout`. Together they give the app a way to describe input fields at a higher level while still letting the underlying platform shape how those fields feel. On Android, that means the material-style animated labels. On iOS, it means a more grouped presentation. The lesson uses that flexibility well instead of hard-coding one platform’s style everywhere.

The birthday form is deliberately plain, and that honesty is useful. Not every part of a product needs to become a design showcase. If the platform’s date-picking UI already solves the interaction well, it is often better to use it than to invent a more fragile custom control.

The gender step then adds a different kind of UI problem: choosing between visually equivalent options where the selection itself needs to be obvious. Radio-button semantics with stronger visual styling are a clean fit here. The lesson’s use of grouped toggle-style buttons backed by one-button selection rules is straightforward and easy to reason about.

Taken together, these screens show a good principle for signup flows: keep the structure stable, but let each input type use the most appropriate interaction model available.

## Further Reading

- [8. Signup Form - Phone, Email, Password and Confirmation](/courses/course-03-build-real-world-full-stack-mobile-apps-java/085-8-signup-form-phone-email-password-and-confirmation/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
- [Layout Basics](/courses/course-01-java-for-mobile-devices/007-layout-basics/)
- [How Do I Position Components Using Layout Managers](/how-do-i/how-do-i-positioning-components-using-layout-managers/)
