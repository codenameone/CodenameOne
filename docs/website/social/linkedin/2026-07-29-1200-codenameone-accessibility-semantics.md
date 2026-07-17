---
title: "Codename One: portable accessibility semantics"
slug: 2026-07-29-1200-codenameone-accessibility-semantics
platform: linkedin
account: codenameone
source_slug: accessibility-semantics
publish_at: '2026-07-29T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/accessibility-semantics.jpg
---

Codename One lightweight components now expose a portable accessibility semantics tree.

Standard controls infer roles, states, values, ranges, and actions. Custom renderers can publish virtual children. The same immutable tree maps to VoiceOver, TalkBack, Windows UI Automation, Linux AT-SPI, Java accessibility, and an off-screen ARIA DOM on the web.

The simulator inspector audits unlabeled actions, invalid ranges, duplicate identifiers, and traversal cycles. Tests can snapshot the tree as JSON and assert the behavior users depend on.

Full write-up: {{canonical}}
