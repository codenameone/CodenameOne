---
title: "Markers, Lightweight Overlays and Map Layout"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Maps"
module_key: "10-maps"
module_order: 10
lesson_order: 3
weight: 32
is_course_lesson: true
description: "Go beyond built-in markers by placing real lightweight components over the map."
---
> Module 10: Maps

{{< youtube sed5OPQSfe0 >}}

Markers are the first useful map feature because they let the user connect geographic coordinates to something meaningful. But the default marker API is only the beginning. As soon as you want richer visuals or interactions, you start wanting more control than a simple pin can provide.

That is where overlays become interesting. Instead of treating the map as a sealed native surface that can only show provider-defined markers, this lesson shows how to place lightweight components above the map and position them according to latitude and longitude. That opens up much richer UI possibilities: custom buttons, status badges, composite visuals, or other interactive elements that feel like part of the app instead of part of the map provider.

The layout-manager approach used here is especially instructive because it keeps the problem honest. A map overlay is still a layout problem. You need to convert coordinates into screen points, place components using their preferred size, and update their positions when the map moves or zooms. Once you describe it that way, the solution becomes much less magical.

This is also a good example of how Codename One's lightweight UI model enables features that would once have been awkward or impossible. If heavyweight native components always won the z-order battle, this pattern would be much harder to support. The ability to mix map content with lightweight overlays is a real design advantage.

So the lesson here is that built-in markers are fine when they are enough, but do not stop there if your map UI needs richer interaction. A carefully designed overlay layer can turn the map from a passive display into a real part of the application interface.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Introduction and Installation](/courses/course-02-deep-dive-mobile-development-with-codename-one/030-introduction-and-installation/)
- [Hello World and Devices](/courses/course-02-deep-dive-mobile-development-with-codename-one/031-hello-world-and-devices/)
