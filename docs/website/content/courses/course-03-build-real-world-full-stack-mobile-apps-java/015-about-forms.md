---
title: "About Forms"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Miscellaneous Features"
module_key: "06-miscellaneous-features"
module_order: 6
lesson_order: 4
weight: 15
is_course_lesson: true
description: "Handle about-page content in both the builder and the generated app without overcomplicating the workflow."
---
> Module 6: Miscellaneous Features

{{< youtube a7MF3oSCASE >}}

About pages are easy to underestimate because they are rarely the headline feature of an app. In practice they matter a lot. They give the product a place for identity, contact information, explanatory content, and other material that should exist without cluttering the main workflow.

This lesson is dealing with two related but different problems. The builder itself needs an about experience, and the generated restaurant app needs a configurable about destination of its own. Those are not the same concern, and treating them separately is the right design move.

For the builder's own about page, HTML is a reasonable tool because the content is mostly informational and static. That is one of the few areas where a browser-based presentation can be more convenient than building everything out of native-looking components. The older lesson says this directly, and that judgment still makes sense.

For the generated app, the choice to rely on a URL instead of embedding arbitrary HTML is also pragmatic. It keeps the app-maker workflow simpler and assumes something that is usually true in the real world: a restaurant likely already has a site or a page that can serve as the deeper “about” destination. That is often better than turning the builder into a full CMS just to support one screen.

The validation portion of the lesson is also important. If the builder is going to save and preview an about destination, it should not accept obviously broken values and hope the user notices later. Lightweight validation at the point of edit is part of making the builder feel trustworthy.

So the real lesson here is not “how to show HTML.” It is how to give both the editor and the generated app an about experience that fits their purpose without dragging the product into unnecessary complexity.

## Further Reading

- [Sidemenu and Preview](/Users/shai/dev/cn1/docs/website/content/courses/course-03-build-real-world-full-stack-mobile-apps-java/014-sidemenu-and-preview.md)
- [Details, Categories and Validation](/Users/shai/dev/cn1/docs/website/content/courses/course-03-build-real-world-full-stack-mobile-apps-java/012-details-categories-and-validation.md)
- [Developer Guide](/Users/shai/dev/cn1/docs/website/content/developer-guide.md)
