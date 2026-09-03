---
title: "The API was the easy part of VoIP and VPN"
slug: 2026-09-04-1200-shai-voip-vpn-builders
platform: linkedin
account: shai
source_slug: voip-vpn-builders
publish_at: '2026-09-04T12:00:00'
timezone: Asia/Jerusalem
image: /blog/voip-vpn-builders.jpg
---

Adding a Java method named `reportIncoming()` is easy.

Making it report a VoIP call to CallKit before iOS terminates the process is the feature.

This week Codename One added native call management and VPN APIs. The interesting work happens after Maven sees the packages an application uses.

The builders add the exact native product pieces: CallKit and PushKit delegates, Android connection services, VPN frameworks, permissions, background modes, and signing checks. If the application does not reference a feature package, it does not inherit that package's native weight or permissions.

The boundaries stay visible. Codename One does not supply a codec, signaling server, or VPN protocol. Managed IKEv2 works on iOS and Android. Raw packet tunneling is Android-only in this release; the iOS build fails instead of shipping a tunnel extension that cannot run.

Browsers cannot reach these system services. Other cross-platform stacks can through native plugins, but the extension targets and native-project wiring remain part of the application team's work.

Codename One's secret weapon is that the builder already owns the native product graph. The application keeps one Java codebase and its own UI, media, protocol, and business logic. The builder generates the platform shell required to ship it.

{{canonical}}
