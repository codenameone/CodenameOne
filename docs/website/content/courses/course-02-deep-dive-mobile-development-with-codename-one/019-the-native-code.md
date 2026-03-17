---
title: "The Native Code"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Native Interfaces - Billing"
module_key: "06-native-interfaces-billing"
module_order: 6
lesson_order: 3
weight: 19
is_course_lesson: true
description: "Implement the platform-specific side of a Codename One native interface cleanly and defensibly."
---
> Module 6: Native Interfaces - Billing

{{< youtube swgT_aDsv3U >}}

Once the interface shape is settled and the native dependencies are in place, the remaining job is to implement the platform code without letting threading, lifecycle, and callback mechanics turn the integration into a black box.

The most practical advice in the lesson is still the best advice today: if the integration is non-trivial, generate native sources and inspect the result in the platform IDE. That gives you real build feedback, better navigation, and a much easier debugging path than trying to reason about everything from the portable project alone. The [include sources](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/) workflow is still the right tool for that job.

The Android and iOS implementations shown here look different, but the conceptual structure is the same. Marshal arguments from the Java side into the native SDK, run on the thread the native platform expects, receive the native callback, and then bridge the result back into the portable layer in a controlled way. Once you view native integration through that lens, the platform syntax becomes less intimidating.

Threading deserves special attention. The Codename One EDT is not the same thing as the native platform UI thread. That difference matters because many native SDKs assume they are being called from the platform's own event thread. If you ignore that boundary, integrations can fail in ways that look random but are really just threading mistakes.

This lesson also helps demystify the uglier parts of native stubs, especially on iOS where generated method names and VM bridge calls can look hostile at first glance. The important point is that most of the ugliness lives at the boundary. Your application code should not have to see it. That is another reason the wrapper layer matters.

So the modern version of this advice is: prototype in the native IDE when needed, keep the native code narrow, respect thread boundaries, and translate results back into a clean Java-facing API. That is how native integrations stay maintainable instead of becoming permanent fear zones in the project.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Native Device Functionality, Invoke Native Interfaces](/how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
