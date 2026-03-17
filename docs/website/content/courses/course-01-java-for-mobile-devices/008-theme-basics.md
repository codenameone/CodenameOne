---
title: "Theme Basics"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 8
weight: 8
is_course_lesson: true
description: "Understand theme concepts while treating CSS as the default styling path for modern projects."
---
> Module 1: Course Lessons

{{< youtube cxllJwt10VU >}}

Styling in Codename One starts with a simple question: are you trying to change how components look, or are you trying to change how the UI behaves structurally? If the answer is visual styling, the modern default is CSS. The video uses the older designer-centered theme workflow, and that still helps explain the underlying concepts, but for a new project you should usually start with CSS and treat the older theme editor as a lower-level tool rather than the main path.

The concepts behind theming are still the same. Components get their appearance from a UIID. A UIID is effectively the name of the style a component uses. If you create a button and give it a custom UIID, that UIID becomes the hook for styling the button consistently. This is true whether the style is defined in CSS or in the older theme resource workflow.

The simplest useful example is a custom button. Give it a background color, a foreground color, some padding, and a readable font size. The point is not to make it beautiful on the first pass. The point is to understand which properties actually shape the component. Background and foreground colors control the obvious look, but spacing is just as important. Padding affects the space inside the component and therefore the touchable area. Margin affects the space outside the component and therefore the relationship between neighboring components.

State-specific styling is one of the first places where theming becomes real. A button usually needs at least an unselected appearance and a pressed or selected appearance. If the normal state has one background and the pressed state has no explicit styling, the result can feel inconsistent or broken. The video demonstrates this with borders and selected styles, and the underlying lesson remains important: style all of the states that matter, not just the first one you see in the simulator.

Borders are another common source of confusion. In the older theme editor, borders often take precedence over background settings, which is why a component can look correct on one platform but unexpectedly wrong on another. The same general rule applies conceptually even when you work in CSS: understand which visual property is actually winning.

The other important concept from the lesson is inheritance. A custom style does not need to redefine everything from scratch. It is usually better to start from a base component style and override only what you actually want to change. That keeps your styles smaller and more maintainable.

Theme constants and other lower-level theme settings still matter, especially when you are controlling broader application behavior or integrating with older theme-based assets. But for a new project, the practical approach is simpler than the older lesson suggests: use layouts to define structure, use CSS to define appearance, and only drop to the older theme tooling when you genuinely need the lower-level control.

## Further Reading

- [Themeing](/themeing/)
- [Developer Guide](/developer-guide/)
- [How Do I Create A Simple Theme](/how-do-i/how-do-i-create-a-simple-theme/)
- [Work With Multi Images And Device Densities](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)
