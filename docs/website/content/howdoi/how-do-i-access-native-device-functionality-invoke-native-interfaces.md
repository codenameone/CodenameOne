---
title: ACCESS NATIVE DEVICE FUNCTIONALITY? INVOKE NATIVE INTERFACES?
slug: how-do-i-access-native-device-functionality-invoke-native-interfaces
url: /how-do-i/how-do-i-access-native-device-functionality-invoke-native-interfaces/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-access-native-device-functionality-invoke-native-interfaces.html
tags:
- advanced
description: How to integrate native iOS, Android etc. functionality into your Codename
  One app without sacrificing the portability of the whole app
youtube_id: 2xKvvv7XoVQ
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-10-1.jpg
---
{{< youtube "2xKvvv7XoVQ" >}}
Native interfaces are how you call platform-specific code from a Codename One application without giving up the portability of the rest of the project. When Codename One says "native" in this context, it does not mean Java's ordinary `native` keyword. It means "use the platform's own language and APIs" when you need something that the portable Codename One layer does not expose directly.

That means the implementation changes by platform. On Android, a native interface implementation can use the Android SDK and third-party Android libraries. On iOS, the implementation uses the native iOS language and APIs. On JavaScript builds, you can call into JavaScript. On the desktop port, you can use ordinary JavaSE APIs. The Java code in your main app remains the same; the native interface is the bridge that dispatches to the right implementation on each platform.

The core idea is simple: define an interface that extends `NativeInterface`, then provide platform-specific implementations of that interface. The interface becomes the contract between your portable Java code and the native side. `isSupported()` is especially important because not every platform will necessarily have an implementation. Your Java code should check whether the native feature is available before relying on it.

This is useful both inside an application and inside a cn1lib. In fact, cn1libs are one of the best uses of native interfaces because they let you wrap messy platform-specific integration behind a clean Java API. The caller sees a normal library. The native details stay hidden inside the implementation.

The most important habit here is to keep the native surface area small. Expose the narrowest interface that solves the problem. It is tempting to mirror a large native API directly, but that usually leads to code that is harder to maintain, harder to test, and harder to port. A small interface with a few focused methods is much easier to reason about and much easier to support across platforms.

Native interfaces also restrict the kinds of types you can pass. That is intentional. Simple values such as primitives, strings, byte arrays, and peers are much easier to move between languages and runtimes. Once you try to pass complex Java objects directly into Objective-C, JavaScript, or other native code, translation becomes far more complicated and performance becomes harder to predict.

`PeerComponent` is one of the most important special cases. It allows native code to return a native visual component that can be placed into a Codename One layout like an ordinary component. The classic example is a native map view. That pattern is powerful because it gives you real native UI where it matters while still letting the surrounding screen remain portable.

The hard part of native interfaces is often not the code itself but the configuration around it. Native libraries frequently come with Gradle dependencies on Android, CocoaPods or frameworks on iOS, manifest changes, plist changes, and packaging rules for extra files. In Codename One these are usually handled through build hints and native source packaging. If a native library's installation instructions start with "add this dependency", "edit the manifest", or "add this plist entry", you should read that as configuration work that must be expressed through the Codename One build system.

The video shows the older "generate native stubs from the IDE" workflow. The idea behind it is still valid, but the everyday project structure around Codename One is more Maven-centric now. The principle has not changed: define the interface cleanly, implement it per platform, and keep the real fix or integration logic in the project that will survive regeneration and rebuilds.

For deeper debugging, the include-sources workflow is still useful. When native code needs to be debugged in the native IDE, generate or inspect the native project, verify the behavior there, and then bring the stable implementation back into the Codename One project. The generated artifacts are useful for diagnosis, but the durable source of truth should remain your actual project and library structure.

## Further Reading

- [Developer Guide](/developer-guide/)
- [Build Hints](/build-hints/)
- [Build Server](/build-server/)
- [Introduction for Android Developers](/introduction-for-android-developers/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android Etc.](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
