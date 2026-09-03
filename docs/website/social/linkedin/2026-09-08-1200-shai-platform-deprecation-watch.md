---
title: "A failed user build should not be our platform alarm"
slug: 2026-09-08-1200-shai-platform-deprecation-watch
platform: linkedin
account: shai
source_slug: platform-deprecation-watch
publish_at: '2026-09-08T12:00:00'
timezone: Asia/Jerusalem
image: /blog/platform-deprecation-watch.jpg
---

For years, our most reliable warning for an Apple or Google change was a developer whose build failed first.

This week we started a daily scheduled Codex task that reads official notices and checks them against Codename One source, builders, generated projects, and packaged artifacts.

The task needs three things before it creates work: a primary source, an applicability test, and a concrete Codename One producer. It searches for existing fixes first. A policy headline is not a bug report, and a changed Gradle setting does not prove the native binary complies.

The first useful results include Android 16 Back handling and a permission-free Contact Picker. Both moved from a platform policy or runtime contract into a specific implementation and test path.

Automation will miss notices and misread scope. Its output is a source trail, not a verdict. We can inspect the requirement, the generated product, the failure, and the fix.

That moves platform security work into the normal release cycle, before a deadline forces the broadest available patch.

{{canonical}}
