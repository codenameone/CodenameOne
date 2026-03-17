---
title: "List, Network, Parsing and Resource File Size"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Performance and Memory Tuning"
module_key: "12-performance-and-memory-tuning"
module_order: 12
lesson_order: 4
weight: 41
is_course_lesson: true
description: "Recognize the common structural performance traps in lists, parsing, and oversized resources."
---
> Module 12: Performance and Memory Tuning

{{< youtube ZEe_hb1Lz6Y >}}

Performance problems do not always come from one obviously slow algorithm. Just as often they come from structural choices that look harmless until the app scales a little. Lists, parsing location, overdraw, network threading, and resource-file bloat all fall into that category.

The first useful point in this lesson is that `List` is not automatically the fast answer just because it has a reusable-cell architecture. On mobile, huge endlessly scrollable datasets are often the wrong interaction model in the first place, and smaller data sets can frequently be handled more cleanly with other UI structures. Performance is one reason, but usability is another.

Overdraw is another example of a problem that is easy to ignore until it starts hurting. Drawing the same region several times is normal to a point, but too much layered opacity and background painting can create unnecessary rendering cost. That is why inspecting component hierarchy and reducing unnecessary opaque layers can matter so much.

The lesson is also right to push parsing and similar work off the EDT when possible. If a network response arrives quickly but the JSON parsing happens on the EDT, the user still experiences a slow app. Getting the work off the UI thread does not just improve raw performance. It protects the user's sense that the app is alive.

Resource-file size deserves equal attention because large resources hurt more than one stage of the product. They make downloads heavier, builds larger, memory use worse, and startup or asset loading less predictable. In practice, most of that size is usually images, especially multiple density variants. So the most effective optimization is often to identify the biggest offenders rather than trying to shave a few bytes from everything.

This lesson is really a collection of reminders that performance is often about architecture and asset discipline, not just low-level code tricks. If the lists are appropriate, the parsing happens in the right place, and the resources stay under control, many other problems become much easier to manage.

## Further Reading

- [What is Performance? Breaking Down the Problem](/courses/course-02-deep-dive-mobile-development-with-codename-one/038-what-is-performance-breaking-down-the-problem/)
- [How Do I Create A List Of Items The Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
- [How Do I Fetch An Image From The Resource File, Add A Multiimage](/how-do-i/how-do-i-fetch-an-image-from-the-resource-file-add-a-multiimage/)
