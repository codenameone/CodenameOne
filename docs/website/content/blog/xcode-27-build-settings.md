---
title: "Xcode 27: The Build Settings That Can Stop a Release"
slug: xcode-27-build-settings
url: /blog/xcode-27-build-settings/
date: '2026-09-22'
author: Shai Almog
description: "Codename One supports Xcode 27 builds with SDK-derived deployment floors and checks for iOS 27 launch-screen and scene-lifecycle requirements."
feed_html: '<img src="https://www.codenameone.com/blog/xcode-27-build-settings.jpg" alt="Getting the next iOS build through" /> Deployment floors, launch metadata, and scene lifecycle in Xcode 27 builds.'
series: ["release-2026-09-18"]
---

![Getting the next iOS build through](/blog/xcode-27-build-settings.jpg)

An unchanged Java app can stop building because a number inside its generated Xcode project is now too low. No new API call, no changed screen, just a deployment target the new SDK refuses to accept.

That was the first problem in our Xcode 27 work. Launch-screen metadata and scene lifecycle supplied the next two. [This week's release](/blog/why-another-java-server/) handles all three in the builder, where an application team should be able to expect them to work.

## Ask the SDK what it accepts

Xcode 27 raised several minimum deployment targets. The recorded side-by-side SDK check in [PR #5788](https://github.com/codenameone/CodenameOne/pull/5788) found these lower bounds:

| Platform | Xcode 26 SDK floor | Xcode 27 SDK floor |
| --- | ---: | ---: |
| iOS | 12.0 | 15.0 |
| tvOS | 12.0 | 15.0 |
| macOS | 10.13 | 12.0 |
| Mac Catalyst, iOS target | 12.0 | 15.0 |
| watchOS | 4.0 | 9.0 |

These are SDK deployment floors, not the operating system version you must set as your application's target SDK. Our watchOS minimum already cleared the new floor. Several other generated targets did not.

The builder now asks the selected SDK for its floor and combines it with the application's requested minimum and the minimums required by its features. A lower project setting gets raised to a supported value, with a message in the build log.

{{< mermaid >}}
flowchart TD
    SDK[Selected SDK deployment floor] --> Max[Choose the highest required minimum]
    App[Application minimum] --> Max
    Features[Feature and extension minimums] --> Max
    Max --> Project[Generated Xcode targets]
    Project --> Build[Compile and package]
    Build --> Plist[Validate finished launch metadata]
{{< /mermaid >}}

Using the SDK's answer matters more than adding another version to a table. It also covers a future SDK whose floor we haven't hardcoded yet.

## Delete the three removed build hints

[PR #5855](https://github.com/codenameone/CodenameOne/pull/5855) removes three build hints. If your project supplies any of these names, the builder stops with an error explaining what replaced it:

| Remove from `codenameone_settings.properties` | What to use now |
| --- | --- |
| `codename1.arg.ios.generateSplashScreens` | The generated `UILaunchScreen`, with `Launch.Foreground.png` for a custom image. |
| `codename1.arg.ios.uiscene` | The UIScene lifecycle is always enabled; no selector is needed. |
| `codename1.arg.ios.launchStoryboardName` | For a custom launch declaration, use `ios.plistInject` and test it on your target devices. |

**Delete the entries entirely.** Changing `ios.uiscene=false` to `ios.uiscene=true` still fails. The check rejects the hint name, regardless of its value or the selected SDK. An iOS 26 SDK build is subject to the same check.

The splash hint originally selected the legacy `Default*.png` generator. Before its removal, it could still suppress normal launch metadata even though that generator was gone. It now causes a build failure, with no retained iPad multitasking effect.

For the default launch screen, put your image at `ios/src/main/resources/Launch.Foreground.png`. The [iOS guide](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/docs/developer-guide/Working-With-iOS.asciidoc) explains the generated launch screen and custom launch declarations. The builder generates the scene lifecycle for every iOS build. If your app depended on the old lifecycle, report the incompatibility so we can fix it.

## Check the finished launch metadata

A separate check reads the **finished `Info.plist`** for builds linked against iOS SDK 27 or later. It requires a supported root launch declaration and `UIApplicationSceneManifest`. This check uses the SDK the build actually links against; the removed-hint check above applies to every SDK.

Checking the finished document matters when an application uses `ios.plistInject`: finding a key's name in a comment or in the wrong dictionary doesn't mean the app declared it at the root. The builder accepts the supported launch-key forms, including an application-supplied launch experience. A full-screen setting doesn't substitute for a launch screen.

You can inspect a built app's metadata locally:

```bash
plutil -p /path/to/YourApp.app/Info.plist
```

Look for the root launch declaration and `UIApplicationSceneManifest`. A valid plist can still have the wrong structure for a platform requirement, which is why the builder checks the completed document as well as producing it.

## If App Review rejects an older project

Some developers have encountered rejections tied to older minimum-version settings. We've updated the generated minimums and added the launch checks, but existing apps have many combinations of build hints, extensions, and custom plist content.

Rebuild with the updated tools. Check the SDK selected in the build log, the effective deployment targets, and whether any of the three removed hints remain in your project. If the submission is still rejected, send us the rejection text, relevant build hints, and the build log identifying the toolchain. That gives us the actual configuration to fix.

As of the September 18 release, **Xcode 27 is supported by the builders but isn't installed on our cloud build servers**. We're waiting for further Apple updates before deciding how to proceed with the server rollout. Local builder support and the cloud toolchain rollout are separate steps.

## Next comes the screen itself

With iOS 27 released, we'll work on device-theme fidelity over the next couple of weeks. We'd also like to add deeper integration with iPhone Duo's folding behavior.

[Apple's tooling update](https://developer.apple.com/news/?id=rfb1rooi) says Xcode 27.1 will add a simulator for Duo's poses and orientations. That is the environment we need to exercise the layout transitions, rather than treating folding as a pair of unrelated screen sizes. The folding work is ahead of us; it isn't part of this builder release.

Codename One gives you control over the UI you ship. Matching new platform conventions should be a deliberate update you can test, alongside the native lifecycle and packaging requirements the framework handles underneath it.

## Move the failure into the build

The new backend, Vault API, and invitation flow expand how much application behavior can live in shared Java. This Apple work has the same practical purpose at the packaging boundary: remove another configuration trap each app would otherwise have to rediscover.

The builder can read the SDK's floor, reject removed hints, and inspect the final metadata before upload. Those checks make a failed configuration actionable while the developer still has the build in front of them. Together with the security APIs, they continue our work on defaults that developers can rely on across targets.

---

## Discussion

_Has your current submission hit a minimum-version or launch-metadata rejection? Include the exact message and relevant build hints so we can trace the configuration._

{{< giscus >}}
