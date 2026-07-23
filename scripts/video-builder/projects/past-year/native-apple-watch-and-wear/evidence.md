# Evidence map

Source: `docs/website/content/blog/native-apple-watch-and-wear.md`
Canonical: https://www.codenameone.com/blog/native-apple-watch-and-wear/

## Thesis

How one Java codebase reaches watchOS and Wear OS without pretending the platforms are identical

## Supported beats

- **Why a watch API at all:** Apple sees watch programming as a completely separate discipline from phone or desktop programming. It is a different API with a different logic, and that makes some sense given the device.
- **watchOS is not iOS:** The interesting engineering is on the Apple side. watchOS has no UIKit view hierarchy, no OpenGL ES and no Metal. None of the rendering paths the iOS port relies on exist there.
- **A separate watch root keeps the app small:** The watch has a far smaller memory and CPU budget than the phone, so the less code that reaches it, the better. That is what codename1.watchMain is for. Instead of reusing your phone's main class, you point the watch build at its own entry class.
- **Wear OS is just Android:** The Android side is much simpler, by design. A Wear OS app is an ordinary Android app that declares the watch hardware feature, so the existing Codename One Android port renders the watch UI through exactly the same pipeline it uses on phones and tablets.
- **What you write:** A few practical guidelines for the watch branch: prefer a single vertical column that scrolls on the Y axis (the Digital Crown and the Wear OS rotary input scroll the focused container), keep interactive targets large and few, and on round screens keep content away from the corners using the form's safe-area insets.
- **Enabling each build:** Both builds are additive: with the hints off, your phone builds are byte-for-byte unchanged. On Apple, one hint turns on the watch target, and the cloud build produces the watch slice as part of the regular iOS build.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues

## Independent problem evidence

- Apple watchOS Apps: https://developer.apple.com/documentation/watchos-apps — Apple documents watchOS applications, complications, and interaction patterns as watch-specific experiences rather than scaled phone screens.
- Package and Distribute Wear OS Apps: https://developer.android.com/training/wearables/apps/packaging — Android's Wear OS guidance describes watch hardware declarations, standalone behavior, and wearable-specific packaging.

## Product proof

- `docs/website/static/blog/native-apple-watch-and-wear/watch-bezel.png`
