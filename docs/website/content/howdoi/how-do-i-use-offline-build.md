---
title: USE OFFLINE BUILD
slug: how-do-i-use-offline-build
url: /how-do-i/how-do-i-use-offline-build/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-use-offline-build.html
tags:
- pro
description: Build without the cloud build servers
youtube_id: IqsUSCgSVTo
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-18.jpg
---
{{< youtube "IqsUSCgSVTo" >}}
Offline build exists for the cases where the Codename One cloud build servers are not an option. That usually means regulated environments, restricted networks, or institutions that cannot send source or build assets to external services. If you are not in that situation, the normal build server is still the simpler and more heavily traveled path.

The important mental model is that offline build is not a full replacement for your local native toolchain. It reproduces the translation and project-generation parts of the Codename One build process on your own machine, then hands you native projects that you continue with in Xcode or Android Studio. In other words, offline build gets you from a Codename One project to platform-native project output without using the cloud.

That means the toolchain requirements matter a lot. You need the native tools installed and working, and you need versions that are compatible with the builder snapshot you are using. The exact list changes over time, so the current [Developer Guide](/developer-guide/) should be treated as the source of truth instead of the version numbers mentioned in the old video.

The video is also a product of an older setup era, so the specific versions it names for Gradle, Xcode, and related tools should be treated as historical. The durable lessons are different. First, offline build is operationally heavier than cloud build. Second, it depends on more moving parts on your own machine. Third, once you do generate the native projects, you debug and compile them with the platform-native tools exactly as you would expect.

A useful way to think about offline builders is as locally installed snapshots of Codename One build logic. You choose which builder version to use, and that choice affects the generated native output. That makes offline build closely related to repeatable build concerns: if one builder version works and a newer one regresses, you can stay on the working snapshot while you investigate.

For Android, the flow is generally: generate the offline Android build, open the resulting native project in Android Studio, make sure the local Gradle/toolchain configuration is correct, and then run or debug it there. For iOS, the flow is similar but more sensitive to the Apple toolchain. You generate the iOS offline build, open the resulting `xcworkspace` in Xcode rather than the bare project file, and continue from there.

The generated directories should be treated as build artifacts, not hand-maintained source trees. If you need to preserve a particular generated native project for investigation, copy it somewhere safe before generating another one. The video calls this out, and it is still an important practical point because regeneration can replace previous output.

The main reason to choose offline build is policy, not convenience. It is usually more complex than the cloud build system, more dependent on local toolchain setup, and more prone to environment-specific problems. But if you are in an environment where cloud builds are not possible, it gives you a workable path to native project generation while keeping the rest of the Codename One development model intact.

## Further Reading

- [Build Server](/build-server/)
- [Developer Guide](/developer-guide/)
- [How Do I Use The Include Sources Feature To Debug The Native Code On iOS/Android Etc.](/how-do-i/how-do-i-use-the-include-sources-feature-to-debug-the-native-code-on-iosandroid-etc/)
- [How Do I Get Repeatable Builds?](/how-do-i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
