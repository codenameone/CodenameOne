---
title: "Threading and the EDT"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 11
weight: 11
is_course_lesson: true
description: "Understand the event dispatch thread and how to keep Codename One applications responsive."
---
> Module 1: Course Lessons

{{< youtube p6UFNw0nGik >}}

Threading becomes much less mysterious once you separate two different concerns: doing work, and updating the user interface. In Codename One, those two things should not be mixed casually. The UI is driven by the event dispatch thread, usually shortened to EDT, and that thread needs to stay responsive if the application is going to feel smooth.

The simplest mental model is that the EDT is the thread that owns UI work. Painting, handling most user interaction, and changing component state should happen there. If you block it with long calculations, network waits, database work, or file processing, the app stops feeling alive. Buttons appear unresponsive, animations freeze, and the whole interface looks broken even if the code is technically still running.

That is why background work matters. Expensive or slow operations should run off the EDT, then hand control back to the EDT when it is time to update the UI. This is one of the core habits of mobile development in general, not just Codename One. The framework gives you utilities to help with that handoff, but the underlying rule is simple: keep slow work away from the UI thread.

The video also points out something that is still worth taking seriously: portability is one reason to keep threading simple. Codename One targets multiple platforms and runtime environments, so code that depends on subtle threading behavior or low-level JVM memory-model tricks is much more likely to become fragile. The framework is happiest when your concurrency model is boring and explicit.

In practice, that means you should treat the EDT as a place for fast UI logic, not a place for long-running business logic. Build the screen there. React to taps there. Start background work from there when needed. Then return to the EDT to update labels, show dialogs, replace forms, or refresh components once the work is done.

It also means you should be careful about "almost fast" work. A single blocking call may not feel dangerous while testing on a desktop simulator, but mobile hardware, slow storage, and real networks make those delays much more obvious. If an operation might pause for a noticeable amount of time, it probably does not belong on the EDT.

Once you adopt that model, threading stops being an abstract theory lesson and becomes a practical discipline: keep the interface responsive, move slow work to the background, and bring results back to the UI thread in a controlled way. Most Codename One applications do not need elaborate concurrency. They need that one rule applied consistently.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Remote Webservices, Perform Operations On The Server](/how-do-i/how-do-i-access-remote-webservices-perform-operations-on-the-server/)
- [How Do I Improve Application Performance Or Track Down Performance Issues](/how-do-i/how-do-i-improve-application-performance-or-track-down-performance-issues/)
- [How Do I Find Problems In My Application Using The Codename One Tools And The Standard IDE Tools](/how-do-i/how-do-i-find-problems-in-my-application-using-the-codename-one-tools-and-the-standard-ide-tools/)
