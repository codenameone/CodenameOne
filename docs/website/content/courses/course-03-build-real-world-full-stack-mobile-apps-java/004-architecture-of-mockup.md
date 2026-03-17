---
title: "Architecture of Mockup"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Initial UI Mockup"
module_key: "03-initial-ui-mockup"
module_order: 3
lesson_order: 1
weight: 4
is_course_lesson: true
description: "Define the shared form structure that the app-maker mockup will build on."
---
> Module 3: Initial UI Mockup

{{< youtube vxsUf3gw1zg >}}

Once the product direction is stable enough, the next question is architectural: what parts of the UI are common, and what parts belong to individual screens? This lesson starts answering that by defining the form hierarchy for the initial app-maker mockup.

That hierarchy matters because the app maker already has a recognizable shell: a navigation structure, shared editing affordances, and a preview-oriented top-level experience. If every screen were built independently, that shell would quickly drift. A shared base form is the right tool for keeping navigation and common editing patterns consistent.

The lesson also makes a pragmatic point about reuse that is easy to miss. Copying an existing model or structure is often the right first move if it gets you to a working baseline quickly. Generalization should come from repeated need, not from the fear of duplication on day one. That is especially true in builder-style products where the real shape of the domain is still being discovered.

The result is a simple but useful structure: one shared navigation-oriented base, a small number of top-level forms that inherit from it, and one-off forms like dish editing that are allowed to stand on their own because their responsibilities are different. That split keeps the code honest about which UI concerns are shared and which are truly local.

So the architectural lesson here is not complicated, but it is important. Establish a common shell for the parts of the product that behave alike, and do not force unrelated screens into that same abstraction just to look tidy.

## Further Reading

- [Base Navigation Form and Shape Effects](/courses/course-03-build-real-world-full-stack-mobile-apps-java/005-base-navigation-form-and-shape-effects/)
- [Developer Guide](/developer-guide/)
- [Layout Basics](/layout-basics/)
