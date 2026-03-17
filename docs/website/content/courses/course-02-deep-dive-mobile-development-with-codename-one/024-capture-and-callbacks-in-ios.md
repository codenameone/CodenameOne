---
title: "Capture and Callbacks in iOS"
layout: "course-lesson"
course_id: "course-02-deep-dive-mobile-development-with-codename-one"
course_title: "Deep Dive into Mobile Development with Codename One - Free Online Course Material"
module_title: "Native Interfaces - Camera"
module_key: "07-native-interfaces-camera"
module_order: 7
lesson_order: 5
weight: 24
is_course_lesson: true
description: "Complete the iOS camera bridge by wiring capture operations and native callbacks back into Java."
---
> Module 7: Native Interfaces - Camera

{{< youtube MbTEz-K8iwY >}}

The last part of the iOS camera work is where the bridge becomes truly useful: still capture, video capture, and the callback path back into the portable layer. Once those are in place, the camera integration stops being a preview widget and becomes a working feature.

The main challenge here is not just calling the native capture APIs. It is doing so on the right thread, with the right delegate or callback setup, and then translating the result into something the Java side can consume. That is the part developers often find intimidating, but the underlying pattern is consistent: start native work, let the native API complete asynchronously, then convert the result into the portable representation you want.

Still image capture is often more complicated than expected because platform APIs evolve over time and older iOS versions may require a different path from newer ones. The specifics in the video are tied to that era of iOS, but the general lesson is still current: camera integrations often need version-aware code paths, and a bridge must isolate that complexity so the portable API remains stable.

Video capture tends to be easier conceptually because the lifecycle is clearer: start recording, receive the completion callback, stop recording, then hand the resulting path or media object back to the Java side. Even there, though, the important part is not the one method call. It is the callback contract between native completion and portable code.

This lesson also reinforces why patience matters more than heroics in native work. Most of the code is not brilliant. It is procedural, repetitive, and specific. The value comes from keeping the bridge narrow enough that all of that platform-specific detail remains confined to one place instead of leaking into the application.

## Further Reading

- [Developer Guide](/developer-guide/)
- [How Do I Access Native Device Functionality, Invoke Native Interfaces](/how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)
