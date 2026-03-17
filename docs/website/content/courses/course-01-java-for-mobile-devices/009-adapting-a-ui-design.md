---
title: "Adapting a UI Design"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 9
weight: 9
is_course_lesson: true
description: "Turn a visual design into a practical Codename One UI using layouts, CSS, and carefully chosen assets."
---
> Module 1: Course Lessons

{{< youtube WT4cFceBWRA >}}

Adapting a UI design is never just a matter of copying pixels from a mockup into code. A design file shows you the visual intention. Your job is to translate that intention into a layout that survives different screen sizes, densities, font rendering differences, and real user interaction.

The first step is to break the design into categories instead of trying to build the whole screen at once. Some parts should stay as live UI: text, buttons, lists, search fields, toolbars, and anything the user interacts with. Some parts may need image assets: photographs, illustrations, decorative textures, or highly specific shapes that are not worth recreating in code. Once you make that distinction, the design becomes much easier to implement.

The video uses a Photoshop-based workflow and cuts image assets out of a PSD. That basic thinking is still useful, but the tooling has moved on. Today the same exercise might start from Figma, Sketch, or exported design assets rather than Photoshop, and the resulting Codename One implementation should usually be styled with CSS rather than centered around the older designer workflow.

When you translate the design into UI, start with layout before styling. Build the screen structure using forms, containers, and layouts that express the visual hierarchy. Decide which areas belong in the toolbar, which belong in the content pane, and which elements need to float over the content. Once the structure is correct, styling becomes much easier because you are polishing a stable layout instead of trying to patch a brittle one.

This is also where you should resist the temptation to chase pixel perfection too early. A design mockup is often drawn for one screen size with one font rasterizer and one set of exact asset dimensions. A real mobile UI has to survive much more than that. It is usually better to match the intent and rhythm of the design than to overfit to one screenshot and end up with something that breaks on the next device.

Assets still matter, but they should be chosen carefully. If a shape can be expressed with styling, borders, padding, and standard components, that is usually preferable to shipping another image. If an icon can come from a material icon font, that is usually preferable to exporting several bitmap versions. Use raster assets where they provide something unique, not as a substitute for understanding layout and styling.

Floating action buttons, layered elements, and custom visual accents are good examples of where the implementation needs to understand the framework rather than just the mockup. A floating button is not simply drawn in one static position. It needs to live in the right layer and respond properly as the form size changes. A highlighted row or decorative border may be better represented by a border image, a styled background, or a reusable UIID depending on how the screen behaves.

The best way to adapt a design in Codename One today is to treat the mockup as a guide, recreate the structure with layouts, use CSS to style the resulting components, and only then introduce image assets where they genuinely improve the result. That approach scales much better than trying to paint the entire screen out of cut-up images.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Themeing](/themeing/)
- [Layout Basics](/layout-basics/)
- [How Do I Create A 9 Piece Image Border](/how-do-i/how-do-i-create-a-9-piece-image-border/)
- [How Do I Create Gorgeous SideMenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
