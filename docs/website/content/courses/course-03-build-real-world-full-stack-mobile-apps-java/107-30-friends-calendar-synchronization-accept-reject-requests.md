---
title: "30. Friends - Calendar Synchronization, Accept/Reject Requests"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 30
weight: 107
is_course_lesson: true
description: "Connect the friends screen to real contact upload and request-acceptance flows so the social graph starts populating itself."
---
> Module 13: Creating a Facebook Clone

{{< youtube -aTpqxDa1Ag >}}

The friends screen becomes much more convincing once it can do something beyond rendering static suggestions. This lesson gives it that capability by wiring contact upload, friend-request actions, and notification refresh into the real backend.

The floating action button for contact upload is the most interesting UI move here. Because the friends screen lives inside a tabbed container rather than as a standalone form, adding a FAB is not just a one-line decoration. The lesson has to deal with how Codename One actually wraps containers to place a floating button above them.

That is a good example of a recurring theme in the course: when a high-level feature feels awkward, it is often because the underlying container model matters more than it first appeared. Understanding that model lets the app get the exact result it wants without resorting to hacks that would be harder to maintain later.

The contact-upload path then turns into real graph growth. By pulling contacts from the device, asking for the minimum useful fields, and sending them to the backend, the app gives the server enough information to improve suggestions and relationship discovery.

The accept/remove request flows are also properly tied to UI mutation. Once the server confirms the action, the relevant container is removed and the layout animates into place. That keeps the interface feeling responsive rather than forcing a full-screen reload after every decision.

The lesson’s mention of notifications is important too. Once friendship actions can happen for real, notification loading stops being decorative and starts representing actual state changes in the product.

## Further Reading

- [13. Friends Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/090-13-friends-container/)
- [22. UserService Part II](/courses/course-03-build-real-world-full-stack-mobile-apps-java/099-22-userservice-part-ii/)
- [14. Notifications Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/091-14-notifications-container/)
- [How Do I Use Push Notification / Send Server Push Messages](/how-do-i/how-do-i-use-push-notification-send-server-push-messages/)
