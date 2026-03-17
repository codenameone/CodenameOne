---
title: "15. The More Container"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 15
weight: 92
is_course_lesson: true
description: "Build the overflow area of the app and turn miscellaneous functionality into a coherent menu rather than a dumping ground."
---
> Module 13: Creating a Facebook Clone

{{< youtube kfKsVAx659s >}}

The “more” tab is where large apps often reveal whether they still have any structural discipline. It can either become a dumping ground for leftovers or a clean overflow area for features that do not deserve top-level navigation.

This lesson takes the second path. The screen is intentionally simple, but it still treats the section as a real part of the app by giving entries consistent icon treatment, spacing, and text hierarchy. That is enough to make the area feel deliberate even before most of its destinations are implemented.

The use of `MultiButton` is a good fit here. These entries are really structured menu rows: primary label, optional secondary text, icon treatment, and clear tap behavior. A higher-level component makes more sense than rebuilding that row structure manually for every entry.

The lesson also shows a healthy willingness to handle a small styling gap in code when the CSS support of the time was not sufficient. That part is historical, but the underlying principle still holds: use CSS as the default, and fill the rare gaps pragmatically instead of distorting the whole design to avoid one line of code.

What matters most is that the app now has a place for user-adjacent functions such as settings without forcing them into the main feed tabs. That keeps the primary navigation focused while still leaving room for the app to grow.

## Further Reading

- [39. Settings Form and Fetching the Avatar Image](/courses/course-03-build-real-world-full-stack-mobile-apps-java/076-39-settings-form-and-fetching-the-avatar-image/)
- [9. The Main Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/086-9-the-main-form/)
- [How Do I Create Gorgeous SideMenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
- [Working with CSS](/courses/course-02-deep-dive-mobile-development-with-codename-one/001-working-with-css/)
