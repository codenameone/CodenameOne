---
title: "Codename One: support should be evidence"
slug: 2026-07-22-1200-codenameone-tested-port-support
platform: linkedin
account: codenameone
source_slug: tested-port-support
publish_at: '2026-07-22T12:00:00'
timezone: Asia/Jerusalem
image: /blog/tested-port-support.jpg
---

A support matrix should not be a collection of promises somebody remembers to update.

We wanted the Codename One matrix to be an output of the test system. Each result points back to a commit, environment, run date, and the tests behind it. A skipped test needs an explanation instead of silently turning into a green checkmark.

That also means publishing the gaps. A green cell says a specific test passed in a specific environment. It does not pretend we exercised every device or OS release. Binary-size and memory comparisons are missing because the current numbers would compare different things across platforms.

The page will change as CI changes. That is the point. Support claims should move with evidence, not with marketing copy.

Full write-up: https://www.codenameone.com/blog/tested-port-support/
