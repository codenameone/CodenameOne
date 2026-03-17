---
title: "9. The Main Form"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 9
weight: 86
is_course_lesson: true
description: "Create the tabbed shell of the Facebook clone and define the four major areas the rest of the app will build on."
---
> Module 13: Creating a Facebook Clone

{{< youtube q4YjUClRHBk >}}

Once the signup flow is in place, the app finally needs a home. This lesson builds that shell: the tabbed main form that holds the news feed, friends, notifications, and “more” sections together.

That shell is more important than it first appears. A social app is mostly movement between adjacent sections of one large experience. If the outer navigation structure feels wrong, the whole app feels fragmented no matter how good the individual screens are.

The tab design in the lesson takes platform differences seriously. Tabs belong in different places on different platforms, and Codename One makes it possible to respect that expectation without duplicating the whole app structure. That is exactly the kind of platform-aware decision that improves a clone without making the code unmanageable.

The lesson also makes a useful styling decision by turning the search affordance into something that looks like a field while still behaving like a navigation trigger. That matches the product’s intent: the main form is not trying to host a full search experience yet, but it still needs to make search feel present and ready.

At this stage, the individual containers behind the tabs are still mostly placeholders, and that is fine. The job of this lesson is to define the application frame and make it possible for the later feature work to land in the right places.

## Further Reading

- [10. Client Data Model - User, Post and Comment](/courses/course-03-build-real-world-full-stack-mobile-apps-java/087-10-client-data-model-user-post-and-comment/)
- [12. The Newsfeed Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/089-12-the-newsfeed-container/)
- [Friends Container](/courses/course-03-build-real-world-full-stack-mobile-apps-java/090-13-friends-container/)
- [How Do I Create Gorgeous SideMenu](/how-do-i/how-do-i-create-gorgeous-sidemenu/)
