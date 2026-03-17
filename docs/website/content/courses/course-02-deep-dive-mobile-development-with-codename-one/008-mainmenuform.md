---
title: "MainMenuForm"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Extracting a UI Design"
module_key: "03-extracting-a-ui-design"
module_order: 3
lesson_order: 5
weight: 8
is_course_lesson: true
description: "Build the main menu screen by combining reusable layout structure with styled content components."
---
> Module 3: Extracting a UI Design

{{< youtube WYlM2utu4Ps >}}

With the base form in place, the main menu screen becomes the first real test of the design system. This is where the shared shell has to support actual content: category filters, dish cards, images, buttons, and the spacing that makes the screen feel usable rather than crowded.

The category strip at the top is a good example of choosing a component for behavior rather than for visual resemblance alone. It may look like tabs in the mockup, but its job is really closer to filtering. That distinction matters because it affects how users understand the control and how the code should model its state.

The dish entries themselves are another good example of layered design work. Each card combines structure, styling, and assets. The title and descriptive text stay live so they can respond to layout, truncation, and localization. The image gets the decorative treatment it needs, including rounded shaping or masking where appropriate. The buttons inherit from the same visual language as the rest of the screen instead of being styled in isolation.

This is also where reusable component-building code starts to pay off. If every dish entry is assembled in roughly the same way, that assembly should live in one helper method or one reusable component rather than being repeated inline. The design gets more consistent, and later changes become much cheaper.

The video spends time on list behavior, selection state, and masked images. Those details are still the right things to care about because they affect how the UI feels in motion, not just how it looks in a static screenshot. A design adaptation is only successful if the interaction model feels as intentional as the visuals.

By the end of this lesson, the main screen should no longer feel like a set of disconnected mockup fragments. It should feel like one coherent form built from reusable patterns: a shared shell, a filter control with clear behavior, and content cards that combine live data with carefully chosen visual treatment.

## Further Reading

- [Layout Basics](/layout-basics/)
- [CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/006-css/)
- [How Do I Create A List Of Items The Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
- [How Do I Create Gorgeous SideMenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
