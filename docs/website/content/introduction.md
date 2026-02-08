---
title: "Introduction"
date: 2020-09-02
slug: "introduction"
---

- Home
- Introduction

## Introduction

Part of what makes Codename One unique is its architecture. You write your app in Java or Kotlin, using the Codename One cross-platform, extensible class library, which includes a rich set of UI widgets, and APIs for nearly everything a mobile app could need. Location APIs, Contacts, Maps, Audio/Video, and more. ![](/uploads/Group-2326.svg) ![Codename One Architecture](/uploads/Group-1851.png)

## Full Access to Native  
Platform and Libraries

Codename One also allows you full access to each platform’s native APIs via native interfaces. Native interfaces are easy to create in Codename One. You just create a Java interface that extends Native Interface, then use the Codename One plugin to generate stubs for the native implementation.

Then you just fill in those stubs with your native code. Using native interfaces, you can even integrate native UI widgets into your app seamlessly.

[Learn more about Native Interfaces](https://codenameone.com/how_di_i/access-native-device-functionality-invoke-native-interfaces.html) ![](/uploads/Group-1691.svg)

## Codename One Runtime

The Codename One runtime

consists of two layers:

## The Java class library

This includes a subset of the JavaSE 8 classes (e.g. java.util, java.lang, java.io, etc…) a set of UI widgets, as well as other packages for accessing device functionality such as location, media, contacts,and maps.

## The native layer

This layer, written in the platform’s native language (Objective-C on iOS, Java on Android, C# for UWP, etc..), is the glue that allows the Codename One class library to access native APIs. ![](/uploads/Group-2273.png)

## 3rd-Party Libraries

You can also make use of 3rd-party native libraries via Native interfaces. You can embed native libraries directly in your project’s “native” directory, or you can use build hints to add dependencies in cocoapods (iOS), gradle (Android), and Nuget (UWP).

There is already a wide assortment of cn1libs developed by both Codename One and third-party developers. Some notable “cn1libs” include Google Maps support, Websocket Support, and Parse support.

[Learn More about CN1libs](https://codenameone.com/cn1libs.html) iOS Architecture ![](/uploads/Group-1563.svg) Android Architecture

[![](/uploads/android-architecture-.svg)](https://codenameone.com/wp-content/uploads/2020/09/android-architecture-.svg)

UWP Architecture

[![](/uploads/UWP-Architecture-.svg)](https://codenameone.com/wp-content/uploads/2020/09/UWP-Architecture-.svg)

Javascript Port Architecture

[![](/uploads/Javascript-Port-Architecture-.svg)](https://codenameone.com/wp-content/uploads/2020/09/Javascript-Port-Architecture-.svg)

## Compiled to Native

And the best part of Codename One apps  
is that they are compiled into 100% native  
code to be blazingly fast. [Get Started](#) [Learn More](https://help.codenameone.com/en-us/article/whats-codename-one-how-does-it-work-1343oho/)
