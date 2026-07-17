---
title: "Shai: why Settings got smaller"
slug: 2026-07-28-1200-shai-standalone-codename-one-settings
platform: linkedin
account: shai
source_slug: standalone-codename-one-settings
publish_at: '2026-07-28T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/standalone-codename-one-settings.jpg
---

Codename One Settings had become the place where unrelated jobs accumulated.

Project properties, account login, certificates, build monitoring, and extensions all lived in one old GUI Builder jar. The rewrite is smaller on purpose.

The standalone tool owns project configuration. The Certificate Wizard owns signing. The website owns accounts and cloud builds. That costs us another Maven artifact and another release step, but each tool now has a boundary we can explain in one sentence.

Full write-up: {{canonical}}
