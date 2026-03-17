---
title: "27. Client Side ServerAPI"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 27
weight: 104
is_course_lesson: true
description: "Replace the mock client API with real network-backed calls and keep the app’s server boundary stable on the device."
---
> Module 13: Creating a Facebook Clone

{{< youtube yamsuV5Airc >}}

This is where the clone stops pretending. The mock `ServerAPI` that made UI work possible earlier now gets replaced with a real network-backed implementation, and the payoff of introducing that abstraction early becomes obvious immediately.

Because the UI was already written against an application-level API rather than against inline sample data, most of the work here is glue code rather than a client-side rewrite. That is exactly what good abstraction buys you.

The helper methods for GET and POST requests are simple, but they matter. They centralize the base URL, authorization token, JSON headers, and common request behavior. Once those pieces live in one place, the rest of the client API stops repeating protocol noise.

The login/signup path is also a useful demonstration of client-side state handling. Successful auth responses hydrate the user model, persist it into storage as JSON, and keep the token separately for convenient authorization headers later. That design is still reasonable. It gives the app a durable local session story without making startup logic depend on a fresh login every time.

The media-upload path is the most protocol-heavy part of the lesson, but even there the client code stays relatively clean because Codename One already provides a usable multipart abstraction. The important thing is that the callback threading is handled correctly. If the network work finishes off the EDT, the success path has to return to the EDT before it mutates UI state.

The contact-upload logic is also a good reminder that “client API” sometimes means real translation work. Native contact data does not arrive in the exact shape the server wants, so the client adapter has to transform it into the application’s transport format.

## Further Reading

- [28. Client/Server Signup Process](/courses/course-03-build-real-world-full-stack-mobile-apps-java/105-28-client-server-signup-process/)
- [29. Newsfeed and Posts From Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/106-29-newsfeed-and-posts-from-server/)
- [25. WebService Layer and UserWebService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/102-25-webservice-layer-and-userwebservice/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
