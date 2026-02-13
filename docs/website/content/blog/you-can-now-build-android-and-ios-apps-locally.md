---
title: You Can Now Build Android & iOS Apps Locally
slug: you-can-now-build-android-and-ios-apps-locally
url: /blog/you-can-now-build-android-and-ios-apps-locally/
original_url: https://www.codenameone.com/blog/you-can-now-build-android-and-ios-apps-locally.html
aliases:
- /blog/you-can-now-build-android-and-ios-apps-locally.html
date: '2021-04-05'
author: Steve Hannah
description: Due to popular demand, we are officially providing local build support
  for iOS, Android and cross-platform JavaSE Desktop apps. No build server or Codename
  One account required.
---

Due to popular demand, we are officially providing local build support for iOS, Android and cross-platform JavaSE Desktop apps. No build server or Codename One account required.

The Build Server has been a fundamental building block of Codename One from the very beginning. In the fractured world of mobile development where you need the latest Mac running the Latest Xcode to build for iOS, and the latest Windows running the latest Visual Studio to build for Windows, the build server reduced your system requirements to JDK 8 and an IDE. In general, this was a big win, and it makes mobile app development much more pleasant.

Despite its demonstrated utility, however, it has been frequently cited as a barrier to adoption by developers who prefer not to rely on a third party service to build their apps. Developers (myself included) have a strong libertarian streak, especially when it comes to our code. We don’t like to have strings of any kind attached to code that we write, and reliance on a build server to build our code certainly qualifies as a “string”.

It has always been “possible” to build apps locally, without the build server. The Codename One source code is open source (GPL+CE), and all of the tools required to build apps are freely available, but we didn’t provide support for this option, nor did we document how to do it. It was up to the developer to hack together their own solution if they **really** wanted to build locally, and this was a complicated endeavor.

Before I joined Codename One, I developed a number of offline build tools myself, and implemented support for alternative compilers and virtual machines, so I can verify that it is and was always possible. However, my offline-build solutions were still **2nd-class** and didn’t support all of the features of Codename One. Native Interfaces, for example, require some complex logic to generate platform-specific “glue”, and my offline builders didn’t support this.

Since joining Codename One, we have had ongoing internal discussions about the merits of developing, maintaining, and supporting local build options. Would local builds result in an avalanche of support requests that would swallow up our limited resources? Would it result in developers opting for the “free” offline build option and canceling their subscriptions? Such a result might impact our ability to exist at all.

These are unresolved questions, but we are poised to find out their answers, as we are officially providing local build support for iOS, Android, and Desktop. These build options are built into our new Maven project archetype. See below for details and caveats about these build targets.

### iOS Builds

The Local iOS build target generates an Xcode project that you can open and build directly in Xcode. This target necessarily requires a Mac with Xcode and cocoapods installed. See [Building for iOS](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#ios) from [this tutorial](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html) for more information.

![intellij-build-ios-project](https://www.codenameone.com/wp-content/uploads/2021/04/intellij-build-ios-project.png)

### Android Builds

The Local Android build target generates an Android Studio project that you can open and build directly in Android Studio. This target necessarily requires that you have Android Studio installed, but it doesn’t have any special requirements outside of that. See [Building for Android](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#android) from [this tutorial](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html) for more information.

![intellij-android-gradle-build](https://www.codenameone.com/wp-content/uploads/2021/04/intellij-android-gradle-build.png)

### Desktop Builds

The local desktop build target builds a cross-platform JavaSE desktop application in the form of an executable Jar file. See [Building JavaSE Desktop App](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#javase) from [this tutorial](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html) for more information.

![intellij-javase-desktop-app](https://www.codenameone.com/wp-content/uploads/2021/04/intellij-javase-desktop-app.png)

### The Case for the Build Server

Now that you can build your projects locally, you are not tied to the build server. You don’t require a Codename One subscription in order to build your code anymore. Your code is your code, and that is final. However, I think you’ll still find that the build server provides a superior developer experience.

The build server provides a one click experience from project to distributable app. Local builds require you to first generate the native (Xcode/Android Studio) project, then open and build that project. This is more time-consuming and fraught with pain points, as maintaining your own local toolchains isn’t all sunshine and rainbows.

### The Case for a Codename One Subscription

If you find Codename One useful, but you don’t require access to our build servers, then the best reason to purchase a subscription is to help us keep the lights on and make the product better.

Beyond that, a Pro subscription provides access to many useful features such as the Push notification service, crash reporting, and e-mail support, and Enterprise subscription provides support for Javascript builds, phone/Zoom support, and many more benefits.

We are also working on some other exclusive content options that will be available to subscribers, as a reward for your support.

### Help Spread the Word

Subscriptions aside, the best way to support us is to spread the word. Blog about the cool things you’re doing with Codename One. Share us on sites like Reddit, Facebook, Twitter, and Hacker News. Answer Codename One related questions on Stack Overflow to help other community members. Build cn1libs and publish them on Github.

We all benefit from a larger, more diverse community.

### Getting Started

Getting started with Codename One is easy. Just go to [Codename One initializr](https://start.codenameone.com/), and generate a starter project that you can open, run, debug, and build in your preferred IDE.
---

## Archived Comments

_This post was automatically migrated from the legacy Codename One blog. The original comments are preserved here for historical context. New discussion happens in the Discussion section below._


### **Mehdi ELGHISSASSI** — June 1, 2021 at 7:03 pm ([permalink](https://www.codenameone.com/blog/you-can-now-build-android-and-ios-apps-locally.html#comment-24461))

> Mehdi ELGHISSASSI says:
>
> hello,  
> how i can display Local Build config option in netbeans to choose Gradle Android Project to generate an Android Studio project.
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fyou-can-now-build-android-and-ios-apps-locally.html)


### **Shai Almog** — June 2, 2021 at 1:12 am ([permalink](https://www.codenameone.com/blog/you-can-now-build-android-and-ios-apps-locally.html#comment-24462))

> Shai Almog says:
>
> You need to migrate the project to maven as explained here <https://www.codenameone.com/blog/moving-to-maven.html>
>
> [Log in to Reply](https://www.codenameone.com/wp-login.php?redirect_to=https%3A%2F%2Fwww.codenameone.com%2Fblog%2Fyou-can-now-build-android-and-ios-apps-locally.html)

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
