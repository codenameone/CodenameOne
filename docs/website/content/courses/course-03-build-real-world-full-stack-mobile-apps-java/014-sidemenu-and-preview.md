---
title: "Sidemenu and Preview"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Miscellaneous Features"
module_key: "06-miscellaneous-features"
module_order: 6
lesson_order: 3
weight: 14
is_course_lesson: true
description: "Polish the app-maker shell and make preview a first-class part of the editing experience."
---
> Module 6: Miscellaneous Features

{{< youtube jhPep9vHv_U >}}

Preview is one of the most important features in a builder product because it turns editing from abstract configuration into immediate feedback. This lesson finally gives that idea the implementation attention it deserves.

The side menu matters here because it helps the builder feel like a complete tool rather than a pile of forms. Small visual decisions such as padding, selection styling, and header content do more than improve aesthetics. They make the product feel stable and intentional while users move between editing contexts.

The preview path is even more important. The clever part is not that a preview exists. It is that the system reuses the actual restaurant app code instead of inventing a fake representation of what the generated app might look like. That is a strong product choice because the preview is more trustworthy when it is built from the same implementation path as the real result.

The lesson openly takes the pragmatic route by copying the restaurant app sources instead of prematurely generalizing everything into a shared framework. That is consistent with the rest of the course and still the right instinct here. If the preview proves valuable and the duplication becomes painful, that is the right moment to refactor. Not before.

So the key lesson is that preview should be treated as a product feature, not as a nice extra. The more directly it reflects the actual generated app, the more useful it becomes to the user and to the developer.

## Further Reading

- [Scope and Basic UI Design](/courses/course-03-build-real-world-full-stack-mobile-apps-java/002-scope-and-basic-ui-design/)
- [Themeing](/themeing/)
- [How Do I Create Gorgeous SideMenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
