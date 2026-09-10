---
title: "API 37 Readiness Starts Before the Target SDK Switch"
slug: android-37-readiness-location-button
url: /blog/android-37-readiness-location-button/
date: '2026-09-17'
author: Shai Almog
description: "Codename One checks Android API 37 compilation, fixes minor-version SDK handling, and adds the system location button with older-platform fallback. Compile readiness and runtime migration remain distinct."
feed_html: '<img src="https://www.codenameone.com/blog/android-37-readiness-location-button.jpg" alt="Ready Before The Deadline" /> Codename One checks Android API 37 compilation, fixes minor-version SDK handling, and adds the system location button with older-platform fallback. Compile readiness and runtime migration remain distinct.'
series: ["release-2026-09-11"]
---

![Ready Before The Deadline](/blog/android-37-readiness-location-button.jpg)

An Android port can compile successfully against an old `android.jar` while referencing a class the next platform has removed. Until this week, our regular port compilation could give exactly that false reassurance.

[PR #5731](https://github.com/codenameone/CodenameOne/pull/5731) adds checks against API 37 and fixes builder assumptions about minor-versioned platforms. [PR #5738](https://github.com/codenameone/CodenameOne/pull/5738) adds the Android 17 location button. Both let us prepare the migration before changing every customer's target SDK.

## Three different meanings of ready

| Check | What it establishes |
| --- | --- |
| Compile port sources against the new platform | Detect removed platform symbols |
| Assemble the generated application against the new SDK | Exercise resources, manifest merging, dependency metadata, and packaging |
| Run and interact with the app on the new OS | Exercise changed runtime behavior and permissions |

A successful compile does not answer the third question. The API 37 PR deliberately leaves the normal SDK floor at 36 rather than silently changing all generated applications' target SDK and required downloads.

The source check compiles identical sources against two platform jars and compares errors. Optional packages with unresolved dependencies can fail identically in both runs; the useful signal is an error introduced only by the newer platform.

A second `android.jar` on the classpath would defeat that check by supplying a removed symbol from the old stub. The script removes those duplicate platform jars. A fault-injection probe using removed fingerprint APIs proved that the comparison detects the regression it was designed to catch.

## The platform name is no longer an integer

The installed API 37 packages use names such as `android-37.0`, `android-37.1`, and `android-37.2`. Treating the whole suffix as an integer failed outright on one builder path. Removing punctuation before parsing was worse: `37.2` became `372`, which silently passed every ordinary minimum-version check.

The builder now extracts the major API level correctly, updates build-tools handling, and uses the SDK manager associated with the SDK root actually being built. The generated-project check then assembles against the new platform, covering failures a source-only check cannot see.

This continues the [platform-watch work from last week](/blog/platform-deprecation-watch/). The goal is to turn a future platform requirement into a failing check while there is still time to fix the producer.

## One-time location belongs behind a deliberate action

Android 17 introduces a system-rendered location button for transactional precise-location access. Google's policy page, checked September 10, lists January 27, 2027, for compliance and describes the Android 17+ enforcement timing as subject to further updates. That is a location-policy timeline, not a blanket API 37 target-SDK deadline. [Google Play location policy](https://support.google.com/googleplay/android-developer/answer/17033915?hl=en).

The Codename One component presents the platform control where supported and an ordinary button elsewhere:

```java
import com.codename1.location.LocationButton;

LocationButton location = new LocationButton(
        LocationButton.TEXT_USE_PRECISE_LOCATION);
location.addLocationSharedListener(fix -> {
    if (fix != null) {
        searchNearby(fix.getLatitude(), fix.getLongitude());
    }
});
form.add(location);
```

`searchNearby` is application code. A `null` result is a normal outcome when permission is declined or no location is delivered. The component exposes `isSystemRendered()` so the application can inspect which path is active.

Google's design ties the request to a visible user action and a session-scoped precise-location grant. The application should still choose coarse location if that is enough for the task. [Android's location-button introduction](https://developer.android.com/blog/posts/redefining-location-privacy-new-tools-and-improvements-for-android-17?hl=en).

## Use the platform protocol without forcing an AndroidX upgrade

The earlier implementation depended on an AndroidX library whose metadata required compile SDK 37 and Android Gradle Plugin 9.1.0. That would have made a new button force a toolchain migration for every generated application using it.

The platform already exposes the session API beneath that wrapper. The merged implementation reaches it through reflection and implements the callback interface through a Java proxy. The system draws its control into a surface hosted inside the Codename One component.

{{< mermaid >}}
flowchart TD
    B[LocationButton component] --> S{Platform session available?}
    S -->|Yes| P[Host system-rendered surface]
    P --> T[User taps system control]
    T --> G[Platform consent and location result]
    S -->|No or session fails| F[Ordinary Codename One button]
    F --> O[Existing location permission flow]
    G --> L[Shared listener receives Location or null]
    O --> L
{{< /mermaid >}}

This is a specific native control integration. It does not change Codename One's general custom-rendered UI model.

## Permission injection and fallback still need attention

The generated manifest must include `USE_LOCATION_BUTTON`. The repository builder adds it when the application references `LocationButton`. The PR explicitly says the separate BuildDaemon mirror was still pending at that point; this article does not certify deployment to the cloud builder serving a particular account.

The implementation also avoids automatically restricting all precise-location permission to button-only access. An application may have a legitimate continuous-location feature as well. That decision requires reviewing the application's actual location use and current platform guidance, rather than inferring policy from the presence of one class.

If a platform session fails, the component falls back to the ordinary button. That preserves a usable control, but an app targeting the new transactional-location requirements should test that the system path really remains active. A visible button alone is insufficient evidence of the new permission flow.

## What was exercised

The location-button PR reports a real Android 17 emulator image running a generated app at compile SDK 37.2 and target SDK 37. The system button rendered, a human tap opened the platform consent sheet, and approval produced a location with the session-scoped permission flags. The same APK on API 36 used the ordinary button and permission flow.

The regular Android screenshot CI leg remains at API 36 in this work. Broader API 37 runtime behavior and policy compliance are not established by the compile gate or this single location flow. We will keep those checks separate so a passing build cannot conceal an untested migration assumption.

That closes the {{< post-link path="/blog/performance-work-between-benchmarks" text="week's release series" >}} with the same principle that guided the runtime changes: fix the shared implementation at the layer that owns the behavior. Builders handle platform packaging, the OS presents sensitive consent, and application code expresses the task. Early preparation should reduce the work customers face when migration is required, while preserving their control over when to make the switch.

---

## Discussion

_Which of your precise-location requests could become a one-time user action?_

{{< giscus >}}
