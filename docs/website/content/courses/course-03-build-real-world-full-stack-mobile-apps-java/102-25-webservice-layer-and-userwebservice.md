---
title: "25. WebService Layer and UserWebService"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 25
weight: 102
is_course_lesson: true
description: "Expose the user and notification operations as clean HTTP endpoints without leaking business logic into controller code."
---
> Module 13: Creating a Facebook Clone

{{< youtube rjIQqfrFvbI >}}

Once the service layer exists, the web-service layer should be boring. That is not a criticism. It is a sign that the architecture is doing its job.

This lesson demonstrates that well. `UserWebService` is mostly a translation layer. It defines URLs, request shapes, headers, and response behavior, then hands the real work off to the service classes underneath. That is exactly what controllers should look like in a healthy backend.

The exception-to-error-DAO mapping is one of the most important parts here. It gives the client a predictable error contract instead of exposing raw Java exceptions or framework defaults. Even simple apps benefit from that discipline because it keeps client-side error handling from becoming guesswork.

The avatar endpoints are a good example of the controller layer adding protocol-specific behavior without swallowing business logic. They return image content with the right response metadata, turn missing avatars into `404` semantics, and still leave the real authorization and data retrieval rules to the service layer.

The lesson also makes a pragmatic choice in a few places by using GET operations for state changes when the mobile client benefits from simpler calling code. That is not something I would present as ideal REST design today. It works here because the client is controlled, but the general rule should still be to avoid using GET for operations that mutate state.

So the right takeaway is not “copy these HTTP verbs literally.” It is “keep the controller thin, explicit, and responsible only for transport concerns.”

## Further Reading

- [26. PostWebService and MediaWebService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/103-26-postwebservice-and-mediawebservice/)
- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [22. UserService Part II](/courses/course-03-build-real-world-full-stack-mobile-apps-java/099-22-userservice-part-ii/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
