---
title: XCode Migration Take 2
slug: xcode-migration-take-2
url: /blog/xcode-migration-take-2/
original_url: https://www.codenameone.com/blog/xcode-migration-take-2.html
aliases:
- /blog/xcode-migration-take-2.html
date: '2016-08-10'
author: Shai Almog
---

![Header Image](/blog/xcode-migration-take-2/xcode-migration.jpg)

We hoped to get the [xcode 7 migration](/blog/ios-server-migration-plan.html) on the build servers [out before version 3.5](/blog/ios-migration-continued.html) but the change had problems and we [chose to revert](/blog/ios-migration-setback.html). We postponed the change so we can get 3.5 out of the door…​  
This Sunday we’ll deploy a server update that should be the first step in the migration.

Because of this deployment we will skip the Friday release and shift it to Sunday which has lower traffic/builds.

### Upcoming Release

This Sunday we’ll release an update that could break the build for existing projects. It’s a major rewrite of our ios build code to use newer API’s when available. During this release our old servers might break which would force us to revert these changes and redeploy at a later date.

Assuming all goes well we will proceed by flipping the ios build servers in two weeks time (again on a Sunday). We’ll announce that separately based on the results of this deployment.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
