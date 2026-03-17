---
title: "1. Introduction"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 1
weight: 78
is_course_lesson: true
description: "Study the Facebook app as a reference, decide what to imitate, and define the architectural goals for the clone."
---
> Module 13: Creating a Facebook Clone

{{< youtube S5pAGkV4h4w >}}

Before writing code, this module does something the course does well when it is at its best: it studies the product being cloned instead of charging straight into implementation.

That matters because the point is not to reproduce Facebook pixel for pixel. The point is to understand which product decisions are worth copying, which ones are inconsistent or awkward, and which ones can teach useful lessons about building a large data-heavy mobile app.

The login and signup flow is a good place to start because it immediately exposes several design tensions. Deep wizard-style flows are easier to understand and adapt well to different screen sizes, but they are also more tedious when users only need to edit one thing. Flatter forms are more efficient, but they can become harder to scan and harder to adapt cleanly across phones and tablets.

That tradeoff is more valuable than the specific Facebook screens shown in the video. The lesson is really about learning how to think through UI depth, progressive disclosure, platform conventions, and the relationship between data complexity and screen structure.

The module also makes a smart scope decision. Facebook is enormous. A useful clone needs to choose which capabilities are interesting enough to implement and which ones would mostly add volume without teaching much. That kind of discipline matters in real projects too. A clone is only educational if it stays focused.

The architectural hint near the end is also important: a social app carries a huge amount of structured data, and Codename One properties are a natural fit for that. Once the app starts dealing with posts, users, friends, comments, privacy flags, and media, the benefit of declarative structured data becomes very obvious.

## Further Reading

- [2. Creating the Project and CSS](/courses/course-03-build-real-world-full-stack-mobile-apps-java/079-2-creating-the-project-and-css/)
- [Overview and Basic Model](/courses/course-02-deep-dive-mobile-development-with-codename-one/015-overview-and-basic-model/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
- [Adapting a UI Design](/courses/course-01-java-for-mobile-devices/009-adapting-a-ui-design/)
