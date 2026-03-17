---
title: "17. Spring Boot Server Architecture and the User Entity"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 17
weight: 94
is_course_lesson: true
description: "Set up the backend architecture and define the first real server-side entity around the user model."
---
> Module 13: Creating a Facebook Clone

{{< youtube iCY64ThCleI >}}

Up to this point the Facebook clone has been living on a mock server boundary. This lesson starts building the real backend behind that boundary, and it does so in the right order: architecture first, then the first entity.

The four-layer split in the lesson is still a strong way to explain the backend: web-service endpoints at the edge, service classes for business logic, DAO objects for transport, and JPA entities plus repositories for persistence. Even if the exact Spring stack evolves, that separation of concerns remains valuable because it keeps storage, transport, and business rules from collapsing into one class.

The best part of this approach is that it leaves room for change. A service method can later be exposed through a different transport without rewriting the business logic. A storage mechanism can later be swapped without forcing the client contract to change at the same rate. That is the real payoff of the structure, not just aesthetic cleanliness.

The `User` entity is the right place to begin because almost every feature built so far depends on it. Signup, login, profile rendering, friendships, avatars, and notifications all become easier to reason about once the server has a proper user model.

The discussion of IDs is also one of the more useful backend design points in the course. Exposing sequential numeric primary keys to the client is convenient at first, but it creates obvious problems. UUID-style external IDs make much more sense for a public-facing system where client-visible identifiers should not invite trivial enumeration.

The client and server user models are deliberately similar, and that is good. They represent the same domain concept. The server version can still carry extra concerns such as password hashing and security token handling, but the basic overlap is a strength, not a problem.

## Further Reading

- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [10. Client Data Model - User, Post and Comment](/courses/course-03-build-real-world-full-stack-mobile-apps-java/087-10-client-data-model-user-post-and-comment/)
- [Introduction to Spring Boot](/courses/course-02-deep-dive-mobile-development-with-codename-one/002-introduction-to-spring-boot/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
