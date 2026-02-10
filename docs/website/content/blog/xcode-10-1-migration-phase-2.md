---
title: Xcode 10.1 Migration Phase 2
slug: xcode-10-1-migration-phase-2
url: /blog/xcode-10-1-migration-phase-2/
original_url: https://www.codenameone.com/blog/xcode-10-1-migration-phase-2.html
aliases:
- /blog/xcode-10-1-migration-phase-2.html
date: '2019-01-20'
author: Shai Almog
---

![Header Image](/blog/xcode-10-1-migration-phase-2/xcode-migration.jpg)

As I [mentioned recently](/blog/xcode-10-1-migration.html), we’re migrating to xcode 10.1. This weekend we entered phase 2 of the migration which allows you to test your builds with xcode 10.1. To do this just set the build hint: `ios.xcode_version=10.1`.

If you run into issues with xcode 10.1 builds let us know ASAP. This is very urgent as phased 3 will kick in this coming Friday. In phase 3 we will flip 10.1 as the default which means that builds sent on Friday will implicitly target xcode 10.1.

If this produces an unforseen regression you can explicitly force xcode 9.2 for compatibility by using the build hint: `ios.xcode_version=9.2`.

This means you can use the capabilities of the latest xcode/iOS SDK within your native code starting with this update.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
