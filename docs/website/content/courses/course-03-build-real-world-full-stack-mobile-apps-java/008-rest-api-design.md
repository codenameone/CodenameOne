---
title: "REST API Design"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "App Maker Server"
module_key: "04-app-maker-server"
module_order: 4
lesson_order: 2
weight: 8
is_course_lesson: true
description: "Design the app-maker API around the actual workflow instead of forcing everything through one giant request."
---
> Module 4: App Maker Server

{{< youtube L7ulPCUxIsw >}}

API design becomes much easier once you stop asking what would be theoretically elegant and start asking what the client actually needs to do. That is the real value of this lesson.

The tempting design for an app maker would be one giant “submit everything and build” request. It sounds simple on paper, but it does not fit the product well. The user is editing incrementally, uploading assets separately, previewing changes, and eventually asking for a build. Those are different operations with different reliability and security concerns, so they should not be collapsed into one oversized endpoint.

The lesson's incremental API design is stronger because it matches the workflow. Restaurant updates are separate from dish updates. File upload is separate from structured JSON changes. Build triggering is separate from both of those because it starts a distinct asynchronous process. That separation makes the system easier to reason about, easier to retry, and easier to secure.

The file-download discussion is especially valuable because it highlights a type of mistake that often sneaks into “internal” APIs. The moment you expose file retrieval, you are no longer just returning an object from a database. You are handling a path into part of the server's filesystem boundary. That demands validation even if the API feels private or product-specific.

This lesson also reinforces a broader design rule: REST is not about worshipping nouns or HTTP verbs in the abstract. It is about shaping the server surface so that the client can perform its real tasks safely and cleanly. When the operations are different, the endpoints should reflect that.

## Further Reading

- [Introduction, Architecture and Authorization](/courses/course-03-build-real-world-full-stack-mobile-apps-java/007-introduction-architecture-and-authorization/)
- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
- [Communicating from the Client](/courses/course-03-build-real-world-full-stack-mobile-apps-java/009-communicating-from-the-client/)
