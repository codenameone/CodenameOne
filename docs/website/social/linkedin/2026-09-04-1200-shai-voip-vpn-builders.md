---
title: "The builder made cross-platform VoIP and VPN possible"
slug: 2026-09-04-1200-shai-voip-vpn-builders
platform: linkedin
account: shai
source_slug: voip-vpn-builders
publish_at: '2026-09-04T12:00:00'
timezone: Asia/Jerusalem
image: /blog/voip-vpn-builders.jpg
---

VoIP and VPN looked like features a Java cross-platform framework had no business promising.

They sit below the application UI, depend on radically different operating-system services, and often run outside the application process. On iOS, a call must reach CallKit while the phone is locked. A packet tunnel lives in a separate Network Extension executable with its own lifecycle, App ID, provisioning profile, and signing rules.

This week Codename One added native call management, managed VPN, and low-level packet tunneling on Android and iOS.

The builders are what made it possible. After Maven sees the packages an application uses, they add the exact native product pieces: CallKit and PushKit delegates, Android connection services, VPN frameworks, permissions, background modes, extension targets, and signing checks. An application that does not use the feature inherits none of that native weight or permission surface.

The boundaries remain visible. Codename One does not supply a codec, signaling server, or VPN protocol. The iOS packet tunnel can inspect, rewrite, drop, and forward packets on the device, but its deliberately small extension does not contain the Codename One networking stack. Remote relay needs a platform-specific transport. Android can relay from the application process.

Browsers cannot reach these system services. Other cross-platform stacks can through native plugins, but the extension targets and native-project wiring remain part of the application team's work.

The application keeps one Java codebase and its own UI, media, protocol, and business logic. The builder generates the native product required to ship it.

{{canonical}}
