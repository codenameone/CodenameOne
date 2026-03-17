---
title: "Layout Animations"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Animations"
module_key: "11-animations"
module_order: 11
lesson_order: 2
weight: 36
is_course_lesson: true
description: "Use reflow-driven layout animation to make structural UI changes feel responsive and intentional."
---
> Module 11: Animations

{{< youtube anfVMvBXXX0 >}}

Layout animation is one of the most useful animation tools in Codename One because it works with the framework instead of fighting it. The layout manager already decides where components belong. Layout animation simply turns that change of layout into motion the user can understand.

The underlying concept is explicit reflow. When you add, remove, or rearrange components after a form is already visible, the UI does not magically update itself in a meaningful animated way. You need to trigger layout so the new positions are calculated. Once you do that, Codename One can animate the move from the old layout state to the new one.

That is why these APIs feel so practical. They are not asking you to compute every pixel of an animation by hand. They let you describe the structural change and then animate the framework’s reflow process.

The lesson distinguishes between layout and unlayout, and that distinction is worth understanding. One direction animates components into their valid final positions. The other direction is useful when you want to visually move something out before it is removed. In the dish-delete example, that creates a two-step effect: the item slides away, and then the rest of the layout closes the gap.

That pattern works well because it mirrors the user’s understanding of what happened. First the chosen item leaves. Then the remaining list reorganizes itself. Motion is carrying meaning here, not just adding polish.

The fade variants and blocking variants of the APIs are useful for the same reason. They let you compose changes in a controlled sequence instead of firing several visual updates at once and hoping the result feels natural.

The lesson is also right to be cautious about the more recursive hierarchy-wide variants. The deeper the framework has to infer for you, the more edge cases appear. Layout animation is most powerful when it is used intentionally around a specific visible change.

## Further Reading

- [Transitions](/courses/course-03-build-real-world-full-stack-mobile-apps-java/035-transitions/)
- [Animation Manager, Style Animations and Low Level Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/037-animation-manager-style-animations-and-low-level-animations/)
- [Dish List and Edit](/courses/course-03-build-real-world-full-stack-mobile-apps-java/006-dish-list-and-edit/)
- [Base Navigation Form and Shape Effects](/courses/course-03-build-real-world-full-stack-mobile-apps-java/005-base-navigation-form-and-shape-effects/)
