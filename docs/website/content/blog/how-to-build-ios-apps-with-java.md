---
title: How to Build iOS Apps with Java
slug: how-to-build-ios-apps-with-java
url: /blog/how-to-build-ios-apps-with-java/
original_url: https://www.codenameone.com/blog/how-to-build-ios-apps-with-java.html
aliases:
- /blog/how-to-build-ios-apps-with-java.html
date: '2022-06-24'
description: Learn how to build and publish iOS apps with Java or Kotlin without a
  Mac or Xcode in this comprehensive guide.
---

![How to Build iOS Apps with Java](/blog/how-to-build-ios-apps-with-java/Build-iOS-apps-with-Java-1024x536.png)

Learn how to build and publish iOS apps with Java or Kotlin without a Mac or Xcode in this comprehensive guide.

In this guide, we will get to know everything about iOS app development with Java.

We will discuss the development process, technology involved, prerequisites and general FAQs for building apps for iPhone/iPad using Java.

****Here are the major topics we’ve covered in this Java iOS app development guide.****

Java is one of the most popular programming languages around the world for everything from mobile development to enterprise and server-side applications.

Being a general-purpose, object-oriented and cross-platform programming language, Java enables developers ****Write Once, Run Anywhere (WORA)****, meaning that a Java program could be developed on any device and can be expected to run on any hardware that has a ****JVM (Java Virtual Machine)****.

Before we delve any further on how to code in Java for building iOS apps, we need to understand two most common approaches for app development…

### Native App Development

Developing a ****platform specific**** app using its ****native**** programming language is called native app development.

For example, using Java or Kotlin for Android and Swift or Obj-C for iOS.

### Cross-Platform App Development

Developing apps using a ****single codebase**** that runs on ****multiple platforms**** refers to cross-platform app development.

Some common cross-platform frameworks include Flutter, React Native and Codename One.

****Related 📝**** [Top 10 Best Cross-Platform App Development Frameworks in 2023](/blog/top-10-best-cross-platform-app-development-frameworks-in-2024/)

> On Android, Java or Kotlin are the native languages whereas Apple's iOS platform relies on Swift and Obj-C as its native languages.

## iOS App Development

### How are iOS apps made?

If you want to get into writing iOS apps for the iPhone, iPads etc, you have two options:

### Code native iOS apps with Swift or Objective-C

If you want to develop ****native apps**** for iOS, the official iOS SDK combined with Xcode allows you to write apps with Swift or Obj-C.

### Use a cross-platform framework

For those who don’t know or want to learn Swift, a suitable ****[cross-platform app development framework](/blog/top-10-best-cross-platform-app-development-frameworks-in-2024/)**** can compile your code to native iOS executable.

## Java vs Swift

### What language to choose for iOS development?

Java is the native language for Android while Swift is the native language for Apple devices (iOS, macOS, watchOS, tvOS).

Both Java and Swift are static-type, object oriented and compiled programming languages.

Swift is one of the newer programming languages while Java has been around for years.

### Difference between Java and Swift:

| Factors | Java | Swift |
| --- | --- | --- |
| Platform | Java is not platform dependent | Swift is dependent on iOS and MacOS |
| Syntax | Java is verbose and has complex syntax and code readability | Swift has easy syntax and code readability |
| Performance | Modern Java is quite fast. A well optimized Java app can be as fast as a native app | Swift is fast as it was built with performance in mind for Apple ecosystem |
| Security | Java is more secure compared to Swift with its byte-code verifier, JVM and security API's | Swift was designed to be memory safe. Swift can call Objective-C code which makes it prone to overflows |
| Popularity | Java is a mature and popular programming language. [Tiobe](https://www.tiobe.com/tiobe-index/) and [Redmonk](https://redmonk.com/sogrady/2022/03/28/language-rankings-1-22/) ranks Java at number 3 by popularity | Swift is reasonably popular for mobile development. [Tiobe](https://www.tiobe.com/tiobe-index/) and [Redmonk](https://redmonk.com/sogrady/2022/03/28/language-rankings-1-22/) ranks Swift at number 10 and 11 respectively |
|

Both Java and Swift are quite different in terms of methods, syntax, code usability etc.

Swift is obviously a preferred choice to develop apps specifically for the Apple ecosystem while Java is a preferred choice for Android development or cross-platform reusability.

> The opportunity cost involved in learning Swift outweighs the benefits since its a platform dependant language.

## Java on Apple Hardware

### Why doesn't Apple support Java?

By now you might be thinking ‘if Java code can run on any platform, why can’t Java run on iOS?’

Java can run on any platform that has a compatible Java Virtual Machine (JVM). Since Apple doesn’t support JVM for iOS, Java can’t run on iPhones and iPads.

Apple makes sure that only Swift and Objective-C has 100% vendor support on iOS by forbidding alternative runtimes from being deployed on it.

> Java developers have long been out in the cold with Apple when it comes to porting their apps on Apple devices.

## Develop iOS apps with Java

### iOS app development using Java is easier than you think.

Everything we have discussed so far is not to say that you cannot develop iOS apps in Java.

The only way to develop iOS apps in Java is to have a compiler that will compile your Java code down to native iOS binary.

With ****Codename One****, Java developers can build apps that run on iOS devices such as the iPhone and iPad. Not only that, your app will work beyond iOS devices i.e Android, Windows and JavaScript.

> Want to utilize your existing Java/Kotlin skills to develops iOS apps? You can do that with Codename One!

If you’re not using Codename One yet, then sign up now:

[Sign Up - It's Free!](https://cloud.codenameone.com/secure/index.html)

## How Does It Work?

### Underlying technology that enables Codename One to develop iOS apps with Java.

Native iOS development requires a Mac with Xcode. To make matters worse, Apple makes changes to their tools on a regular basis.

Codename One has a built-in simulator when running and debugging an app. For native iOS builds, Codename One build cloud uses Macs running Xcode (the native Apple tool) to build the app.

This removes the need to install/update complex toolchains and simplify the process of building a native iOS app.

[![Codename One - iOS Architecture](/blog/how-to-build-ios-apps-with-java/Group-1563.svg)](/introduction/) 

[Codename One - iOS Architecture](/introduction/)

The process works seamlessly and makes Codename One apps native as they are literally compiled by the native platform.

Java bytecode is dynamically translated to a native iOS Xcode project and seamlessly compiled to a native binary. This binary can be installed on iOS devices or uploaded to App Store.

> • With Codename One, you don't need to have a Mac to develop iOS apps.
>   
> • Codename One also provides an option to ****build offline****.
>   
> • To understand how Codename One works, check our [developer guide](/developer-guide/).

## Prerequisites

### What you need to get started.

To run and build the project, you should be running a modern version of Mac OS, Windows, or Linux with JDK 11 installed.

You don’t need to install anything other than the Codename One plugin. You can work with Mac, Windows or Linux and everything should “just work”.

Let’s dig in.

## Getting Started

### Tutorial to walk you through the steps of building a Hello World app.

1. Codename One initializer

2. Open in IDE

3. Develop & Debug App

4. Create iOS Build

### Step 1: Generate a new project with Codename One initializr

The easiest and quickest way to create a new project is to use the [Codename One initializr](https://start.codenameone.com/).

This online tool will allow you to choose from a growing selection of project templates in Java or Kotlin, and download a starter project that you can open in your preferred IDE or build directly on the command-line using Maven.

![Codename One Initializr](/blog/how-to-build-ios-apps-with-java/1_4GPJ7_DpFLf9XGEcaBoaQQ.jpeg) 

Codename One initializr

Important:
The package name you select for your app can’t be changed once the app is submitted to a store!

• Go to [Codename One initializr](https://start.codenameone.com/), select the ****Java Bare-bones Project**** from the ****Template**** select box.

• Enter a ****Package**** and ****Main Class**** for your app. The ****Package**** will be used both for your App ID, when you submit your app to the App Store, and for your Maven project’s `groupID`. The ****Main Class**** is the name of the `main` Java class for your app.

• Press ****Download**** and save the project as a .zip file.

• After the download completes, extract the zip file.

Resources:
• [Getting Started with the Bare-bones Java App Template](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html).
  
• [Online Tool to Generate iOS Starter Project](https://dev.to/shannah/online-tool-to-generate-ios-android-starter-project-k7h)
  
• [Codename One Maven Developer Guide](https://shannah.github.io/codenameone-maven-manual/).

### Step 2: Open the project in your preferred IDE

You can run the project directly from the [command-line](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#running-on-cli) or you can open it in your preferred IDE (e.g. [IntelliJ](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#run-in-intellij) or [NetBeans](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#run-in-netbeans)).

For this tutorial, I’m going to use IntelliJ.

As this is a Maven project, IntelliJ can open this project and work with it natively without requiring any special plugins.

• Open the extracted project in IntelliJ (or your preferred IDE).

• Press the green ![intellij run icon](/blog/how-to-build-ios-apps-with-java/intellij-run-icon.png) icon to run the app in the simulator.

![](/blog/how-to-build-ios-apps-with-java/idea-toolbar.png)

• Wait while Maven downloads the build dependencies. It will open the Codename One simulator with the simple ****Hello World**** app.

### Step 3: Develop & Debug your app

If you run this project in the Codename One Simulator without making any modifications to the app, it will look something like this:

![](/blog/how-to-build-ios-apps-with-java/simulator-first-run.png)

The simulator makes it easy to develop and debug your app without having to build and deploy to a real device.

It includes a number of useful features aimed at streamlining the development process.

Generally, you would work exclusively in the simulator until you have a near finished product that you want to share with your beta-testers.

Resources:
• For more information about the ****Codename One simulator****, see this [page](/codename-one-simulator/).
  
• More information about the ****project structure, project files, editing Java code**** and ****CSS stylesheet**** can be found in this detailed [guide](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html).

### Step 4: Build the project for iOS

Finally, we can build a native app for iOS by running the Xcode iOS project target.

When building for iOS, you can either ****build locally**** or via ****build server****.

Tip:
We recommend using the ****build server**** as it doesn’t require installing any special development tools on the computer beyond the standard JDK.

#### Build Server

With Codename One build server, iOS apps can be built on Windows, Linux, or Mac with no special requirements beyond Maven and the JDK. All you need is a free Codename One account.

For iOS builds, there are two build targets that use the build server:

****• iOS Debug Build**** (for testing and debugging)

****• iOS Release Build**** (to submit to the iOS App store)

Note:
Before you can submit an iOS build to the build server, you need to go through a few of Apple’s requirements. See [iOS Prerequisites](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#ios-prerequisites) for more information about these steps.

• Click on the configuration menu in the upper right toolbar, and select ****Build Server**** > ****iOS Debug Build/iOS Release Build**** as shown below:

![](/blog/how-to-build-ios-apps-with-java/intellij-build-ios-debug.png) 

Building for iOS via Build Server

• Press the ![intellij run icon](/blog/how-to-build-ios-apps-with-java/intellij-run-icon.png) button to build the project.

• After you submit the build, you can track and install them via ****Build Server****, ****Control Center**** or the ****Android Build app****.

Resources:
• [Build Server](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#build-app).
  
•  [Control Center](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#settings).
  
• [Android Build App](https://play.google.com/store/apps/details?id=com.codename1.build.app).

#### Local iOS builds

The Local iOS build target generates an Xcode iOS project that you can open and build directly in Xcode.

Note:
This target necessarily requires a ****Mac with Xcode**** installed. See [Building for iOS](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html#ios)for more information.

• Click on the ****Configuration**** menu in the upper right toolbar, and select ****Local Builds**** > ****Xcode iOS Project**** as shown below:

![](https://www.codenameone.com/wp-content/uploads/2021/04/intellij-build-ios-project.png) 

Building locally for iOS

• Press the ![intellij run icon](/blog/how-to-build-ios-apps-with-java/intellij-run-icon.png) button to build the project.

• If all goes well, the project will be found in the `ios/target` directory.

• You can proceed to open the Xcode project (.xcworkspace file) in Xcode, and build the project.

Tip:
To build a native app, Codename One supports a variety of different target platforms:
  
  
ㅤAndroid
  
ㅤ iOS
  
ㅤ Windows
  
ㅤ Mac
  
ㅤJavaScript
  
ㅤJavaSE

## TLDR (Too Long Didn't Read)

### All of the above is shown in this tutorial by Steve Hannah

In this quick video, [Steve Hannah](https://sjhannah.com/blog/2021/04/07/video-building-a-codename-one-project-for-ios/) uses the bare-bones Java project generated by Codename One initializr, which he’s running in IntelliJ, to build for iOS.

He also gives a brief tour of the project structure and build targets.

## Resources

There are several online sources for iOS app development tutorials using Java or Kotlin with Codename One.

Here are some useful links:

• [Codename One Docs](/developing-in-codename-one/): The first steps, guides, tutorials and resources you need.

• [Compare Codename One](/compare/): Codename One’s comparison with other cross-platform frameworks.

• [Codename One Maven Developer Guide](https://shannah.github.io/codenameone-maven-manual/)

• [Getting Started with the Bare-bones Java App Template](https://shannah.github.io/cn1-maven-archetypes/cn1app-archetype-tutorial/getting-started.html)

• [Building a Codename One project for iOS (Video)](https://sjhannah.com/blog/2021/04/07/video-building-a-codename-one-project-for-ios/)

• [Build Cross-Platform Native Mobile Apps Using Java/Kotlin for iOS, Android, Desktop and Web](https://dzone.com/articles/create-native-mobile-apps-using-java-kotlin-and-ma)

## The Community

Codename One is backed by a vibrant community of more than 100k developers in over 200 countries.

Codename One 1.0 was released in 2012 by ex-Sun engineers. It was the first solution to build native iOS apps in Java and it’s still the most mature, performant and stable cross-platform mobile toolkit for Java/Kotlin developers.

Codename One Community:
  

ㅤ[GitHub](https://github.com/codenameone/CodenameOne)
  

ㅤ
[StackOverflow](https://stackoverflow.com/questions/tagged/codenameone)
  
ㅤ
[Reddit](https://www.reddit.com/r/cn1/)

##### Quick Start with Codename One initializr​

###### Build your first iOS app with Java/Kotlin now.​

[Get Started](https://start.codenameone.com/)

## Frequently Asked Questions (FAQs)

Can I run Java on iOS?

Java code can't run on iPhone and iPad since there is no Java Virtual Machine (JVM) for iOS. However, Codename One can compile Java code to native iOS binary.

Can I develop iOS apps without Swift?

Yes, you can develop iOS apps without Swift using a compiler that can compile your code down to native iOS binary.

Can I develop iOS apps with Java?

Yes, if you are a Java developer, you don’t need to learn Swift or Objective-C to develop iOS apps. You can use Codename One to develop iOS apps using Java or Kotlin.

Is Java good for iOS development?

Java is a good choice for iOS app development if you:

• Don’t know or want to learn Swift or Objective-C

• Want to build a cross-platform app

• Want easy deployment & maintenance

• Want to save costs & resources

Can I develop iOS apps without Xcode?

Yes, you can build iOS apps without Xcode using Codename One, an open-source cross-platform framework for building native apps with Java/Kotlin.

Do I need a Mac to develop iOS apps?

No, with Codename One, you can develop and distribute iOS apps without a Mac, macOS or Xcode.

Can I build iOS apps on Windows?

Yes, you can build iOS apps on Windows using Codename One without using a Mac, macOS or Xcode.

Is it legal to develop iOS apps on Windows?

Yes, it's legal to develop iOS apps on non-Apple hardware or software as it does not violate the Apple Developer Agreement.

How to build iOS apps with Java?

You can build native iOS apps with Java using Codename One. Just sign up for a free Codename One account, go to Codename One initializr, and generate a starter project that you can open, run, debug, and build in your preferred IDE.

How to build iOS apps with Kotlin?

You can build native iOS apps with Kotlin using Codename One. Just sign up for a free Codename One account, go to Codename One initializr, and generate a starter project that you can open, run, debug, and build in your preferred IDE.

## Final Thoughts

Building iOS apps with Swift is the obvious route if you want to develop specifically for iOS only.

But if you are a Java developer and don’t know Swift, Codename One is your best bet to develop iOS apps using Java or Kotlin.

Getting started with Codename One is easy. Just sign up for a free Codename One account, go to Codename One initializr, and generate a starter project that you can open, run, debug, and build in your preferred IDE.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
