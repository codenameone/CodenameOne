---
title: "Watch Follow-Through: Complications, Tiles, and a Companion Wear APK"
slug: watch-complications-wear-companion
url: /blog/watch-complications-wear-companion/
date: '2026-08-31'
author: Shai Almog
description: "Codename One now generates watchOS WidgetKit complications, Wear OS complication data sources and Tiles, and a companion Wear APK beside the phone artifact."
feed_html: '<img src="https://www.codenameone.com/blog/watch-complications-wear-companion.jpg" alt="Watch complications and a companion Wear application generated from a Codename One project" /> Codename One now generates watchOS WidgetKit complications, Wear OS complication data sources and Tiles, and a companion Wear APK beside the phone artifact.'
series: ["release-2026-08-28"]
---

![Watch complications and a companion Wear application generated from a Codename One project](/blog/watch-complications-wear-companion.jpg)

Last week's wearable post ended with two conspicuous limits. Declaring a `WATCH_*` surface did not generate the native extension or service that puts it on a watch face. An Android companion build did not return a Wear APK beside the phone APK.

[PR #5583](https://github.com/codenameone/CodenameOne/pull/5583) closes both gaps. [PR #5594](https://github.com/codenameone/CodenameOne/pull/5594) fixes two watchOS failures that appeared once the complete watch path ran. This is follow-through on the model we shipped last week, not a second wearable announcement.

For native desktop windows and the rest of this release, see the [weekly release overview](/blog/native-desktop-windows/).

## One surface declaration now reaches the watch face

A complication remains a watch family in the existing surfaces API:

```java
WidgetKind steps = new WidgetKind("steps")
        .setDisplayName("Steps")
        .addSupportedSize(WidgetSize.WATCH_CIRCULAR)
        .addSupportedSize(WidgetSize.WATCH_RECTANGULAR);

Surfaces.registerWidgetKind(steps);
Surfaces.publish("steps", timeline);
```

The four portable families lower to the platform surfaces below:

| Codename One family | Apple Watch | Wear OS |
| --- | --- | --- |
| `WATCH_CIRCULAR` | `accessoryCircular` | Ranged value or monochrome image complication |
| `WATCH_RECTANGULAR` | `accessoryRectangular` | Long-text complication and a Tile |
| `WATCH_INLINE` | `accessoryInline` | Short-text complication |
| `WATCH_CORNER` | `accessoryCorner` | Circular complication because Wear has no corner slot |

The build now produces the missing platform pieces. Apple Watch receives a `CN1WatchWidgets` WidgetKit extension embedded inside the watch application. Wear OS receives one complication data source per kind and a ProtoLayout Tile for the rectangular family.

{{< mermaid >}}
flowchart TD
    A[WidgetKind with WATCH families] --> B[Shared surface descriptor and timeline]
    B --> I[watchOS build]
    B --> W[Wear OS build]
    I --> IE[CN1WatchWidgets<br/>WidgetKit extension]
    W --> WC[Complication data source<br/>per kind]
    W --> WT[Tile service<br/>rectangular family]
    IE --> F1[Apple Watch face]
    WC --> F2[Wear OS watch face]
    WT --> T[Wear OS Tile carousel]
{{< /mermaid >}}

No watch family means no watch extension, complication service, Tile service, or related dependency. A kind that declares only watch families also stops producing an accidental Android home-screen widget. Add a phone family when the product needs both.

## A watch face does not render a small widget

Wear OS asks a complication data source for a typed value. It does not accept a component hierarchy. The generated adapter mines the surface tree for at most two text nodes and one image, then maps them to short text, long text, ranged value, or monochrome image data.

Containers, padding, backgrounds, corner radii, alignment, weight, per-node color, and child actions do not survive that lowering. The renderer logs dropped details once through `CN1Surfaces`, so `adb logcat -s CN1Surfaces` shows what the watch face received.

Apple's WidgetKit accessory families render more of the SwiftUI tree, but the slots remain small and often monochrome. A single number or gauge is safer than a layout that depends on several labels.

A Wear Tile is different. It renders the node tree and supports per-node actions. Circular progress can remain circular there. A `SurfaceDynamicText` countdown does not tick continuously on the Tile; it advances when the published timeline refreshes.

The simulator previews all four watch families at their real point sizes and clips circular shapes. It cannot show the lossy Wear OS conversion. Check a final complication on the device and watch face you plan to support.

## Phone-published data has to cross to the watch

The phone and watch use separate storage. Even Apple's App Group identifier resolves to a watch-local container on the watch. Publishing a timeline on the phone cannot change a file in that container.

When a kind contains a watch family, a successful phone-side `Surfaces.publish()` now mirrors the descriptor after the local publish completes:

{{< mermaid >}}
sequenceDiagram
    participant App as Phone app
    participant Local as Phone surface store
    participant Bridge as WatchConnectivity or Data Layer
    participant Watch as Watch surface store
    participant Face as Watch face
    App->>Local: Publish descriptor and timeline
    Local-->>App: Local publish succeeds
    App->>Bridge: Mirror watch-bearing kind
    Bridge->>Watch: Store descriptor and imagery
    Watch->>Face: Request timeline reload
{{< /mermaid >}}

Apple uses `transferCurrentComplicationUserInfo` when the watch reports that a complication is active and the daily transfer budget has room. Otherwise it queues ordinary user info for the next watch wake. The payload is capped at 48 KB. Imagery is dropped before the whole update so the number can still refresh.

Wear OS sends the descriptor through the Data Layer and moves imagery as a file. The watch process can apply the framework-reserved update without launching the Codename One UI.

This mirror is best effort. A product that requires a guaranteed current value should let the watch fetch from the network or publish from its own application lifecycle.

## Android companion builds now return two artifacts

`codename1.watchMain` still names the watch application entry point on both platforms:

```properties
codename1.arg.codename1.watchMain=com.example.MyWatchApp
```

Without `codename1.watchStandalone`, Android now returns:

```text
myapp.apk
myapp-wear.apk
```

The Wear artifact declares the watch hardware feature and receives a higher version code. Google Play uses supported-device filters and version codes to select the phone or watch product from one listing. `android.watchVersionCodeOffset` changes the default gap, and `android.watchVersionCode` sets the watch code directly.

Setting `codename1.watchStandalone=true` still makes the single Android artifact the standalone watch application. Set `android.watchModule=false` when the application wants the phone-to-watch Data Layer but does not ship its own watch UI.

On Apple, a companion remains embedded inside the phone application. A standalone watch target is generated, but the returned archive still targets the phone scheme. App Store submission of that standalone product needs a manual archive of the watch scheme in Xcode.

## Running the complete path found two watchOS failures

The new complication work exercised a watch application from build through launch. That exposed two issues fixed by [PR #5594](https://github.com/codenameone/CodenameOne/pull/5594).

Six native functions in `IOSNative.m` call translated `IOSWearableCallbacks` entry points. Their generated header was never included. The watch compiler rejected the undeclared calls. A phone compiler that accepted the implicit declarations could pass the Java objects through the wrong native calling convention.

After that compile failure was fixed, the watch app crashed at launch. Objective-C called `[session activate]`, which is the Swift spelling of the API. The Objective-C selector is `activateSession`. iOS happened to answer the wrong selector in the existing path; watchOS did not.

The generated callback header is now included only when WatchConnectivity is translated, and both activation call sites use `activateSession`. The watch screenshot run reaches the full suite instead of stopping before the first capture.

## Closing the gap we wrote down

The useful part of publishing explicit limits is that they become work items instead of folklore. Last week's post said complications were a portable model without generated platform adapters and that Android companion packaging was missing. This release replaces both sentences with working build outputs, tests, and device-facing documentation.

The rest of this week's work applies the same discipline. Desktop windows isolate paint and input state per native surface. Document Provider publishes a narrow file tree to another process. Nearby Devices separates ranging, association, and transport so an application takes only the platform access it uses. Rootless jailbreak detection updates the signal set without promoting a local heuristic into a server trust decision.

Codename One's security lead depends on these boundaries remaining visible. A watch update can be delayed. A face may discard most of a layout. A companion APK has its own lifecycle and storage. Code that handles those states explicitly is safer than code built on a cross-platform promise that the operating systems do not keep.

The {{< post-link path="/blog/rootless-jailbreak-detection" text="next post upgrades iOS jailbreak detection for current rootless layouts" >}}.

---

## Discussion

_What would you put in a complication if the watch face gave you one number, one image, and one tap action?_

{{< giscus >}}
