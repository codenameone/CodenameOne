---
title: "Shai: own your pixels"
slug: 2026-07-27-0300-shai-pixel-perfect-is-a-test
platform: linkedin
account: shai
source_slug: pixel-perfect-is-a-test
publish_at: '2026-07-27T03:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/pixel-perfect-is-a-test.jpg
---

An OS update can change the UIKit, SwiftUI, Compose, or Material widgets in an app you already shipped. The platform owns those widget implementations.

Codename One statically links its lightweight UI into the native app. The screen you test stays that way until you decide to change it.

The price is that we do not inherit a new iOS or Android look for free. This week we built native reference apps and made CI compare our themes against them. PR #5274 alone changed 1,147 files. It covers pixels, geometry, glass backgrounds, and fixed animation frames.

That is work we take on so you can work on your app instead of working for the Apple and Google design teams. You adopt their new look on your schedule, without giving them control of your shipped UI.

Full write-up: {{canonical}}
