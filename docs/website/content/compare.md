---
title: "COMPARE"
date: 2020-09-11
slug: "compare"
---

- Home
- Compare

# COMPARE CODENAME ONE

It's often an Apples and Oranges comparison since Codename One is so different from everything else.

To properly evaluate a cross-platform framework, we often look at our existing experiences and evaluate based on what we know.

The set of options below present some of insights about how we match up to other cross-platform solutions in the market today. But first lets start up with some of the Codename One benefits.

![Comparing Cross-Platform Frameworks](/uploads/Competition_Monochromatic.png)

### Codename One is..

Java based

Love it or hate it, Java is still king of the hill. With 9-10 million active Java developers and a permanent spot as one of the top programming languages. The language/platform is familiar to tens of millions of developers world wide guaranteeing code maintainability well into the future.

Fast

Codename One translates all the code to native code or the native VM, resulting in performance that matches the performance of native code.

Easy to use

Developers can use the rich tools available for Java developers such as IntelliJ IDEA/NetBeans/Eclipse to work with the code. Codename One has its own GUI builder and many niceties to track issues in your code. The biggest benefit though is in the build server which generates a native application for you without having to deal with all of the complexities of building a native app for every platform.

Portable - WORA

Due to its unique lightweight architecture, Codename One boast greater portability than HTML5 and more supported devices! Thanks to its unique cloud build system, it doesn’t require any native toolchain or dedicated hardware.

### Codename One vs

Flutter React Native Hybrid-Web Hybrid-Native Xamarin J2ObjC JavaFX/Swing Mobile Application Framework (MAF) Flutter

- **Truly Native –** Flutter is an entirely separate runtime on Android which means every Flutter app pays the price of two separate runtimes communicating among themselves. This means an inherent, permanent overhead in size/performance and portability that can’t be erased. Communicating outside of the ART runtime is inherently slow on Android.  
    

- **Dart Language –** Dart is an unpopular language and never gained serious community traction like Java and Kotlin.
- **Threads** – Flutter tries to simplify the programming model by removing threading from the equation. While this does make some elements simpler, it makes other things far more cumbersome. Since native code and the physical OS use threading all profiler/debugging data is much harder to work with due to the asynchronous nature of all calls.

- **CSS –** Codename One uses a subset of CSS that can be edited live to see the changes in a running app. Flutter uses a custom language to define styling.
- **Reactive vs. Imperative –** Flutter uses a more reactive model whereas Codename One is imperative with some reactive capabilities built on top. This gives Codename One developers more flexibility in programming. Imperative development is arguably easier since it’s linear. Reactive programming gives up some developer power/control which is a consistent trend with Flutter (notice the thread point above)
- **Standard based –** Codename One uses Maven as its build system and other common standards. Flutter uses its own build system, IDEs etc.
- **Native Calls –** Building a native call in Flutter is difficult since it requires complex native integration with a messaging system to Flutter and its asynchronous call system that works differently from all native OS. Codename One native calls are just a native method call in the respective native language. It’s that simple.
- **No Hardware Limitations –** Codename One offers a 100% open source Java & Kotlin VM that works on iOS, Android, Windows UWP and others without any hardware limitations. It doesn’t require a Mac for iOS development and doesn’t require a Windows machine for Windows development.

- **App Size –** Since Flutter apps use built-in widgets and not platform widgets, the app’s size is usually bigger. Currently, the smallest possible app made with Flutter can weigh no less than 4MB. A basic ‘Hello World’ app in Flutter produces a 7.5mb file, where Codename One uses 1.7mb only, so for relatively small apps, using Codename One is a choice you won’t regret.

React Native

- **WORA –** React Native isn’t a WORA solution – it uses native widgets and even has its own lightweight implementation of a controller since the concept of controllers is not universal.
- **Performance –** React Native has on device performance issues because basic UI concepts aren’t interchangeable between native widgets. This isn’t a problem for Codename One where all components are lightweight.
- **Hardware Requirements –** React Native requires a Mac for iOS development and a complex toolchain for every platform.
- **Abstractions –** React abstracts everything behind callbacks reducing code size but making the behavior and flow more opaque.
- **Portability –** React Native isn’t as portable, you can’t reuse the code for a web embedded app or a desktop app.
- **Expressiveness –** React Native isn’t more terse than Codename One with Java 8 or Kotlin.
- **JavaScript –** JavaScript is a fragile language and the practice of working outside of an IDE is inferior as projects grow in size.

Hybrid-Web

Hybrid-Web frameworks such as **Ionic, Cordova, Monaca** etc leverage web technologies i.e. HTML, CSS & JavaScript to build hybrid web apps that run in WebView wrapped within a native app. These are essentially web apps bundled into native apps.

Most of the Hybrid-Web solutions are fundamentally built on Cordova/PhoneGap to gain access to native OS features. Codename One’s native capabilities have the following additional advantages over Hybrid-Web frameworks:

- **Stable Target –** Since your code executes on the device browser you are very likely to have device only issues that can’t be debugged properly. Codename One is statically linked to your application which means you will have a stable application with an identical version of Codename One on all devices! Since the code is handled by a single entity, if you run into problems we can actually help you fix them!
- **Performance –** Hybrid-Web apps are slow to begin with but made slower on devices due to some architectural choices especially on iOS devices.
- **Portability –** Web technologies were designed for HTTP delivery not for local execution, this makes adapting the UI for multiple DPI’s challenging. Codename One was built from scratch to work on all DPI’s and provides the visual tools to do so.
- **Power –** Mobile browsers are fragmented and constantly late with new features, requiring a full OS upgrade for new features. Basics such as threads aren’t supported by some devices. Codename One provides most of the power and convenience that desktop Java developers are used to.
- **Ease –** HTML isn’t hard but JavaScript with CSS while adapting for all devices can become frustrating. Codename One provides visual tools including a build server alleviating the need to install native tools on your machine.

Hybrid-Native

Hybrid-Native frameworks such as **React Native, NativeScript, Appcelerator Titanium** etc leverage JavaScript and/or JS frameworks to build hybrid web apps that run directly on the native device without using WebViews. These are essentially web apps compiled into native apps.

Most of such frameworks offer limited code reusability and requires rewriting the UI for every platform. We think the biggest difference between Codename One and all of these tools is that we are a Java/Kotlin based true “WORA” solution.

- **Native Tools** **–** For most of such tools, in order to build an app for iOS you must have a Mac and for Windows (if supported) you need a Windows machine. This is tedious and painful! Codename One uses a Build Server to remove that need and provides seamless simulator execution, debugging, profiling etc.
- **Standard IDE –** Most such tools have their own IDE’s with limited features. Codename One works with IntelliJ IDEA/NetBeans/Eclipse, which are mature industry leading IDE’s.
- **Fast Builds –** Most of the Hybrid-Native tools literally translate JavaScript code to a platform project with every build (thru the command line no less) which takes quite a while to run. Debugging is a HUGE challenge there as well. With Codename One you just press play!
- **Portability –** Most of the Hybrid-Native tools require that you rewrite your UI (roughly 10-50% of the code) for every platform. Better solutions limit the developer to the lowest common denominator approach, this requires coding specifics for every platform. Codename One uses a unique lightweight architecture that enables it to offer high level features on all devices.
- **Ease –** Most such tools don’t offer features such as integrated GUI builders, themes, localization etc

Xamarin

Xamarin might seem like a similar tool to Codename One using C# instead of Java, but this is misleading as the tools are so different conceptually, they have very little in common.

- **Native Language –** On Android, C# code will perform slower than Java code as it would need to pass thru JNI. This overhead is very noticeable when working with heavyweight widgets.
- **Cloud Build –** Codename One’s cloud build capability allows developers to build a native application using the Codename One cloud servers. This removes the need to own dedicated hardware and allows you to build native iOS apps from Windows and native Windows apps from your Mac or Linux machine.
    
- **Web Deployment –** Xamarin supports almost all of Codename One’s supported platforms. However, it doesn’t support building native JavaScript web applications. Codename One supports the process of compiling an application into a JavaScript application that can be hosted on the web. This is done by statically translating the Java bytecode to JavaScript obfuscated code.
    
- **Portability Strategy –** Codename One uses a single project that works everywhere. By default, no native code of any type is necessary to build a Codename One app. Xamarin requires a “pseudo native” project (still written in C#) to represent the lifecycle, resources and other elements of the various supported platforms. By default this will include a lot of the platform specific code. Codename One tries to abstract the aspect of platform native differences and Xamarin pushes it to the forefront.
    

J2ObjC

Google’s J2ObjC compiler is an open source project that isn’t really comparable to Codename One and solves a completely different problem. However we get asked about this frequently so this is a direct quote from their site:

> J2ObjC does not provide any sort of platform-independent UI toolkit, nor are there any plans to do so in the future. iOS UI code needs to be written in Objective-C or Objective-C++ using Apple’s iOS SDK (Android UIs using Android’s API, web app UIs using GWT, etc.).

Other than that, the tool just deals with the iOS porting of business logic. Requires a Mac and requires that developers work with Xcode. It doesn’t include a full featured garbage collector (the OS X GC mentioned in their wiki was deprecated on OS X and ARC is not a GC) and isn’t really a cross platform solution.

JavaFX/Swing

Large parts of Codename One were inspired by the design of Swing. However, a great deal was fixed. Codename One fixed many concepts in Swing thanks to hindsight and experience in Swing development e.g.

- Optimized for phones/tablets, this includes support for gestures and complex key layouts.
- Proper styles & theming.
- Resource file format/standard GUI builder.
- Truly open source.
- Static linking.

**JavaFX –** JavaFX got off in a completely different direction by using a scene-graph graphics layer approach to GUI. This is very much the approach taken up by Flash and one of the main stumbling blocks for flash on mobile devices.  
It is remarkably hard to port a scene-graph implementation consistently to every platform and then integrate it with the native platform widgets as we are sure the guys at Oracle have found out. Making this sort of implementation performant is even harder as the guys from Adobe found out.  
While Swing had a lot of traction (30% of the GUI development market at its peak) JavaFX never reached any measurable market penetration. Its only major users are Swing shops that have no future migration path other than JavaFX. Oracle itself fired most of its developer advocates and JavaFX related teams, it doesn’t use JavaFX for any of its core products, e.g. MAF has no relation to JavaFX.  
Up until recently there was no JavaFX on mobile devices and arguably there still isn’t, a team at Oracle open sourced a half baked JavaFX for iOS implementation that suffers greatly from the limitations mentioned above. This was picked by some developers and shipped as a product for mobile application development. We consider such efforts to be callous & irresponsible to the developers who look at them as a serious solution, it breeds confusion within the community that’s already fragmented.

- Java FX is HUGE and remarkably complex – can a bunch of hackers maintain/support something as big as this? Even Oracle with all its resources has a ridiculous amount of bugs within the desktop version of JavaFX.
- Oracle already vetted JavaFX’s potential on mobile and came to the realization that its not a practical solution for the mobile ecosystem due to its complexity. e.g. JavaFX requires its own kerning on top of OpenGL ES, this is nearly impossible to do properly because of the complexities of subpixel anti-aliasing on mobile screens.
- To further cemenet the point Oracle has so little faith in the future of FX that it killed the Scene Builder project.
- JavaFX was designed for desktops where manipulation of native widgets is possible and documented. Native widgets are ESSENTIAL on mobile, you can’t do input without a native widget. Video, HTML rendering etc. must all be done natively… That’s REALLY hard to do in a portable way that won’t crash the VM.
- JavaFX applications are HUGE because they need a HUGE VM. This translates to slow download, slow performance and slow builds.

To further contrast these issues with Codename One, since the project is so huge, complex and clearly not supported by Oracle picking this up will leave developers in an untenable state where bugs can’t be fixed and issues can’t be addressed (e.g. bug when migrating to iOS 10).  
Codename One is much smaller in terms of code size but has a much larger active developer community, because projects are translated to native OS projects they should be buildable and maintainable even without Codename One’s support.

Mobile Application Framework (MAF)

Oracle MAF (Mobile Application Framework) is a tool that provides the ability to create some iPhone applications using a combination of Java EE and HTML5 essentially bringing the worst of all worlds together.

  
The tool is marketed as a free tool which is misleading since its free for development but not for distribution. The cost is per application and is in the range of 50,000 USD.

  
MAF suffers from all the problems of Hybrid-Web (mentioned in the section) and it adds to that all the complexity of working with a Java backend without removing those other complexities. To make matters worse, since the VM is embedded into the code, you are limited to an interpreted mode which means slow performance on top of the existing HTML5 overhead.

|  | Codename One | Flutter | React Native | Xamarin | Ionic |
| --- | --- | --- | --- | --- | --- |
| Language | Java | Dart |
| Portability Strategy | WORA + Native Interfaces (One Project) |
| Architecture | Native |
| IDE | NetBeans, Eclipse or IntelliJ IDEA |
| Code Reusability | 100% |
| Performance | Near Native |
| App Size | Small |
| Platforms | Android, iOS, Windows, Mac, Web |
| Cloud Build | Yes (can work on Linux for iOS development) |
| Widgets | Lightweight |
| Support | Yes (Community + Professional) |
