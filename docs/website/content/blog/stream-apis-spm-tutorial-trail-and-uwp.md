---
title: Stream APIs, Swift Package Manager, Tutorial Trail Refresh, and the UWP Transition
date: '2026-03-20'
author: Shai Almog
slug: stream-apis-spm-tutorial-trail-and-uwp
url: /blog/stream-apis-spm-tutorial-trail-and-uwp/
description: Codename One now includes built-in java.util.stream support, adds Swift Package Manager support for iOS dependencies, refreshes the written tutorial trail, and removes the UWP target from the Maven plugin in 7.0.229.
feed_html: '<img src="https://www.codenameone.com/blog/stream-apis-spm-tutorial-trail-and-uwp.jpg" alt="Stream APIs, Swift Package Manager, Tutorial Trail Refresh, and the UWP Transition" /> Codename One now includes built-in java.util.stream support, adds Swift Package Manager support for iOS dependencies, refreshes the written tutorial trail, and removes the UWP target from the Maven plugin in 7.0.229.'
---

![Stream APIs, Swift Package Manager, Tutorial Trail Refresh, and the UWP Transition](/blog/stream-apis-spm-tutorial-trail-and-uwp.jpg)

There are a few updates in Codename One `7.0.229` that are worth discussing together because they all point in the same direction: less legacy friction, more modern Java and iOS workflows, and documentation that reflects how we actually build projects today.

## Built-in Support for Java Stream APIs

One of the long standing gaps in our Java compatibility story was `java.util.stream`.

We have supported Java 8 language features for a long time, but streams remained one of those APIs that developers would miss the moment they tried to bring modern Java habits or shared code into a Codename One project. That changes now.

We now ship built-in support for the core stream API surface, including operations such as `filter()`, `map()`, `sorted()`, `distinct()`, `limit()`, `skip()`, `reduce()`, `collect()`, `count()`, and predicate checks such as `anyMatch()` and `allMatch()`. We also include the basic `Collectors` helpers such as `toList()` and `joining()`.

Here is a simple example:

```java
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

List<String> names = Stream.of("Shai", "Steve", "Chen", "Shai")
        .distinct()
        .filter(name -> name.length() >= 4)
        .map(String::toUpperCase)
        .sorted()
        .collect(Collectors.toList());
```

And another example:

```java
import java.util.stream.Collectors;
import java.util.stream.Stream;

String platforms = Stream.of("iOS", "Android", "Desktop", "Web")
        .filter(name -> !name.equals("Desktop"))
        .collect(Collectors.joining(", "));
```

Why does this matter?

First, it makes normal Java code much more portable into Codename One. A lot of server side utilities, shared business logic and even ordinary everyday examples now read the way modern Java developers expect them to read.

Second, streams are not just shorter syntax. They let us express data transformation more clearly. Filtering, mapping and collecting data is something we do all the time in UI code, networking code and data preparation. Replacing hand-written loops with a more declarative pipeline makes code easier to read and often easier to maintain.

Third, this removes one of the obvious "yes, but..." answers when discussing modern Java support in Codename One. That is an important milestone.

## Swift Package Manager Support

The other big update is that we added Swift Package Manager support to Codename One, and documented it in the [Working with iOS section of the developer guide](/developer-guide/#_using_swift_package_manager).

The first thing to understand is that for existing projects, nothing should suddenly change. The default behavior is the compatibility mode, `auto`, so older projects that already use CocoaPods should just keep working.

For non-native developers, a bit of background helps here.

CocoaPods and Swift Package Manager are both dependency managers for iOS/macOS native libraries. Historically, a lot of Apple ecosystem libraries were distributed with CocoaPods instructions, so that became our integration path too. But CocoaPods is now effectively in maintenance mode, while Apple is pushing the ecosystem toward Swift Package Manager. That means the long term path forward is SPM.

Also, despite the name, Swift Package Manager is not "only for Swift code". It can manage dependencies for Swift packages, but those packages can expose Swift code, Objective-C code, or mixed native code. From a Codename One developer perspective this is about how native iOS dependencies are resolved and linked. It does not mean your project suddenly needs to become a Swift project.

The important build hint is:

```properties
ios.dependencyManager=auto
```

In practice, this is the mode most developers should start with because it preserves backward compatibility:

- If you only define `ios.pods`, we use CocoaPods.
- If you only define `ios.spm.*`, we use SPM.
- If you define both families of hints, both are applied.

We support four modes:

- `auto` is the default compatibility mode and should make existing projects just work.
- `both` applies both CocoaPods and Swift Package Manager. This is the practical migration mode.
- `cocoapods` forces CocoaPods only.
- `spm` forces Swift Package Manager only.

The `both` mode matters because migration is rarely a one-step event. One native dependency may still document CocoaPods while another has already moved to SPM. Supporting both lets you migrate incrementally instead of blocking on the slowest vendor.

Here is an SPM-only setup:

```properties
ios.dependencyManager=spm
ios.spm.packages=swift-collections|https://github.com/apple/swift-collections.git|from:1.1.0
ios.spm.products.swift-collections=Collections
```

The package syntax is:

```text
<identity>|<url>|<requirement>
```

Supported requirement formats include `from:`, `exact:`, `branch:`, `revision:` and `range:`.

It also helps to think of the SPM hints as the rough equivalent of the CocoaPods hints:

| CocoaPods | Swift Package Manager | What it means |
| --- | --- | --- |
| `ios.pods=GoogleMaps` | `ios.spm.products.googlemaps=GoogleMaps` | The native library or product you want linked |
| Pod source declaration in the Podfile / `ios.pods.sources` when needed | Package URL inside `ios.spm.packages` | Where the dependency comes from |
| Pod version expression inside `ios.pods` | Requirement inside `ios.spm.packages` | How the dependency version is selected |
| `ios.pods.platform=...` | Package requirement and normal Xcode/SPM resolution | Platform/version compatibility constraints |

The mapping is not one-to-one because CocoaPods thinks in terms of pods and pod sources, while SPM thinks in terms of packages and exported products. But conceptually they solve the same problem: bringing an external native dependency into the generated iOS project.

Another important detail is debugging. As documented in the [native source debugging section](/developer-guide/#_on_device_debugging), you should now open the `.xcworkspace` whenever it is generated, not just for CocoaPods builds. That workspace is no longer exclusive to CocoaPods. It can also be the right entry point for SPM-based or mixed dependency setups.

## Tutorial Trail Refresh

Over March 15-17, 2026 we refreshed a large portion of the tutorial trail and revised the written text across the course material.

This is important because some of the original videos are still valuable, but they naturally show the tooling and assumptions of their time. The written lessons now do a better job of acting as the current source of truth for the modern Codename One workflow, especially around Maven, Initializr, CSS and current project structure.

The course hubs are here:

- [Java for Mobile Devices](/course-01-java-for-mobile-devices/)
- [Deep Dive into Mobile Development with Codename One](/course-02-deep-dive-mobile-development-with-codename-one/)
- [Build Real World Full Stack Mobile Apps in Java](/course-03-build-real-world-full-stack-mobile-apps-java/)

If you want examples of the revised text style, have a look at:

- [Introduction](/courses/course-01-java-for-mobile-devices/001-introduction/)
- [Threading and the EDT](/courses/course-01-java-for-mobile-devices/011-threading-and-the-edt/)
- [41. Post Image and Video from NewPostForm](/courses/course-03-build-real-world-full-stack-mobile-apps-java/118-41-post-image-and-video-from-newpostform/)

I like this direction because it treats the older video material honestly. We are not trying to pretend nothing changed. We are preserving the conceptual value of the material while updating the written trail so new developers are not pushed toward obsolete setup steps.

## UWP Removal from the Maven Plugin

With Codename One `7.0.229`, released on March 20, 2026, we are officially removing the UWP target from the Maven plugin.

This is really an acknowledgment of reality more than a sudden functional change.

The UWP port has already been treated as historical in the updated documentation, including the [developer guide](/developer-guide/), the [historical UWP chapter](/developer-guide/#_working_with_uwp), and the related notes in push documentation. New Maven templates and current project generation flows should not keep advertising a target that we no longer consider part of the supported day to day path.

That said, it is important to highlight one subtle point: the UWP build servers are still functioning for legacy cases. So if you have an older codebase and you absolutely need to keep it alive, this is not a statement that those servers suddenly vanished overnight.

But for current work, the better answer on Windows is the desktop Windows build. It fits the present Codename One workflow much better, it is the path we actively support, and for most developers it is simply the more practical alternative.

This is exactly the sort of cleanup we need to keep making. Carrying old targets indefinitely in the main tooling creates confusion for new developers and false expectations for existing ones. Preserving legacy infrastructure where practical is good. Presenting that legacy path as a first-class modern target is not.

## Closing Thoughts

The theme across all of these changes is pretty simple.

We are making Codename One more comfortable for modern Java development, more aligned with the current iOS ecosystem, more honest in the documentation, and more focused in the supported build targets.

That is the right direction.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
