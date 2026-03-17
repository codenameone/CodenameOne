---
title: "Communicating with the Server"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Putting it All Together"
module_key: "08-putting-it-all-together"
module_order: 8
lesson_order: 1
weight: 25
is_course_lesson: true
description: "Build a pragmatic network layer that loads quickly, caches sensibly, and refreshes the UI safely."
---
> Module 8: Putting it All Together

{{< youtube LUHb5fuWzJE >}}

Finishing an application is usually less about adding one big feature than about making a lot of practical decisions that keep the product moving. This lesson starts that final assembly process by focusing on the network layer and on one of the most important development skills in product work: defining a finish line and refusing to keep expanding it.

On the technical side, the service layer in this lesson is built around a useful principle for mobile apps: load from local state first when you can, then refresh from the server. That makes the application feel responsive, reduces the cost of repeated launches, and gives the user something to look at immediately instead of forcing every cold start to wait on the network.

The caching approach in the older lesson is intentionally simple. The app stores the server's JSON payload locally, reloads it into the model on startup, and then asks the server whether the data has changed. That is not a full offline-sync system, but it is a strong practical baseline for many applications because it keeps startup fast without hiding the fact that the server remains the source of truth.

The other important design choice is where UI updates occur. The code uses the network layer for transport, the model layer for decoded data, and the EDT for visible updates. That separation matters. Once network completion and UI refresh logic start blurring together, responsiveness and maintainability both suffer.

This lesson also makes a valuable product point: finishing requires discipline. There is always another improvement to make. There is always one more feature that would be nice. If you never constrain the scope, the app never reaches the stage where real users can react to it. A modest but working feature set is often far more valuable than an endlessly expanding plan.

So the practical outcome here is a network layer with sane caching, timestamp-based refresh logic, and a UI that reacts when data arrives. Just as importantly, it is a reminder that "good enough to ship" is often a more important milestone than "still theoretically improvable."

## Further Reading

- [Connecting to a Web Service](/courses/course-02-deep-dive-mobile-development-with-codename-one/003-connecting-to-a-web-service/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
- [How Do I Access Remote Webservices, Perform Operations On The Server](/how-do-i/how-do-i-access-remote-webservices-perform-operations-on-the-server/)
