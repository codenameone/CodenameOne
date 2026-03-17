---
title: "Introduction, Architecture and Authorization"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "App Maker Server"
module_key: "04-app-maker-server"
module_order: 4
lesson_order: 1
weight: 7
is_course_lesson: true
description: "Connect the app maker to the backend and define the authorization model for editing restaurant data."
---
> Module 4: App Maker Server

{{< youtube FcHVK9ObONg >}}

Up to this point the app maker has mostly been a client-side mockup and workflow exercise. This module turns it into a real system by deciding how it talks to the server and how the server decides who is allowed to change what.

The first architectural choice is pragmatic and sensible: reuse the restaurant server instead of inventing a second independent backend immediately. That may not be the forever architecture, but it is the right depth-first move because it lets the product validate its end-to-end flow before spending time on system separation that may not yet be justified.

Authorization is the central problem here. Public restaurant data can be fetched by ordinary identifiers, but editable restaurant state needs a different trust boundary. The lesson solves that by introducing a secret key used only for write operations. That is a strong fit for this kind of product because it avoids the ceremony of a full user-password system while still creating a private capability that the public app should never expose.

This is a good example of designing security around the actual product, not around abstract purity. The restaurant app and the app maker do not need identical access models. One is a public consumer-facing app. The other is an editing tool. They should not be using the same credential path just because they operate on the same data.

The lesson also hints at an important full-stack pattern: some operations are naturally asynchronous. A build request, for example, may take long enough that the server should queue the work and report the result later rather than pretending it is an ordinary fast request. That is the right instinct, because builder-style products often have a split between quick CRUD operations and longer-running generation tasks.

So the broader takeaway is that once the app maker becomes real, the server is no longer just a storage endpoint. It becomes the place where editing authority, asynchronous work, and product boundaries are defined.

## Further Reading

- [Server](/courses/course-03-build-real-world-full-stack-mobile-apps-java/001-server/)
- [REST API Design](/courses/course-03-build-real-world-full-stack-mobile-apps-java/008-rest-api-design/)
- [Security Basics and Certificate Pinning](/courses/course-02-deep-dive-mobile-development-with-codename-one/028-security-basics-and-certificate-pinning/)
