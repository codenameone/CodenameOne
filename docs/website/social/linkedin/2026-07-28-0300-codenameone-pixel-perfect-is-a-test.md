---
title: "Codename One: native fidelity on your schedule"
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

Codename One apps are native, but the lightweight UI is statically linked into the app instead of being owned by the operating system's widget toolkit. An OS update cannot silently restyle that UI.

That stability means a new platform design does not arrive for free. The new fidelity suite compares iOS 26 Liquid Glass and Android Material 3 themes against native reference applications. The current baselines contain 68 iOS pairs and 54 Android pairs across light, dark, normal, pressed, selected, and disabled states.

CI reports visual similarity and geometry separately. Tab and switch animations are frozen at fixed progress points. The gate only moves one way, so a theme change cannot silently lower the recorded baseline.

Full write-up: {{canonical}}
