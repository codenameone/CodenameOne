---
title: "Smart-home portability needs more than common names"
slug: 2026-08-24-0400-codenameone-smart-home-homekit-matter
platform: linkedin
account: codenameone
source_slug: smart-home-homekit-matter
publish_at: '2026-08-24T04:00:00'
timezone: Asia/Jerusalem
image: /blog/smart-home-homekit-matter.jpg
---

HomeKit and Matter can both describe brightness. They do not use the same identifier, scale, or surrounding device model.

`com.codename1.home` now gives a Codename One application canonical traits for lights, locks, thermostats, scenes, change subscriptions, and Matter commissioning.

The useful part is where the API refuses to flatten a difference.

Android can commission a Matter device into Google Home with little setup, but reading the accessory graph needs a developer registration and signing-key identity. That state is `COMMISSIONING_ONLY`, not a misleading `AVAILABLE`.

Batch reads and writes preserve partial results. A reading can have a value, an error, or no current measurement. Capability queries expose background-delivery and commissioning limits.

The simulator includes an unreachable socket, a constrained dimmer, a bridged light, and a thermostat with no single setpoint in automatic mode. Portable code should meet the awkward cases before it meets hardware.

{{canonical}}
