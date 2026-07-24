---
title: "Bluetooth support across every Codename One target"
slug: 2026-07-26-1200-codenameone-bluetooth-beyond-ble
platform: linkedin
account: codenameone
source_slug: bluetooth-beyond-ble
publish_at: '2026-07-26T12:00:00'
timezone: Asia/Jerusalem
image: /blog/bluetooth-beyond-ble.jpg
---

Bluetooth is now part of the Codename One core API and works across every target, including JavaScript. The API covers:

• BLE peripheral mode and local GATT servers
• notifications and queued GATT operations
• L2CAP byte streams
• classic RFCOMM for scanners and printers
• Web Bluetooth for browser builds
• a simulator that can delay or fail the next radio operation
• a native desktop backend for testing against real devices

Not every role exists on every platform. Web Bluetooth uses the browser chooser and central GATT only. iOS does not expose arbitrary RFCOMM. Capability queries make those limits part of the API.

The builders inspect the roles an app references and add only the matching permissions and native pieces.

{{canonical}}
