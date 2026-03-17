---
title: "37. Generic Settings using InstantUI - Automatic Dynamic UI Generation"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 37
weight: 114
is_course_lesson: true
description: "Use InstantUI and property metadata to generate a low-maintenance profile editor instead of hand-building every settings field."
---
> Module 13: Creating a Facebook Clone

{{< youtube hys6Rpkru50 >}}

Once profile editing moves beyond a couple of images, hand-authoring every settings field quickly becomes tedious. This lesson solves that by leaning on InstantUI and property metadata to generate a large part of the editable profile form automatically.

That is a strong fit for this problem. User profiles tend to grow over time, and a manually curated form can easily become a maintenance bottleneck. If the underlying property model is already expressive, it makes sense to let the UI derive more from that model instead of forcing every field through handwritten boilerplate.

The lesson is also realistic about the limits of automatic UI generation. Some values fit naturally into generated editors. Others, such as birthdays stored in a long-based transport format or constrained choice fields such as gender, need metadata or companion properties so the generated form becomes usable instead of technically correct but awkward.

That balance is exactly the right way to think about InstantUI. It is not magic. It is a way to push repetitive form construction down into the model layer while still giving the application room to guide how tricky fields should appear.

The unbind-on-exit behavior is another good reminder that generated forms still participate in normal object lifecycles. Automation reduces boilerplate, but it does not remove the need to be explicit about binding scope, saving, and cleanup.

## Further Reading

- [36. SettingsForm - Cover and Avatar](/courses/course-03-build-real-world-full-stack-mobile-apps-java/113-36-settingsform-cover-and-avatar/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [40. Edit User - UI Binding and Multipart Image Upload](/courses/course-03-build-real-world-full-stack-mobile-apps-java/077-40-edit-user-ui-binding-and-multipart-image-upload/)
