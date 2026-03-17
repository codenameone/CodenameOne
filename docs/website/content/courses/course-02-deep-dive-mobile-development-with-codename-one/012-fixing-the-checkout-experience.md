---
title: "Fixing the Checkout Experience"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "UI Design From Scratch"
module_key: "04-ui-design-from-scratch"
module_order: 4
lesson_order: 3
weight: 12
is_course_lesson: true
description: "Improve the checkout screen so users can actually edit the order they are about to place."
---
> Module 4: UI Design From Scratch

{{< youtube CvbcWC4hLYk >}}

A checkout screen is not finished just because it looks good. If users cannot understand what they ordered, change quantities easily, or remove mistakes without friction, the design has failed at the point where clarity matters most.

That is the problem this lesson addresses. The earlier checkout design was visually appealing, but it did not give the user a practical way to change the order. That kind of gap is common in mockups. A screen is designed for presentation, then later you discover that a real product still needs editing, correction, and confirmation behaviors that the design never accounted for.

There are several possible fixes. Swipe gestures can keep the surface clean, but they are easy to miss. An edit mode can work, but it adds state and often makes the flow feel heavier than necessary. Always-visible controls take more space, but they are honest about what the user can do.

The solution in the lesson is a good practical compromise: make the quantity directly editable through a compact control that fits the existing visual language, and use a picker-based interaction to avoid keyboard friction. That is an especially sensible choice on mobile because it reduces validation problems, keeps the interaction predictable, and makes deletion just another quantity change to zero rather than a separate destructive action.

What matters most here is not the exact widget choice. It is the principle that checkout should support correction without making the user search for hidden functionality. At this stage of the flow, discoverability and confidence matter more than keeping the layout pristine.

So if a checkout UI feels elegant but does not let the user edit the order naturally, fix the behavior first and let the visual solution follow that need. Real product quality comes from that willingness to adjust the design once real interaction requirements become obvious.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Create A List Of Items The Easy Way](/how-do-i/how-do-i-create-a-list-of-items-the-easy-way/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
