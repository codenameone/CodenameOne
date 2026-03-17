---
title: "Server"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Restaurant Server"
module_key: "01-restaurant-server"
module_order: 1
lesson_order: 1
weight: 1
is_course_lesson: true
description: "Understand the server-side shape of the restaurant application and the responsibilities that belong there."
---
> Module 1: Restaurant Server

{{< youtube exL7bS0StP4 >}}

This course starts by bringing the server back into the picture. Earlier material focused heavily on the mobile side, but a real full-stack application only makes sense once the server responsibilities are clear as well.

The good news is that most of the server code in this module is deliberately boring, and that is exactly what you want. A good backend is not usually interesting because of flashy implementation tricks. It is useful because it behaves predictably, enforces the rules the client should not be trusted to enforce, and keeps the application state coherent.

The lesson uses a Spring Boot backend again, which is still a sensible choice for this kind of Java-based full-stack application. The exact IDE screenshots are older, but the architectural point remains current: the mobile app and the server are two parts of one system, and the server is where pricing, authorization, versioning, and multi-tenant concerns need to be treated seriously.

One of the most important examples in the lesson is the pricing discussion around Braintree. If the client submits prices and the server trusts them, the system is already too weak. Even if no one is attacking it actively, stale client data and altered requests can still produce wrong results. The server should recalculate authoritative values from trusted identifiers and server-side state. That single design habit prevents an entire class of problems.

The same principle shows up in entity design. IDs, DAO objects, and transport representations may look repetitive, but they serve different purposes. The entity models storage. The DAO or DTO models communication. Keeping those roles distinct gives the backend room to evolve without exposing internal persistence choices directly to the client.

So the first server lesson is really about responsibility boundaries. The mobile app owns experience and interaction. The server owns authority, validation, and cross-client consistency. Once that boundary is clear, the rest of the full-stack application becomes much easier to reason about.

## Further Reading

- [Introduction to Spring Boot](/courses/course-02-deep-dive-mobile-development-with-codename-one/002-introduction-to-spring-boot/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
- [Security Basics and Certificate Pinning](/courses/course-02-deep-dive-mobile-development-with-codename-one/028-security-basics-and-certificate-pinning/)
