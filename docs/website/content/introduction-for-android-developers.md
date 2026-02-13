---
title: "Introduction For Android Developers"
date: 2016-10-12
slug: "introduction-for-android-developers"
---

# Android Developer Introduction To Codename One

Take your first steps in Codename One from an Android developer perspective

1. [Home](/)
2. [Developers](https://beta.codenameone.com/developing-in-codename-one.html)
3. Android Developers

**The content of this page is out of date!** please check a more thorough discussion and sample porting in [this post](/blog/port-native-android-app-ios-iphone-guide.html).

<iframe src="https://www.youtube.com/embed/fUhGYP0Oscc?rel=0" width="853" height="480" frameborder="0" allowfullscreen="allowfullscreen"></iframe>

Codename One allows Android developers familiar with Java and Eclipse (or NetBeans/IntelliJ) to instantly build native applications for Android, iOS, Windows and other platforms!

Codename One has the following additional advantages:

- A fast simulator
- Standard Java debugging and profiling tools
- Drag and drop development (optional)
- Resources are automatically scaled to the different DPI's in the tools
- No need to deal with manifests and permissions (Codename One automatically detects and applies the necessary permissions)
- Codename One hides the differences between versions on Android such as Gingerbread, Jellybean and Lollipop
- Resources can be downloaded dynamically and aren't compiled into the APK
- Component hierarchy (views) is much simpler in Codename One

To learn more about Codename One in general you can go to [our developer section](https://beta.codenameone.com/developing-in-codename-one.html). Read below for information of Codename One equivalents to common Android features.

## Layouts

In Android developers build their user interfaces in XML by nesting layout views and view elements one within another (you can write Android apps in code but that isn't as common). These elements are translated by the compiler and in order to bind code to them you need to "find" them by their ID to use them. Codename One differs in several ways:

- You can build Codename One applications either via the GUI builder (in which case you use a findXXX() method to locate the component) or write Java source code manually.
- Component is Codename One's equivalent of a view, Container derives from Component.
- All layouts are applied to the Container class essentially decoupling layout from Container hierarchy

E.g. Android's linear layout is essentially very similar to the box layout in Codename One. So to arrange components in a row in Codename One we can do something like:

<script src="http://pastebin.com/embed_js.php?i=AVXEg5dD"></script>

You can do the same with the GUI builder as well using simple drag and drop. To read more about the different types of layouts and how they can be nested/used you can follow the section on layout managers in the developer guide.

## Fragments

Fragments essentially solve an issue with Android which mixed Activities and Views in a hierarchy creating a somewhat complex scenario to support a very dynamic architecture. This concept is powerful, yet isn't portable, convenient or robust. Fragments become unnecessary because of the much simpler Codename One hierarchy.

## Multiple DPI's & 9-Patch Borders

Codename One supports a multi-image format which is roughly identical to the multiple DPI directories commonly used in Android. Unlike Android the Codename One designer tool automatically scales the images to all the resolutions (using high quality scaling algorithm) saving you the need to maintain multiple resolution support.

The same is true with Codename One's [9-part image borders](how-do-i---create-a-9-piece-image-border.html). However, unlike Androids very elaborate 9-patch borders Codename One borders are far simpler. Codename One uses tiling for border images (instead of scaling) and breaks the images down, it also includes an image cutting wizard and internally cuts the images to 9-pieces.

Android's 9-patch images are VERY powerful but also very hard to implement on all platforms in a performant way. They are also a very difficult concept to grasp for most developers.

## Activities

Codename One has its own lifecycle object which acts a bit more like an application than the activity does, its somewhat simpler to program to. Codename One supports firing intents although that is mostly Android specific and won't work on other platforms.

## Android Specific Features

You can invoke "native" Android code from Codename One without breaking the portability of Codename One. Follow the native code section in the developer guide to understand how this can be accomplished, you can also see the [how do I video tutorial on the subject](how-do-i---access-native-device-functionality-invoke-native-interfaces.html). You can also add things to the manifest etc. if required by your native code.
