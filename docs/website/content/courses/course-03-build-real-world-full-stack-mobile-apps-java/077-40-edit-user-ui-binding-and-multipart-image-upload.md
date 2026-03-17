---
title: "40. Edit User - UI Binding and Multipart Image Upload"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating an Uber Clone"
module_key: "12-creating-an-uber-clone"
module_order: 12
lesson_order: 40
weight: 77
is_course_lesson: true
description: "Bind profile fields directly to the user model and upload avatar images as multipart requests when the account changes."
---
> Module 12: Creating an Uber Clone

{{< youtube S1SHZLXQxSI >}}

Editing account details is one of those places where a lot of apps make the user work harder than necessary. The lesson starts by calling out Uber’s more segmented editing flow and then chooses a flatter, more direct alternative: bind text fields straight to the user object and save only when the user leaves the form.

That is a good tradeoff for this app. The screen stays simple, the code stays short, and the user can edit several values naturally instead of drilling through one tiny form at a time.

The binding API does most of the heavy lifting here. Once the user object is bound to the text fields, the form can populate itself from the model and keep the model updated as the user types. That is exactly the kind of repetitive plumbing worth automating.

The lesson is also careful about when data is actually sent to the server. Binding the UI to the model does not mean every keystroke should become a network request. Saving on exit keeps the experience responsive and avoids unnecessary traffic. The comparison against the original object state is a simple but effective way to decide whether anything changed.

The avatar upload flow extends the same idea. The app lets the user capture a new image, resizes it on the client so the upload stays within sensible limits, updates the local UI immediately, and then sends the file to the server using a multipart request. That sequencing feels good because the user sees the result right away even while the server-side update is still in flight.

The warning about unbinding is also important. Once bindings are attached to a global model object, forgetting to release them can keep entire form hierarchies alive longer than intended. This is exactly the kind of subtle memory problem that is easy to create and hard to notice unless you think about object lifetimes deliberately.

## Further Reading

- [39. Settings Form and Fetching the Avatar Image](/courses/course-03-build-real-world-full-stack-mobile-apps-java/076-39-settings-form-and-fetching-the-avatar-image/)
- [Properties Are Amazing](/blog/properties-are-amazing/)
- [Storage, Filesystem and SQL](/courses/course-01-java-for-mobile-devices/010-storage-filesystem-and-sql/)
- [How Do I Take a Picture with the Camera](/how-do-i/how-do-i-take-a-picture-with-the-camera/)
