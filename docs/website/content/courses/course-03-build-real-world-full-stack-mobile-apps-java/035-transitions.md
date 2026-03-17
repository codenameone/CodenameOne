---
title: "Transitions"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Animations"
module_key: "11-animations"
module_order: 11
lesson_order: 1
weight: 35
is_course_lesson: true
description: "Use transitions to communicate navigation, emphasis, and context changes without turning the UI into a distraction."
---
> Module 11: Animations

{{< youtube aVkeOIx-Is8 >}}

Animation helps when it explains something. It hurts when it becomes decoration for its own sake. That warning at the start of the video is still the right way to approach transitions in Codename One.

The two most useful jobs for transitions are emphasis and confirmation. Sometimes you want to draw the user's eye to something important. More often, you want motion to confirm what just happened: a new screen was opened, a dialog came from outside the normal navigation stack, or a piece of content expanded into a detail view.

That is why the lesson spends more time on meaning than on visual variety. Codename One has many transition types, but the important decision is not which effect looks coolest. It is which effect best matches the user’s mental model of the action that just occurred.

The examples in the app are good ones. A cover-style transition makes sense when opening something that behaves like a layer over the current flow. A morph transition makes sense when a thumbnail turns into a full-size detail view because the user can visually track the relationship between the two states. Even the reverse direction matters. If the return motion tells a different story, a different transition may be the better choice.

The video also explains one detail that often confuses people: forms and dialogs each have transition-in and transition-out behavior, but in practice form navigation often focuses on the outgoing transition. That is less about API trivia than about keeping navigation consistent. What the user perceives is the movement between states, and consistency matters more than theoretical symmetry.

Another good pattern from the lesson is restoring the previous transition after a special-case navigation. That prevents one locally chosen effect from leaking into unrelated parts of the app. Transitions should describe the current interaction, not accidentally redefine the whole application.

## Further Reading

- [Layout Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/036-layout-animations/)
- [Animation Manager, Style Animations and Low Level Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/037-animation-manager-style-animations-and-low-level-animations/)
- [Morph Transition: Animating Elements Between Forms](/courses/course-03-build-real-world-full-stack-mobile-apps-java/038-circular-floating-action-button-animation/)
- [Bubble Border](/courses/course-03-build-real-world-full-stack-mobile-apps-java/007-bubble-border/)
