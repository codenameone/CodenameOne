---
title: "8. Signup Form - Phone, Email, Password and Confirmation"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 8
weight: 85
is_course_lesson: true
description: "Finish the signup wizard by handling alternate contact routes, password entry, and final confirmation."
---
> Module 13: Creating a Facebook Clone

{{< youtube v-kJ_nIYq3I >}}

The final signup stages are less about new UI ideas and more about keeping the flow coherent as the user approaches completion. That makes this lesson mostly a test of whether the earlier abstractions were worth building.

The answer is yes. Phone-number and email entry are almost the same screen, and the code treats them that way without forcing them into a confusing generic abstraction. This is a good example of restraint. Similar screens can share a helper without erasing the fact that they represent slightly different decisions in the flow.

The key detail here is navigation between alternatives. The user can move from phone to email or back again without the wizard feeling like it split into two unrelated branches. That continuity matters more than the individual input fields themselves.

The password and confirmation stages are then intentionally minimal. By this point the wizard has already done most of its real work, so the final screens should feel like progress, not like the app suddenly discovered a new set of demands. The lesson handles that correctly by keeping the screens short, contextual, and clearly connected to what the user just entered.

The final wiring into the UI controller is also important. A wizard is not complete until the rest of the app can actually route into it and out of it cleanly. This lesson closes that loop so the signup flow stops being a pile of isolated forms and becomes a real branch of application navigation.

## Further Reading

- [7. Signup Form - Name, Birthday and Gender](/courses/course-03-build-real-world-full-stack-mobile-apps-java/084-7-signup-form-name-birthday-and-gender/)
- [9. The Main Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/086-9-the-main-form/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
- [Creating a Hello World App](/courses/course-01-java-for-mobile-devices/002-creating-a-hello-world-app/)
