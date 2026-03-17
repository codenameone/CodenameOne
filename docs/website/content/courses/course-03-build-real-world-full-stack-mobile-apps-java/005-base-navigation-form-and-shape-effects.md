---
title: "Base Navigation Form and Shape Effects"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Initial UI Mockup"
module_key: "03-initial-ui-mockup"
module_order: 3
lesson_order: 2
weight: 5
is_course_lesson: true
description: "Build the reusable navigation shell and the image treatments that define the app-maker’s visual identity."
---
> Module 3: Initial UI Mockup

{{< youtube xzwq4P9ogoU >}}

This lesson is where the app-maker mockup starts becoming visually specific. The shared navigation shell is not just a place to hang menus. It is also where the product gets much of its identity: editable title areas, branded imagery, and the small visual treatments that make the preview feel like an actual product instead of a form editor.

One of the strongest ideas here is the use of editable text fields styled to feel like normal title elements. That approach matches the product well. The app maker is about live customization, so turning parts of the preview itself into editable elements makes the interface more direct and more understandable.

The rounded-logo discussion is also useful because it shows how shape work should be approached in Codename One. The important point is not the specific masking trick alone. It is the broader principle that visual effects need to be implemented in a way that stays portable and predictable across platforms. A trick that looks elegant but behaves differently from target to target is not actually helping the design.

This lesson predates the stronger CSS-first emphasis that now makes sense for most styling work, but the underlying visual decisions still hold up. Use CSS and current theming practices for the bulk of the style system, and reserve lower-level image or mask work for the cases where the design truly needs it, such as logo shaping or image treatment that cannot be expressed cleanly through normal styling.

So the navigation form in this lesson is important for two reasons. It gives the app maker a consistent structural shell, and it establishes the visual language that later editable screens will build on top of.

## Further Reading

- [Themeing](/themeing/)
- [Working With CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
- [Architecture of Mockup](/courses/course-03-build-real-world-full-stack-mobile-apps-java/004-architecture-of-mockup/)
