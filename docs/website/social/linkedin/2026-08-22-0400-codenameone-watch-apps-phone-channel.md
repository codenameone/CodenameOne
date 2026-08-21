---
title: "A watch app is a second application"
slug: 2026-08-22-0400-codenameone-watch-apps-phone-channel
platform: linkedin
account: codenameone
source_slug: watch-apps-phone-channel
publish_at: '2026-08-22T04:00:00'
timezone: Asia/Jerusalem
image: /blog/watch-apps-phone-channel.jpg
---

A watch app is not a small form running inside the phone process.

It has another executable, another sandbox, and another lifecycle. The phone may be asleep when the watch wakes. `Storage`, `Preferences`, and SQLite are local to each device.

Codename One now builds the watch application from one `codename1.watchMain` entry point on Apple Watch and Wear OS. The simulator launches the phone and watch as separate processes and connects them on the desktop.

`WearableConnection` makes the runtime boundary explicit:

• `sendMessage()` asks an awake peer for a reply.
• `putData()` replicates the latest value across sleep and relaunch.
• `transferFile()` moves a larger payload in the background.

The same API maps to `WCSession` and the Wearable Data Layer. Source, resources, CSS, and surface timelines remain shared. Lifecycle and failure stay real.

{{canonical}}
