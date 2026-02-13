---
title: "Build Tools"
date: 2020-08-31
slug: "build-tools"
---

## Codename One’s Build Tools

Codename One apps perform like native apps, because they are real native apps.

They are statically compiled into native binaries using the target platform’s official build tools.

On platforms that do not support Java natively, such as iOS, the app’s JVM bytecode is first transpiled into a form that the native build tools will accept. On iOS, the app’s JVM bytecode is transformed into C source code, in a real Xcode project.

On Android, since Java is supported natively, no such transformation is necessary. The app’s source code is bundled directly into an Android studio gradle project, which can be built directly using the Android SDK build tools.  
  
The figure below shows the build process for each supported platform.

APP.JAR UWP/Windows 10 Windows Desktop Mac Desktop Javascript Android IOS

### Transplantation Step

### IKVM

Translate JVM bytecode to .dll

a

## ded

## ded

### TeaVM

(Translate JVM bytecode to JavaScript source)

## ded

### ParparVM

(Translate JVM bytecode to C-Source)

a

### Build Step

### Visual Studio Project

a

### Java Packager

a

### Java Packager

a a

### Android Studio Gradle Project

a

### Xcode Project

a

### output

![](/uploads/Group-1106-svg.png)

### UWP App

(.appx)

![](/uploads/Group-1181-svg.png)

### Windows App

(.exe)

![](/uploads/3JtKV36@2x.png)

### Mac app

(.app)

![](/uploads/Group-1188-svg.png)

### Progressive Web App

(.war or .html)

![](/uploads/Group-1118-svg.png)

### Android App

(.apk)

![](/uploads/iTunes-ipa-4@2x.png)

### iOS App

(.ipa)

a a a a

### distribution

![](/uploads/g18086_1_-svg.png)

Windows App Store

![](/uploads/layer1_2_-svg.png)

Mac App Store

![](/uploads/Group-725-svg.png)

Google Play Store

![](/uploads/layer1_2_-1-svg.png)

iOS App Store

\[build\_tools\_process\_slider\]

## Setting up the Build Tools

You don’t need to set up any of these build tools. The Codename One Build cloud takes care of all that. If you have the IntelliJ/NetBeans/Eclipse plugin installed, you can build your Java or Kotlin project as a native mobile app with the press of a button. [Get Started](https://www.codenameone.com/getting-started.html)
