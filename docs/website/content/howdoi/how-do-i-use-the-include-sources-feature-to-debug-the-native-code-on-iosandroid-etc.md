---
title: USE THE INCLUDE SOURCES FEATURE TO DEBUG THE NATIVE CODE ON IOS/ANDROID ETC.
slug: how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc
url: /how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc.html
tags:
- pro
description: The source is useful for debugging on device, looking under the hood
  and writing native interfaces
youtube_id: 6oTy-LcTm0s
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-7-1.jpg
---
{{< youtube "6oTy-LcTm0s" >}}
The include-sources feature is for the moments when you need to see the native project Codename One generated for a build. That makes it useful for native debugging, profiling, investigating platform-specific behavior, and understanding how a portable Codename One class is represented on the native side.

It is not the primary way to write platform-specific code. If you need durable custom native functionality, native interfaces are the right abstraction. Include-sources is the tool you reach for when you need visibility into the generated native output or when you need to debug what the platform-specific side is actually doing.

When the feature is enabled, the build produces an additional source archive alongside the native build result. That archive contains the generated native project for the target platform. Because the build has to package more output, it takes longer, which is one reason this is not the default path for ordinary builds.

For iOS, the important practical detail is that you open the generated `xcworkspace`, not just the bare project file. The workspace includes the full project structure Xcode expects to run and debug correctly. Once it is open, you can run on the simulator or on a real device and use the normal native debugging tools there. Device testing is still essential for features that Apple does not support in the simulator, such as push.

For Android, the generated project opens in Android Studio. The exact Gradle and Android tooling details in the old video are outdated, but the workflow is still valid: open the generated project, point it at your local Android toolchain if needed, then run or debug on a device or emulator. In practice, a real device is often the faster and more useful target for this kind of debugging.

The strongest use of include-sources is when you want to set breakpoints inside generated native code and walk the full stack. That lets you see how a high-level Codename One operation ends up behaving on the native side. If you are diagnosing a rendering problem, a lifecycle issue, a native crash, or a problem in a native interface bridge, this can be far more revealing than debugging only from the portable Java side.

The video shows this with breakpoints inside `Dialog` handling, and that lesson generalizes well. Once you can stop inside the generated native implementation, inspect variables, and walk back up the stack, you have a much clearer picture of whether the problem is in your app code, in the Codename One framework layer, or in the native platform behavior underneath it.

One subtle but important point is that the source coverage differs by platform. On Android, much of your application logic may still be represented as packaged binaries in the generated project. On iOS, more of the translated output is available as native source because of how the pipeline works. That affects what you can step into and how directly you can inspect it.

The modern takeaway is simple: use include-sources when the normal simulator or application-level debugger stops being enough. It is a debugging and inspection feature, not a day-to-day coding model. Once you understand the native-side issue, the lasting fix should still go back into the real Codename One project, build configuration, or native interface code.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Build Server](/build-server/)
- [How Do I Debug On An Android Device](/how-do-i/how-do-i-debug-on-an-android-device/)
- [How Do I Access Native Device Functionality? Invoke Native Interfaces?](/how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
