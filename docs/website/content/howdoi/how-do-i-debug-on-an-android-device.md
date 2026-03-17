---
title: DEBUG A CODENAME ONE APPLICATION ON AN ANDROID DEVICE
slug: how-do-i-debug-on-an-android-device
url: /how-do-i/how-do-i-debug-on-an-android-device/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-debug-on-an-android-device.html
tags:
- advanced
- debugging
description: Debugging on a device using the Android Studio IDE
youtube_id: 008AK1GfHA8
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-14.jpg
---
{{< youtube "008AK1GfHA8" >}}

Debugging on an Android device is the right move when the simulator is no longer telling you enough. If the problem only happens on Android hardware, only appears after a native build, involves Android permissions or activities, or touches native integration code, then you need visibility into the generated Android project and the device runtime.

The video shows the older version of this process: enable source inclusion, send an Android build, download the generated sources, create an Android Studio project, copy the generated code into it, and debug from there. The important idea is that when you need to understand what the Android side is doing, the generated native sources are valuable. What has changed is how often you should do this. It is no longer something you should think of as part of normal day-to-day development.

In a modern Codename One project, the application source in your main project is still the source of truth. Start there first. If the problem reproduces in the simulator, stay in the regular Codename One codebase and debug it there. If the bug only appears on Android, then generate the Android sources with source inclusion enabled and use Android Studio to inspect what the native output is doing.

The useful workflow is to treat the generated Android project as a debugging artifact. Open it in Android Studio, connect a device, run under the debugger, inspect logcat, and place breakpoints in the generated sources or any native bridge code that is relevant to the problem. This is especially useful for permission issues, manifest problems, native interface behavior, packaging problems, and crashes that only occur on device. Once you understand the failure, move the real fix back into your Codename One application, native interface implementation, or build configuration.

That last step is the important one. You should not maintain fixes directly in the generated Android sources because those files are regenerated on the next build. The generated project is there so you can observe and diagnose. The durable fix belongs in the actual project that produced it.

The video is also a bit out of date in how manual the Android Studio setup is. Today the surrounding toolchain is cleaner and more Maven-centric, but the same principle applies: include sources when you need deeper visibility, inspect the generated Android project only for Android-specific failures, and then carry the solution back to the real codebase. If you approach it that way, Android Studio becomes a precise debugging tool instead of a second development environment you have to keep in sync.

## Further Reading

- [Introduction for Android Developers](/introduction-for-android-developers/)
- [Build Server](/build-server/)
- [Build Tools](/build-tools/)
- [Development Environment](/development-environment/)
- [Moving To Maven](/blog/moving-to-maven/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
