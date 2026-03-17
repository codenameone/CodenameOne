---
title: "Dish List and Edit"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Initial UI Mockup"
module_key: "03-initial-ui-mockup"
module_order: 3
lesson_order: 3
weight: 6
is_course_lesson: true
description: "Build the core dish-management flow of the app maker and make editing feel immediate."
---
> Module 3: Initial UI Mockup

{{< youtube yv3WXt8o88k >}}

The dish list is where the app maker stops feeling like a shell and starts feeling like a real editor. The user is no longer just changing a title or background. They are managing a collection of product content that will define the generated app.

The most important decision in this lesson is the editing flow. Instead of presenting a separate “create dish” ceremony and then making the user confirm every step, the app immediately creates an entry and opens it for editing. That is a very mobile-friendly choice. It keeps momentum high and avoids forcing a cumbersome OK/cancel workflow where deletion is often the simpler and clearer form of undo.

The grid-based presentation is also a good product choice because dishes are fundamentally visual. People think about menu items through images and headlines, not just through rows of text. The list screen should therefore behave more like a content board than a data table.

On the editing side, the lesson continues the preview-first design language established earlier. The title is editable in place, the image remains a dominant part of the form, and the editing controls are arranged to support that rather than overwhelm it. The floating action button placement, background treatment, and bottom-positioned delete action all serve that same goal: keep the main content visible while still making the editing affordances clear.

This is also a good example of where layout choices matter more than decorative polish. Wrapping controls correctly, avoiding awkward line breaks in editable title areas, and positioning actions relative to the title region are what make the form feel intentional. Once the structure is right, the styling has something solid to work with.

So this lesson is really the first end-to-end content-management interaction in the app maker. It shows how to let users create, inspect, and refine one of the core artifacts of the generated app without turning the editing experience into a wall of forms.

## Further Reading

- [Base Navigation Form and Shape Effects](/courses/course-03-build-real-world-full-stack-mobile-apps-java/005-base-navigation-form-and-shape-effects/)
- [How Do I Create A List Of Items The Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
- [Layout Basics](/layout-basics/)
