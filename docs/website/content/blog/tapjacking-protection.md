---
title: "Tapjacking Protection: Rejecting Android Touches Behind an Overlay"
slug: tapjacking-protection
url: /blog/tapjacking-protection/
date: '2026-08-25'
author: Shai Almog
description: "Codename One can now report or block Android gestures that begin behind another application's overlay, and can ask Android 12 or newer to hide overlay windows on sensitive screens."
feed_html: '<img src="https://www.codenameone.com/blog/tapjacking-protection.jpg" alt="A protected Android confirmation screen rejecting a touch from behind a malicious overlay" /> Codename One can now report or block Android gestures that begin behind another application&#39;s overlay, and can ask Android 12 or newer to hide overlay windows on sensitive screens.'
series: ["release-2026-08-21"]
---

![A protected Android confirmation screen rejecting a touch from behind a malicious overlay](/blog/tapjacking-protection.jpg)

A confirmation screen can be correct and still receive a tap the user did not understand. On Android, another application can draw over the screen and make a transfer button look like part of a different interaction.

[PR #5553](https://github.com/codenameone/CodenameOne/pull/5553) adds tapjacking and screen-overlay protection to `DeviceIntegrity`. It can report the condition, drop the full gesture, and ask Android 12 or newer to prevent overlay windows on a sensitive screen.

This work extends the security thread in the [portable encrypted SQLite release overview](/blog/sqlite-portable-encrypted/). Database encryption protects stored bytes. Tapjacking protection controls input while the application is running.

## Android reports the overlay on the touch

Android marks a `MotionEvent` as fully obscured when another window covers the point that was touched. It marks the event as partially obscured when another window covers any part of the application window.

Those signals have different false-positive rates. A fully obscured touch is the tapjacking case. Partial obscuring can come from ordinary system UI.

{{< mermaid >}}
flowchart TD
    A[Android MotionEvent] --> B{Fully obscured?}
    B -->|yes| C{Policy}
    B -->|no| D{Partially obscured?}
    D -->|no| E[Deliver gesture]
    D -->|yes| F{STRICT?}
    F -->|yes| G[Drop through UP or CANCEL]
    F -->|no| E
    C -->|REPORT| H[Notify and deliver]
    C -->|BLOCK or STRICT| G
    C -->|OFF| E
{{< /mermaid >}}

Detection is therefore touch-driven. `isScreenObscured()` describes the latest observed touch. It is not a live query for every window on the device. An overlay that appears while nobody touches the application produces no event to inspect.

## Four policies make the cost explicit

`TapjackingPolicy` has four modes:

| Policy | Reports | Drops fully obscured gestures | Drops partially obscured gestures |
| --- | --- | --- | --- |
| `OFF` | No | No | No |
| `REPORT` | Yes | No | No |
| `BLOCK` | Yes | Yes | No |
| `STRICT` | Yes | Yes | Yes |

`BLOCK` is the normal setting for a sensitive application. `REPORT` is useful before enforcement when you need to measure what devices and installed tools produce. `STRICT` can discard intended taps caused by benign system UI, so it belongs only on flows where missing a tap is preferable to accepting a framed one.

The runtime setup can live in `init()`:

```java
DeviceIntegrity.setTapjackingProtection(TapjackingPolicy.BLOCK);

DeviceIntegrity.addTapjackingListener(event -> {
    if (Boolean.TRUE.equals(event.getSource())) {
        Dialog.show("Security warning",
                "Another app is drawing over this screen. "
                        + "Close it before continuing.",
                "OK", null);
    }
});
```

The listener receives state transitions rather than one callback per touch. Read the state from the event. The callback is delivered on the event dispatch thread, while a later clean touch can update the global state before a busy event thread handles the earlier warning.

## A blocked gesture is all or nothing

Dropping only the press would let the matching release reach a component that never received a press. The port latches the decision from `ACTION_DOWN` through `UP` or `CANCEL`. Application components receive none of the gesture.

The Android port performs the check explicitly before the event reaches Codename One. The normal Android `setFilterTouchesWhenObscured()` path is not enough because the Codename One view dispatches directly into its own touch handler. The standard filter remains enabled on fallback paths as a second layer.

This also matters for native peers. A `BrowserComponent` or native text field can have a platform-specific event path. Preventing the overlay window is stronger than filtering touches after they arrive.

## Android 12 can hide overlays

On a sensitive screen, combine the touch policy with overlay prevention and screen capture protection:

```java
if (DeviceIntegrity.isHideOverlayWindowsSupported()) {
    DeviceIntegrity.setHideOverlayWindows(true);
}
DeviceIntegrity.setSecureScreen(true);
```

`isHideOverlayWindowsSupported()` requires Android 12 or newer and the `android.permission.HIDE_OVERLAY_WINDOWS` manifest permission. The zero-code guard adds the permission and chooses the normal policy:

```properties
android.tapjackingGuard=true
android.tapjackingGuard.mode=block
android.tapjackingGuard.hideOverlays=true
```

Clear `setHideOverlayWindows(false)` and `setSecureScreen(false)` when the application leaves the sensitive flow if the rest of the product allows those features.

## The platform boundary is part of the API

iOS does not let one application draw a window over another, so the policy has no iOS work to do. Screen recording and mirroring are different threats and use `ios.disableScreenshots=true`.

The simulator exposes **Simulate > App Shield > Screen Overlay (Tapjacking)**. It drives the same listener and signal path as the Android port, which lets the warning, policy transitions, and analytics path run on the desktop. Real-device testing is still needed for partial-obscuring behavior because system UI differs by Android vendor.

Tapjacking is not reported as a standing device-compromise reason. An overlay is transient and can be benign. Folding it into `isDeviceCompromised()` would make a notification shade look like a rooted device. Instead it raises its own App Shield signal and remains available as a local input policy.

Secure-by-default does not mean enabling the strictest switch everywhere. It means the normal path blocks the high-confidence case, the API exposes the cost of stronger settings, and unsupported platforms do not claim a protection they cannot provide.

The {{< post-link path="/blog/camera-vision-scanners" text="next post restores a short path for common camera and vision cases" >}}.

---

## Discussion

_Would you begin with `REPORT` in production, or block fully obscured touches from the first release?_

{{< giscus >}}
