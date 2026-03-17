---
title: "21. Service Layer and UserService"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 21
weight: 98
is_course_lesson: true
description: "Move from storage to business logic by implementing the first major service class behind signup, login, and activation."
---
> Module 13: Creating a Facebook Clone

{{< youtube QY7josfVbdg >}}

The entity layer stores information. The service layer is where the application starts making decisions. That distinction matters, and this lesson is the first place in the Facebook backend where the system begins to feel like more than a database with endpoints attached to it.

`UserService` is the natural place to start because signup, login, activation, and identity all flow through it. Those are not just persistence operations. They involve validation, password handling, token creation, activation workflows, and relationship bootstrapping.

The login path is a good example of service-layer value. The repositories can find candidate users by email or phone, but the service is the right place to enforce expectations such as uniqueness and password verification. The service is also the right place to convert failure into a meaningful application-level error instead of leaking raw persistence concerns outward.

Signup is more interesting because it touches almost every part of the surrounding architecture. It needs to reject duplicate accounts, absorb information from shadow-user records, hash the password, mint an auth token, generate an activation code, and then send that code through an external delivery mechanism. That is exactly the kind of workflow that belongs in a service class and nowhere else.

The lesson’s `setProps` helper is also a useful reminder that not every field should be updated just because it exists on the entity. A service method should decide explicitly which properties are safe to copy from client input and which ones require special handling.

The sections on Twilio and Mailgun are partly about tooling, but the more durable architectural lesson is that delivery integrations should be driven from the service layer and configured from outside source control. API keys, sender identities, and environment-specific credentials belong in configuration, not hard-coded into business logic.

## Further Reading

- [22. UserService Part II](/courses/course-03-build-real-world-full-stack-mobile-apps-java/099-22-userservice-part-ii/)
- [17. Spring Boot Server Architecture and the User Entity](/courses/course-03-build-real-world-full-stack-mobile-apps-java/094-17-spring-boot-server-architecture-and-the-user-entity/)
- [20. Notification, Newsfeed and ShadowUser Entities](/courses/course-03-build-real-world-full-stack-mobile-apps-java/097-20-notification-newsfeed-and-shadowuser-entities/)
- [Push 3 - The Server Side and Build Logic](/courses/course-03-build-real-world-full-stack-mobile-apps-java/022-push-3-the-server-side-and-build-logic/)
