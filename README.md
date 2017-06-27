# Codename One

## Write Once Run Anywhere Native Mobile Apps for Java Developers

<img align="right" src="https://www.codenameone.com/img/iphone-and-android.png" height="250">

[Codename One](https://www.codenameone.com/) is a mobile cross platform environment for Java developers. It can compile Java bytecode to native OS executables (iOS, Android, UWP etc.). 
It is a complete mobile platform featuring virtual machines, simulator, design tools (visual theme/builder), IDE integrations, ports to multiple OS's and much more. It provides full access to the underlying native OS code (e.g. Objective-C, C#, Dalvik/ART) thru a portable abstraction which enables 100% code reuse.

Codename One is the only platform that...

- Has Write Once Run Anywhere support with no special hardware requirements and 100% code reuse
- Compiles Java into native code for iOS, UWP (Universal Windows Platform), Android & even JavaScript
- Is Open Source & Free with an enterprise grade commercial offering
- Is Easy to use with 100% portable Drag & Drop GUI builder
- Has Full access to underlying native OS capabilities using the native OS programming language (e.g. Objective-C) without compromising portability
- Lets you use native widgets (views) and mix them with Codename One components within the same hierarchy (heavyweight/lightweight mixing)

[Codename One](https://www.codenameone.com/) is a mature open source project with roots dating back to Sun Microsystems (2006) where one of its core underlying components was developed and open sourced. You can learn about its history and how it works in [this video](https://www.youtube.com/watch?v=EMRmo6ZRnGw).

## Quick Start

There is a lot to know about Codename One, if you just want to see a 3 minute video that explains how to use it check this out. Notice there are similar videos for Eclipse, IntelliJ/IDEA [here](https://www.codenameone.com/download.html):

[![Hello Codename One](http://img.youtube.com/vi/73d65cvyQv4/0.jpg)](http://www.youtube.com/watch?v=73d65cvyQv4 "Hello World Codename One")

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


## Setup & Getting Started With The Code

Setup is covered in depth in [this article and video](https://www.codenameone.com/blog/how-to-use-the-codename-one-sources.html). Notice that this covers debugging the simulator and working with the code that requires the Codename One plugin for NetBeans. You can install that by installing NetBeans and typing "Codename One" in the plugin search section see [the getting started tutorial](https://www.codenameone.com/getting-started.html).

[![Using The Codename One Source Code](http://img.youtube.com/vi/2nD75pODPWk/0.jpg)](http://www.youtube.com/watch?v=2nD75pODPWk "Using The Codename One Source Code")

While Codename One itself works with all major IDE's the code in this repository was designed to work with NetBeans.

<img src="http://codenameone.com/img/NetBeans-logo.png" width="120">

<img src="http://codenameone.com/img/intellij_idea-logo.png" width="120">

<img src="http://codenameone.com/img/eclipse-logo.png" width="120">

## ParparVM
Codename One's iOS VM is quite unique and is open source as well. You can read more about it [in its dedicated folder in this repository](https://github.com/codenameone/CodenameOne/tree/master/vm).

ParparVM is a uniquely conservative VM that translates Java bytecode to C code. Thus providing native performance and access while still providing a safety net. This approach is unique to Codename One and is essential for future compatibility!

Apple has a tendency to change things abruptly e.g. 64bit support, bitcode etc. Since ParparVM just generates a standard Xcode project there were no code changes required for any of these tectonic shifts. It's as if you handcoded the project yourself!

You can even open the resulting project in xcode and debug it or profile it directly on the iOS device. This provides a lot of useful information such as readable callstacks and valuable/actionable performance tracking...

Traditional compilers fall flat in these cases.

## Modified iKVM

Codename One maintains a fork of iKVM which is a JVM for CLR. This modified port allows us to run the Universal Windows Platform implementation of Codename One natively on Windows 10 devices. 

## Getting Help & FAQ

<img align="right" src="http://codenameone.com/img/blog/new_icon.png" height="250">

We provide support over at [StackOverflow when you tag using codenameone](http://stackoverflow.com/tags/codenameone), you can ask anything there and we try to be pretty responsive. [The StackOverflow link](http://stackoverflow.com/tags/codenameone) also serves as an excellent community driven FAQ since it literally maps user questions to answers.

Codename One has a [discussion group](https://www.codenameone.com/discussion-forum.html) where you can post questions. However, due to the nature of that group we try to limit discussions over the source. The discussion forum is intended for simpler usage and more complex source code hacks/native compilation might create noise there.
