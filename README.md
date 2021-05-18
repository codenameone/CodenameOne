<img src="https://www.codenameone.com/img/blog/github-banner-1.jpg"> 


## Codename One - Cross Platform Native Apps with Java or Kotlin

[Codename One](https://www.codenameone.com/) is a mobile first cross platform environment for Java and [Kotlin](https://www.codenameone.com/blog/kotlin-support-public-beta.html) developers. It can compile Java bytecode to native OS executables (iOS, Android, UWP etc.).
It's a complete mobile platform featuring virtual machines, simulator, design tools (visual theme/builder/css), IDE integrations, ports to multiple OS's and much more. It provides full access to the underlying native OS code (e.g. Objective-C, C#, Dalvik/ART) through a portable abstraction which enables 100% code reuse.

![GitHub repo size](https://img.shields.io/github/repo-size/codenameone/CodenameOne?style=plastic)
![GitHub top language](https://img.shields.io/github/languages/top/codenameone/CodenameOne?style=plastic)
![GitHub last commit](https://img.shields.io/github/last-commit/codenameone/CodenameOne?color=red&style=plastic)
<!-- [![Build Status](https://travis-ci.org/codenameone/CodenameOne.svg?branch=master)](https://travis-ci.org/codenameone/CodenameOne)
![GitHub language count](https://img.shields.io/github/languages/codenameone/CodenameOne?style=plastic)

<p>
<a href="https://twitter.com/CodenameOne"><img src="https://img.shields.io/badge/twitter-%231DA1F2.svg?&style=for-the-badge&logo=twitter&logoColor=white" height=25></a> 
<a href="https://medium.com/CodenameOne"><img src="https://img.shields.io/badge/medium-%2312100E.svg?&style=for-the-badge&logo=medium&logoColor=white" height=25></a> 
<a href="https://dev.to/codenameone"><img src="https://img.shields.io/badge/DEV.TO-%230A0A0A.svg?&style=for-the-badge&logo=dev-dot-to&logoColor=white" height=25></a> -->

Codename One is the only platform that...

- Has Write Once Run Anywhere support with no special hardware requirements and 100% code reuse
- Compiles Java or Kotlin into native code for iOS, UWP (Universal Windows Platform), Android and even JavaScript (with seamless PWA and Thread support)
- Is Open Source and Free with an enterprise grade commercial offering
- Is Easy to use with 100% portable Drag and Drop GUI builder
- Has Full access to underlying native OS capabilities using the native OS programming language (e.g. Objective-C) without compromising portability
- Has full control over every pixel on the screen! Just override paint and draw or use a glass pane to draw anywhere...
- Lets you use native widgets (views) and mix them with Codename One components within the same hierarchy (heavyweight/lightweight mixing)
- Supports seamless Continuous Integration out of the box


Here are some concrete benefits you can get with Codename One:

<img align="right" src="https://www.codenameone.com/img/github/content/runs-instantly.png" height="200">

### Codename One's Simulator Runs Instantly

Unlike emulators which you can see in Android etc. Codename One uses a simulator. This means it starts up fast even when debugging. You can enjoy IDE features such as live code reload to modify code in runtime etc.

This means faster debugging cycle and faster development process!

<img align="left" src="https://www.codenameone.com/img/github/content/large-selection-skins.png" height="200">

### Large Selection of Device Skins

Choose from a large selection of device “skins” to see how your app will look on particular devices. The skin takes into account factors such as resolution and
device density to provide a pixel-perfectre presentation of your app, as it would appear on the real device.
Switching between device skins is nearly instant.

You can edit and contribute skins in their own open source project [here](https://github.com/codenameone/codenameone-skins).

<img align="right" src="https://www.codenameone.com/img/github/content/interactive-console.png" height="200">

### Interactive Console

Interact with your application’s APIs at runtime using the interactive Groovy Console. Inspect the application state or experiment with changes all while the app is running. 

This lets you investigate issues and experiment without even the small overhead of recompiling.

<img align="left" src="https://www.codenameone.com/img/github/content/live-reload.png" height="200">

### Live Reload

The Simulator let’s you take advantage of the "Reload Changed Classes" feature in IntelliJ (named "Apply Code Changes" in NetBeans) so that changes you make in your Java source code will be applied immediately to your already-running app in the simulator.

Note that this is often superior to the interactive console but there are limitations such as the ability to add methods/change structure of the code. These limits don't apply to the interactive console!

<img align="right" src="https://www.codenameone.com/img/github/content/css-live-update.png" height="200">

### CSS Live Update

When you make changes to your app’s CSS stylesheet, the changes are reflected instantly in the simulator. This includes changing your theme, images, fonts etc. All changes are instantly refreshed on save, no need to reload/refresh or anything of the sort!

This makes the process of styling an application remarkably easy and fast.

<img align="left" src="https://www.codenameone.com/img/github/content/component-inspector.png" height="200">

### Component Inspector

Use the powerful component inspector to browse the UI component hierarchy in your app. 
This tool makes it easy to find out where that extra padding is coming from or why something just isn’t lining up the way you’d like. You can also change the UIID (selector) of a component in runtime to see how it impacts the UI and see which component in the hierarchy maps to an element in the component tree (DOM equivalent).

<img align="right" src="https://www.codenameone.com/img/github/content/network-monitor.png" height="200">

### Network Monitor

See all of the network connections that your app makes using the Network Monitor. This valuable tool comes in handy when you’re trying to figure out why an HTTP request isn’t working for you. Check the headers and bodies of both the request and the response. You can even throttle the network to simulate a slow network connection.

<img align="left" src="https://www.codenameone.com/img/github/content/record-ui-unit-tests.png" height="200">

### Record UI Unit Tests

Use the Test Recorder tool to record unit tests for your app. Once you start recording, it will save your interactions into a unit test that can be played back later to verify that behaviour remains correct.

You can then connect the recorded tests to your CI process including automated on device testing.

## How Does it Work?

[Codename One](https://www.codenameone.com/) is a mature open source project with roots dating back to Sun Microsystems (2006) where one of its core underlying components was developed and open sourced. You can learn about its history and how it works in [this video](https://www.youtube.com/watch?v=MrwbpdMALig).

Codename One apps perform like native apps, because they are real native apps.

They are statically compiled into native binaries using the target platform’s official build tools. 

On platforms that do not support Java natively, such as iOS, the app’s JVM bytecode is first transpiled into a form that the native build tools will accept. On iOS, the app’s JVM bytecode is transformed into C source code, in a real xcode project. On Android, since Java is supported natively, no such transformation is necessary. The app jar is bundled directly into an Android studio gradle project, which can be built directly using the Android SDK build tools.

The figure below shows the build process for each supported platform

<a href="https://www.codenameone.com/img/github/codename-one-architecture.jpg" target="_blank"><img width="70%" src="https://www.codenameone.com/img/github/codename-one-architecture.jpg"></a>

You can click the image to enlarge or view a PDF version [here](https://www.codenameone.com/img/github/architecture.pdf).


## Quick Start

TIP: We are currently transitioning to Maven, and have created a new, simpler method for creating projects.  Check out https://start.codenameone.com to get started now.

There is a lot to know about Codename One, this 3 minute video gives a very concise high level view. Notice there are similar videos for Eclipse, IntelliJ/IDEA and Netbeans [here](https://www.codenameone.com/download.html):

[![Hello Codename One](http://img.youtube.com/vi/rl6z7DD2-vg/0.jpg)](http://www.youtube.com/watch?v=rl6z7DD2-vg "Hello World Codename One")

## Extensible

Codename One can be extended easily using 3rd party libraries that can include native OS code. There is an extensive list of these libraries (cn1libs) [here](https://www.codenameone.com/cn1libs.html). The libraries list is generated automatically based on [this github project](https://github.com/codenameone/CodenameOneLibs/).

You can learn more about Codename One and its capabilities at the [main site](https://www.codenameone.com) and you can see an extensive list of documentation and tutorials [here](http://www.codenameone.com/blog/tutorials-resources-learn-java-mobile-videos-courses-ios-android.html).

## Important Links & Docs

You can get started with the binary and the birds eye view in the [download section](https://www.codenameone.com/download.html). Additional important links are:

- [JavaDoc](https://www.codenameone.com/javadoc/)
- Developer Guide - [HTML](https://www.codenameone.com/manual/) & [PDF](https://www.codenameone.com/files/developer-guide.pdf)
- [How Codename One Works](http://stackoverflow.com/questions/10639766/how-does-codename-one-work/10646336) (stackoverflow)
- [Codename One Academy](http://codenameone.teachable.com/)
- [Blog](https://www.codenameone.com/blog/)
- [Community Discussion Forum](https://www.codenameone.com/discussion-forum.html)
- [Using the Kotlin Support](https://www.codenameone.com/blog/kotlin-support-public-beta.html)


## Setup & Getting Started With The Code

NOTE: We are in the process of migrating from Ant to Maven, which simplifies the process for building from source.  This section still refers to the process for for building with Ant.  See [Maven Quick Start](#maven) for the new Maven build instructions.

Setup is covered in depth in [this article and video](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html). Notice that this covers debugging the simulator and working with the code that requires the Codename One plugin for NetBeans. You can install that by installing NetBeans and typing "Codename One" in the plugin search section see [the getting started tutorial](https://www.codenameone.com/getting-started.html).

[![Using The Codename One Source Code](http://img.youtube.com/vi/2nD75pODPWk/0.jpg)](http://www.youtube.com/watch?v=2nD75pODPWk "Using The Codename One Source Code")

While Codename One itself works with all major IDE's the code in this repository was designed to work with NetBeans.

<img src="http://codenameone.com/img/NetBeans-logo.png" width="120">

<img src="http://resources.jetbrains.com/storage/products/intellij-idea/img/meta/intellij-idea_logo_300x300.png" width="120">

<img src="http://codenameone.com/img/eclipse-logo.png" width="120">

Please notice that while we fully support IntelliJ/IDEA (both CE and Ultimate), we don't support Android Studio which diverted too much from the mainline IDE.

### Quick Start

**Getting and Building Sources**

~~~~
$ git clone https://github.com/codenameone/CodenameOne
$ cd CodenameOne
$ ant
~~~~

**Running Unit Tests**

~~~~
$ ant test-javase
~~~~

**Running Samples**

The Samples directory contains a growing set of sample applications.  These samples aren't meant to be demos, but rather samples of how to use APIs.

You can launch the sample runner app from the command-line using:

~~~
$ ant samples
~~~

<a id="maven">

### Quick Start with Maven

~~~~
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne/maven
mvn install
~~~~

This will build and install Codename One in your local Maven repository.

To build the archetype projects from source, you should check out the [cn1-maven-archetypes](https://github.com/shannah/cn1-maven-archetypes) repository and build it also:

~~~~
git clone https://github.com/shannah/cn1-maven-archetypes
cd cn1-maven-archetypes
mvn install
~~~~

To get started building projects using Maven, see https://start.codenameone.com.


## ParparVM
Codename One's iOS VM is quite unique and is open source as well. You can read more about it [in its dedicated folder in this repository](https://github.com/codenameone/CodenameOne/tree/master/vm).

ParparVM is a uniquely conservative VM that translates Java bytecode to C code. Thus providing native performance and access while still providing a safety net. This approach is unique to Codename One and is essential for future compatibility!

Apple has a tendency to change things abruptly e.g. 64bit support, bitcode etc. Since ParparVM generates a standard Xcode project there were no code changes required for any of these tectonic shifts. It's as if you handcoded the project yourself!

You can even open the resulting project in xcode and debug it or profile it directly on the iOS device. This provides a lot of useful information such as readable callstacks and valuable/actionable performance tracking...

Traditional compilers fall flat in these cases.

## Modified iKVM

Codename One maintains a fork of iKVM which is a JVM for CLR. This modified port allows us to run the Universal Windows Platform implementation of Codename One natively on Windows 10 devices.

## Getting Help & FAQ

<img align="right" src="http://codenameone.com/img/blog/new_icon.png" height="250">

We provide support over at [StackOverflow when you tag using codenameone](http://stackoverflow.com/tags/codenameone), you can ask anything there and we try to be pretty responsive. [The StackOverflow link](http://stackoverflow.com/tags/codenameone) also serves as an excellent community driven FAQ since it literally maps user questions to answers.

Codename One has a [discussion group](https://www.codenameone.com/discussion-forum.html) where you can post questions. However, due to the nature of that group we try to limit discussions over the source. The discussion forum is intended for simpler usage and more complex source code hacks/native compilation might create noise there.
