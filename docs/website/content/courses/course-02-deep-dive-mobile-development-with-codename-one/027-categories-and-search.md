---
title: "Categories and Search"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Putting it All Together"
module_key: "08-putting-it-all-together"
module_order: 8
lesson_order: 3
weight: 27
is_course_lesson: true
description: "Make the menu easier to navigate by combining category filters with responsive search."
---
> Module 8: Putting it All Together

{{< youtube GEG_S-dyxaM >}}

As the menu grows, navigation becomes as important as presentation. A visually nice screen is not enough if users cannot narrow the list to what they want quickly. That is where category filtering and search start to matter.

The search approach in the older lesson is implemented manually even though Codename One has higher-level search toolbar support. That remains a useful teaching example because it exposes the moving parts: toggling the toolbar state, swapping in a text field, listening for changes, and filtering the visible content based on the underlying model.

In a modern application, the decision between a custom search interaction and a built-in search helper should be pragmatic. If the built-in mechanism now covers the behavior you want, use it. If you need custom behavior or a design-specific interaction, implement it intentionally rather than by accident. The lesson still helps because it shows what that custom implementation really requires.

The category filter and text search are also a good reminder that UI filtering works best when the model underneath is clean. If each dish entry carries the data the filter needs, the visible UI can be rebuilt or hidden based on that data without making the search code itself complicated.

The important product lesson here is that finishing the app does not mean ignoring discoverability. Small features like search and category selection often do more for everyday usability than another round of visual polish.

## Further Reading

- [How Do I Create A List Of Items The Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
- [Developer Guide](/developer-guide/)
- [Communicating with the Server](/courses/course-02-deep-dive-mobile-development-with-codename-one/025-communicating-with-the-server/)
