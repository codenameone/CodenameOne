---
title: "Codename One: native fidelity ratchet"
slug: 2026-07-28-0300-codenameone-pixel-perfect-is-a-test
platform: linkedin
account: codenameone
source_slug: pixel-perfect-is-a-test
publish_at: '2026-07-28T03:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/pixel-perfect-is-a-test.jpg
---

Codename One now tests its iOS 26 Liquid Glass and Android Material 3 themes against native reference applications.

The current baselines contain 68 iOS pairs and 54 Android pairs across light, dark, normal, pressed, selected, and disabled states. CI reports visual similarity and geometry separately. Tab and switch animations are frozen at fixed progress points so motion regressions cannot hide between the first and last frame.

The gate only moves one way. A theme change can improve a recorded result or fail the build. It cannot silently lower the baseline.

Full write-up: {{canonical}}
