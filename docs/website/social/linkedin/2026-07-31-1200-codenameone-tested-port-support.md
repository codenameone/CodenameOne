---
title: "Codename One: port status from CI evidence"
slug: 2026-07-31-1200-codenameone-tested-port-support
platform: linkedin
account: codenameone
source_slug: tested-port-support
publish_at: '2026-07-31T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/tested-port-support.jpg
---

The new Codename One Port Status page maps 49 feature groups across 10 targets to current CI reports.

Each result carries a commit, environment, run date, mapped tests, and skip explanations. Architecture and renderer variants remain separate, including iOS Metal versus OpenGL and Windows or Linux x64 versus ARM64.

A green cell means the named tests passed in the named environment. It does not claim every OS version and device was exercised. The page keeps that boundary visible.

Full write-up: {{canonical}}
