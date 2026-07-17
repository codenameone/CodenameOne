---
title: "Shai: why the support table has missing metrics"
slug: 2026-08-01-1200-shai-tested-port-support
platform: linkedin
account: shai
source_slug: tested-port-support
publish_at: '2026-08-01T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/tested-port-support.jpg
---

Our new port status page deliberately omits binary size and memory.

Android and web report compressed packages. Apple reports an unpacked app bundle. Linux and Windows report executables. The ports also expose different memory concepts. Putting those values in one table would look scientific while comparing different things.

So the first version publishes what CI can prove honestly: 49 feature groups, 10 targets, exact test results, run environments, dates, and skip reasons. The missing metrics will return after we build a dedicated release fixture that packages and samples every target the same way.

Full write-up: {{canonical}}
