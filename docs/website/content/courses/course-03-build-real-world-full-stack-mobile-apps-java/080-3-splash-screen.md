---
title: "3. Splash Screen"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 3
weight: 80
is_course_lesson: true
description: "Introduce a splash screen and a simple UI controller so the app has a clean first-launch experience."
---
> Module 13: Creating a Facebook Clone

{{< youtube Rat-FDqDCTU >}}

Splash screens are easy to overthink. The point is not to hide all startup work behind branding. The point is to make the first moment of the app feel intentional instead of abrupt.

This lesson uses a splash screen for a practical reason. The Facebook-style login experience looks very different from the rest of the app, so dropping the user immediately into an unrelated partially loaded screen would feel messy. A simple transition screen gives the app a more coherent starting point.

The old video spends time on iOS screenshot generation and older native splash mechanics. That part is historical now and should not be treated as the main lesson. The lasting idea is that startup needs a dedicated entry point. Instead of letting the default generated form dictate the experience, the app introduces a UI controller that owns the initial navigation flow.

That controller is the more important architectural move. Once the app has a single place that decides whether to show the splash screen, login flow, or some later authenticated screen, startup logic stops being scattered across individual forms.

The morph from the splash logo into the next screen’s logo is also a good example of animation serving a real purpose. It visually connects the temporary startup state to the application state that follows it, which makes the change feel continuous rather than arbitrary.

## Further Reading

- [2. Creating the Project and CSS](/courses/course-03-build-real-world-full-stack-mobile-apps-java/079-2-creating-the-project-and-css/)
- [Transitions](/courses/course-03-build-real-world-full-stack-mobile-apps-java/035-transitions/)
- [Animation Manager, Style Animations and Low Level Animations](/courses/course-03-build-real-world-full-stack-mobile-apps-java/037-animation-manager-style-animations-and-low-level-animations/)
- [Creating a Hello World App](/courses/course-01-java-for-mobile-devices/002-creating-a-hello-world-app/)
