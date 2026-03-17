---
title: "Push Notification"
layout: "course-lesson"
course_id: "course-01-java-for-mobile-devices"
course_title: "Java for Mobile Devices - Free online course"
module_title: "Course Lessons"
module_key: "01-course-lessons"
module_order: 1
lesson_order: 13
weight: 13
is_course_lesson: true
description: "Understand push notification as a signaling channel rather than a general networking layer."
---
> Module 1: Course Lessons

{{< youtube 8wzBpEp81Kc >}}

Push notification is best understood as a user-notification channel, not as a general-purpose networking layer. It is good for telling a device that something happened and, depending on platform and app state, optionally carrying some payload with that message. It is not something you should design your core application protocol around.

That distinction matters because push is inherently unreliable as a transport. Users can disable it. Some devices or services may not support it the same way. Different platforms treat background delivery differently. Build your app so that push signals something important, but do not assume it is always available or always delivered in the same way everywhere.

Codename One smooths over a lot of the platform differences by providing a unified push API and server-side push entry point. You still need platform credentials for the vendor services, but you do not have to design a completely separate push implementation for every target OS. The high-level workflow is: configure the provider credentials, register the app for push, collect the push token on the device, send that token to your server, and then use your server to send push messages through the Codename One push infrastructure.

On the client side, one of the most important details is that push callbacks belong in the main application class. The `PushCallback` implementation must live in the class that represents the app lifecycle. That is where Codename One wires push delivery into the application. The key callbacks are the message callback itself, the registration callback, and the registration error callback.

The registration callback is especially important because that is where you usually obtain the push key and send it to your own backend. A common mistake is to treat the device identifier argument as if it were the push token. It is not. The push key is what your server needs in order to target that device later.

Server-side push is just as important as the client setup. Sending the request is not enough; you also need to parse the response and log the outcome. A lot of push debugging time is wasted by code that fires off a request and never looks closely at the response body. If delivery fails because of a credential mismatch, certificate problem, provider rejection, or malformed request, that response is often your only useful clue.

Push types also need to be chosen deliberately. Visible notification types are usually the safest default because they match what users and platforms expect. Hidden push and payload-heavy push are more nuanced and behave differently across operating systems, especially on iOS. The lesson is out of date in its references to older Google push naming and setup steps, but the design lesson is still current: treat push as signaling, not as the backbone of app synchronization.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Build Hints](/build-hints/)
- [Build Server](/build-server/)
- [How Do I Use Crash Protection? Get Device Logs?](/how-do-i/how-do-i-use-crash-protection-get-device-logs/)
