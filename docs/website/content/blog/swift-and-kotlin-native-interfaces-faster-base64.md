---
title: Swift and Kotlin Native Interfaces, Faster Base64, and More
date: '2026-04-10'
author: Shai Almog
slug: swift-and-kotlin-native-interfaces-faster-base64
url: /blog/swift-and-kotlin-native-interfaces-faster-base64/
description: The biggest change this week is that native interfaces now support Swift on iOS and Kotlin on Android.
feed_html: '<img src="https://www.codenameone.com/blog/swift-and-kotlin-native-interfaces-faster-base64.jpg" alt="Swift and Kotlin Native Interfaces, Faster Base64" /> The biggest change this week is that native interfaces now support Swift on iOS and Kotlin on Android.'
---

![Swift and Kotlin Native Interfaces, Faster Base64](/blog/swift-and-kotlin-native-interfaces-faster-base64.jpg)

The biggest change this week is that [native interfaces now support Swift on iOS and Kotlin on Android](https://github.com/codenameone/CodenameOne/issues/3274).

This is still highly experimental like most of the new features you read about in these posts. The main value here is simplified integration of native code which more often is coming with Kotlin or Swift sample code.

## Native Interfaces Now Support Swift and Kotlin

Native interfaces are one of the most important features in Codename One. They guarantee you won't be stuck if you need something that we can't/won't deliver. They let our community extend Codename One in ways we couldn't possibly imagine.

Native interfaces let us keep the application in shared Java code, while still reaching into native APIs when we need platform-specific functionality. 

Until now, the generated implementation flow was based on:

- **Java** for Android (with the additional Android native APIs)
- **Objective-C** for iOS

Both still work. Both are still supported. Both are still the default.

Some Android developers work in Kotlin. iOS developers increasingly work in Swift. Supporting those languages in native interfaces was a long time request in our issue tracker.

Native interface implementations can now be written as:

- **Java or Kotlin** on Android
- **Objective-C or Swift** on iOS

That makes the feature much more natural to use in real-world projects.

### Typical File Locations

| Platform | Typical file locations for native interface implementations |
| --- | --- |
| Android (Java/Kotlin) | `android/src/main/java/com/mycompany/myapp/MyNativeImpl.java`<br>`android/src/main/java/com/mycompany/myapp/MyNativeImpl.kt` |
| iOS (Objective-C/Swift) | `ios/src/main/objectivec/com_mycompany_myapp_MyNativeImpl.h`<br>`ios/src/main/objectivec/com_mycompany_myapp_MyNativeImpl.m`<br>`ios/src/main/objectivec/com_mycompany_myapp_MyNativeImpl.swift` |

By default, the generation process still produces the traditional Java and Objective-C stubs. That is intentional. We do not want to disrupt existing projects or workflows.

But if you want Swift and Kotlin stubs instead, you can enable them explicitly:

```bash
mvn cn1:generate-native-interfaces \
  -Dcn1.generateNativeInterfaces.swift=true \
  -Dcn1.generateNativeInterfaces.kotlin=true
```

That keeps the default conservative, while offering support for newer languages.

### Swift on iOS

On iOS, the "Generate Native Sources" tool still produces the Objective-C files you would expect:

- `com_mycompany_myapp_MyNativeImpl.h`
- `com_mycompany_myapp_MyNativeImpl.m`

If Swift generation is enabled, it will produce:

- `com_mycompany_myapp_MyNativeImpl.swift`

The main detail to keep in mind is that the runtime still expects the same implementation naming convention. So if you implement the class in Swift, you should keep the generated implementation name and annotate it with `@objc(...)` so the runtime can discover it properly.

That's an important detail that gives us the best of both worlds. The runtime model stays stable, while developers get to write the native side in Swift instead of Objective-C.

### Kotlin on Android

The Android side is even more straightforward.

You can still implement the generated class in Java:

```text
android/src/main/java/com/mycompany/myapp/MyNativeImpl.java
```

Or you can now implement it in Kotlin:

```text
android/src/main/java/com/mycompany/myapp/MyNativeImpl.kt
```

Some Android APIs, SDK samples, and documentation are Kotlin-first now. When you are integrating something native, it might be the path of least resistance.

Kotlin support makes native interfaces feel much more at home in the Android ecosystem.

As a sidenote, native Android Java code should also support JDK 17 level syntax with current builds. This is something we introduced a while back, but it didn't make it to the blog because we couldn't find the relevant issue.

## Base64 Performance Improved Dramatically

The other major item in this update is Base64 performance (and performance in general).

Base64 sits in all kinds of important paths: encoded assets, payload handling, persistence, transport, auth flows, and platform bridges. If it is slow, you feel it in places that are hard to diagnose and annoying to optimize.

We improved both **encoding** and **decoding** on **Android** and **iOS**.

This includes a new API that lets developers **reuse a buffer**, which helps reduce GC thrashing in hot paths. That is useful if you are tuning performance-sensitive code directly.

But even if you do nothing and just use the API the way you always did, performance still improves significantly.

### Android Results

On Android, the old implementation was behind native performance, especially for decoding.

Before these improvements:

- encode was **7.8% slower**
- decode was **112.3% slower**

After these improvements:

- encode is **73.4% faster**
- decode is **61.3% faster**

That is a very significant swing, especially on decoding. But I especially love how our API beats the pants off of Android's native API...

### iOS Results

On iOS, the gap was even larger.

Before these improvements:

- encode was **245.1% slower**
- decode was **319.6% slower**

After these improvements:

- encode is now **39.7% slower**
- decode is **98.9% slower**

That is still not parity, but it is a massive improvement.

The interesting part is that to get to this performance boost, we improved some APIs in ParparVM that should impact the entire application. The biggest change was a significant improvement to array access logic which should make looping over arrays MUCH faster.

It is also one of those cases where platform internals matter a lot. Base64 on iOS is a very heavily optimized native capability. It likely uses low-level implementation tricks and probably leans on SIMD support in ways we do not currently match.

Our current goal on this front is to beat the native iOS performance too, which we plan to achieve with SIMD support in ParparVM. We have some work in progress on that front which I hope will land soon. 

I think ParparVM should outperform "native" Objective-C/Swift since it doesn't have the overhead of ARC or message passing. It does have the GC overhead, but that shouldn't impact most applications since our gc is concurrent. Based on these results, we're not there yet.

## Playground Keeps Getting Better

The new [Playground](/playground/) is still in the early stages, but it is already becoming more useful.

This update includes slightly improved theming, and a download button that lets you take a Playground project and continue experimenting with it inside the IDE.

That is a small addition with a lot of practical value.

A playground is great for trying ideas quickly, but good experiments tend to stop being experiments. Sometimes a quick prototype turns into a real component, a test case, a support sample, or the start of a feature. Being able to move that work into the IDE directly makes the Playground much more than a toy.

The Java language support in Playground and its design are currently our top priorities.

## Sheet Now Supports a Title Component API

`Sheet` now supports a proper title component API.

That gives us a much better way to build richer sheet headers instead of treating the title as just a string.

For example:

```java
private Sheet createSheet(Sheet parent, String title) {
    Sheet newSheet = new Sheet(parent, title);
    Label titleIcon = new Label();
    FontImage.setMaterialIcon(titleIcon, FontImage.MATERIAL_IMAGE, 3f);
    titleIcon.setUIID("SheetTitleIcon");
    Label titleLabel = new Label(title);
    titleLabel.setUIID("SheetTitleText");
    newSheet.setTitleComponent(BoxLayout.encloseYCenter(titleIcon, titleLabel));
    Container content = newSheet.getContentPane();
    content.setLayout(BoxLayout.y());
    content.add(new Label("Sheet content"));
    content.add(new Button("Primary Action"));
    content.add(new Label("Secondary details"));
    newSheet.getCommandsContainer().add(new Button("Edit"));
    return newSheet;
}
```

This is the sort of API that gives you more control without making the component harder to use.

You can add icons, custom styling, better hierarchy, and a more polished presentation without resorting to hacks. That makes `Sheet` more flexible in the places where visual structure really matters.

## Native Simulator Fonts Are More Correct

We also improved the way `native:*` fonts behave in the Java SE simulator.

This is one of those areas where "close enough" often isn’t actually close enough. Typography affects spacing, wrapping, alignment, and general visual feel. If the simulator is using the wrong font family, the UI can look right enough to pass casual inspection but still differ in exactly the places that matter on the device.

The simulator now tries to use installed iOS-family fonts when running with an iOS simulator skin:

- **San Francisco / SF Pro** first
- then **Helvetica Neue**

If those fonts are not available on the host machine, it falls back to bundled **Roboto** fonts so behavior remains predictable.

That gives us a better approximation of real iOS rendering without making the simulator fragile.

### Can Codename One Bundle Apple's Fonts?

No.

Codename One does not bundle Apple’s proprietary fonts.

If you want exact iOS typography on Windows or Linux, you will need to install those fonts separately under your own Apple license terms.

In practice:

- **macOS** users will usually already have the relevant fonts
- **Windows/Linux** users can install San Francisco / SF Pro through [Apple’s official channels](https://developer.apple.com/fonts/)
- if the fonts are not installed, Roboto fallback still works, but text metrics may differ from real iOS devices

This is the right compromise. Better fidelity where possible, safe fallback where necessary.

## `Dialog.show()` Can Now Use `InteractionDialog` Mode

Dialogs are useful, but they have always been awkward around `PeerComponent` content.

If your app uses things like:

- `MapContainer`
- Browser components
- Video
- Other native peer-backed views

Then you have probably run into situations where a normal `Dialog` can trigger odd behaviors such as the background suddenly turning white.

This happens because a `Dialog` is technically a separate `Form` that grabs a screenshot of the previous `Form` and shows it in the background. That works well for simple cases, but since native components are rendered in a separate thread/ hierarchy, we can't reliably grab a screenshot of them.

The workaround is to avoid `Dialog` altogether in such cases. We have `InteractionDialog` which relies on layered panes to provide the same experience as a `Dialog` and even allows "floating dialog" behavior (hence the "interaction" part). The problem is that `Dialog.show()` variants are common in the code and very convenient to use.

To address that, dialogs now support a mode where `Dialog.show()` can use an **InteractionDialog** instead.

You can enable it globally with:

```java
Dialog.setDefaultInteractionDialogMode(true);
```

Or through the theme hint:

```text
defaultInteractionDialogModeBool=true
```

This is especially relevant for apps that lean heavily on maps, browser content, or native-embedded views in general.

If that sounds like your app, this is probably worth enabling.

## Bug Fixes and Enhancements of Note

A few smaller fixes in this update are worth mentioning too:

- Tapping the status bar on iOS should finally scroll to the top correctly
- Fixed a ToastBar regression in which extra padding was added at the top
- Material icons in `Tabs` now update their color to match the label color
- Javadocs now include the Java APIs we support again, not just the Codename One APIs


## Closing Thoughts

Thanks to those who took the time to annotate the issues that they found important in the issue tracker. Despite adding new RFEs this week were still able to reduce the issue count to 499, which is fantastic. 

If you submitted an issue in the past, please go to the [issue tracker](https://github.com/codenameone/CodenameOne/issues) and filter based on your account. Make sure that all your issues are still applicable. Close what's irrelevant and comment on what isn't. We're doing that work ourselves, and it's a slow-tedious task.

Thank you!

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}