---
title: "Codename One: standalone Settings tool"
slug: 2026-07-27-1200-codenameone-standalone-codename-one-settings
platform: linkedin
account: codenameone
source_slug: standalone-codename-one-settings
publish_at: '2026-07-27T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/standalone-codename-one-settings.jpg
---

`mvn cn1:settings` now launches a standalone Codename One desktop tool.

It edits the current project's identity, build hints, themes, and extensions. Account security moved to the website. Signing moved to the Certificate Wizard. Build monitoring stays with the cloud build console.

The property files remain the source of truth. The application is a focused editor for them, distributed as its own Maven artifact instead of a screen embedded in the old GUI Builder jar.

Full write-up: {{canonical}}
