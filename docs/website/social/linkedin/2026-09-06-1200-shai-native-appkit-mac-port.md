---
title: "Why the Mac port moved from Catalyst to AppKit"
slug: 2026-09-06-1200-shai-native-appkit-mac-port
platform: linkedin
account: shai
source_slug: native-appkit-mac-port
publish_at: '2026-09-06T12:00:00'
timezone: Asia/Jerusalem
image: /blog/native-appkit-mac-port.jpg
---

Codename One's native Mac build is now an AppKit application, not an iOS application presented through Catalyst.

The port owns `NSApplication`, `NSWindow`, `NSMenu`, `NSTextInputClient`, `NSScreen`, and an independent Metal layer for every window.

Catalyst rendered a secondary window into a mutable image and copied it into a scene. One 4K BGRA buffer is roughly 33 MB before the next buffer or copy. AppKit paints each dirty region into that window's own drawable.

Always-on-top, utility windows, minimize, restore, maximize, modality, menus, monitor scale, and input methods are desktop operations again.

The existing Mac build targets keep their names. Applications that depend on Catalyst can set `codename1.arg.macNative.enabled=true` and build the ordinary `ios-source` or `ios-device` target; Catalyst has no target of its own.

The framework semantics tree is published as a per-window hierarchy of `NSAccessibilityElement` children, so VoiceOver receives roles, labels, values, states, actions, and live-region announcements through AppKit.

The migration keeps the existing Mac build targets while replacing the native foundation underneath them. Applications that need Catalyst can still opt into it; everyone else gets a desktop port built around desktop primitives.

{{canonical}}
