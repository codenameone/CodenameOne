---
title: "28. Client/Server Signup Process"
layout: "course-lesson"
course_id: "course-03-build-real-world-full-stack-mobile-apps-java"
course_title: "Build Real World Full Stack Mobile Apps in Java - Free Course"
module_title: "Creating a Facebook Clone"
module_key: "13-creating-a-facebook-clone"
module_order: 13
lesson_order: 28
weight: 105
is_course_lesson: true
description: "Connect the signup wizard to the real backend and turn the mock account flow into a working client/server process."
---
> Module 13: Creating a Facebook Clone

{{< youtube 8WER-8R0WqA >}}

The sensible first end-to-end integration point is authentication, and this lesson makes that explicit. Once login and signup talk to the real backend, the app stops being a UI prototype and starts becoming a working product.

The login wiring is intentionally modest, which is good. The screen collects the fields it already owns, builds the request object, shows a progress indicator while the network request is in flight, and then either enters the main UI or displays an error. That is the whole story, and it should be.

Signup is more interesting because it forces the multi-step wizard to start producing real data instead of just navigating from screen to screen. The binding approach introduced earlier pays off here. As the user moves through the wizard, the bound user object accumulates the entered values until the final request can be sent to the server.

That is a clean way to connect a wizard to a backend. The UI does not need to manually copy every field at the end because each stage has already been working against the same evolving object.

The confirmation step also becomes meaningful here. Instead of being a final mockup screen, it now sits on top of the real activation flow triggered by the backend’s SMS or email delivery. That makes the whole signup branch feel much more grounded.

The last change to the UI controller is small but important: startup now depends on whether a session already exists. That is one of the first places where the app behaves like a real installed client instead of a demo that always begins from the same blank state.

## Further Reading

- [27. Client Side ServerAPI](/courses/course-03-build-real-world-full-stack-mobile-apps-java/104-27-client-side-serverapi/)
- [21. Service Layer and UserService](/courses/course-03-build-real-world-full-stack-mobile-apps-java/098-21-service-layer-and-userservice/)
- [22. UserService Part II](/courses/course-03-build-real-world-full-stack-mobile-apps-java/099-22-userservice-part-ii/)
- [4. Login Form](/courses/course-03-build-real-world-full-stack-mobile-apps-java/081-4-login-form/)
