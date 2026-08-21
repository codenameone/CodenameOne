---
title: "A correct confirmation screen can receive the wrong tap"
slug: 2026-08-25-1200-codenameone-tapjacking-protection
platform: linkedin
account: codenameone
source_slug: tapjacking-protection
publish_at: '2026-08-25T12:00:00'
timezone: Asia/Jerusalem
image: /blog/tapjacking-protection.jpg
---

Our security hardening work now covers another part of the path between the user and a sensitive operation. A malicious Android application can draw over a transfer screen and change what the user believes a tap will confirm.

Codename One now exposes tapjacking protection through `DeviceIntegrity`.

`REPORT` records an obscured gesture without changing input. `BLOCK` drops a gesture that begins fully obscured. `STRICT` also drops partially obscured gestures, including some benign system UI, so it carries a real usability cost.

The whole gesture is dropped through release or cancel. Delivering a release after swallowing the press would leave the UI with half an interaction.

Android 12 and newer can also hide overlay windows on a sensitive screen. That protects native peers such as browser and text components, where filtering only the Codename One event path is insufficient.

The feature is Android-specific because iOS does not allow one application to draw over another. The API says that instead of reporting a control that does nothing.

{{canonical}}
