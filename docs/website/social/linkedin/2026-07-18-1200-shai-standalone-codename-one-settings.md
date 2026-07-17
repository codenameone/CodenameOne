---
title: "Shai: refreshing the tools around Codename One"
slug: 2026-07-18-1200-shai-standalone-codename-one-settings
platform: linkedin
account: shai
source_slug: standalone-codename-one-settings
publish_at: '2026-07-18T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-17'
status: approved
image: /blog/standalone-codename-one-settings.jpg
---

Old developer tools rarely become bad in one dramatic step. They accumulate jobs until nobody can explain where one tool ends and another begins.

Codename One Settings had reached that point. Project configuration lived beside account login, certificates, build monitoring, and extensions inside the old GUI Builder jar.

We rewrote it as a standalone tool, but the larger goal is to refresh the development environment around Codename One. Settings now owns project configuration. Signing, accounts, and cloud builds have their own homes.

This is less about a new command and more about removing old assumptions one tool at a time. Smaller tools with clear boundaries are easier to understand, replace, and improve.

Full write-up: https://www.codenameone.com/blog/standalone-codename-one-settings/
