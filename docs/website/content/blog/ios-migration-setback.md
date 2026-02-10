---
title: iOS Migration Setback
slug: ios-migration-setback
url: /blog/ios-migration-setback/
original_url: https://www.codenameone.com/blog/ios-migration-setback.html
aliases:
- /blog/ios-migration-setback.html
date: '2016-06-19'
author: Shai Almog
---

![Header Image](/blog/ios-migration-setback/xcode-migration.jpg)

A couple of weeks ago we [detailed a plan](/blog/ios-migration-continued.html) to migrate to the new xcode 7.x  
build servers. We tried this migration over the weekend and while for most developers this worked rather nicely  
for some there were issues that we can’t explain so we decided to revert the change and regroup.

We want this transition to be smoother than past transitions and since we aren’t currently working against a  
deadline we feel we have some time to refine this migration to a point where it will be seamless for all/most  
of our users.

We don’t have a set date to retry the migration but we are currently aiming for 2-3 weeks from now.

The main issue involves the rather elaborate build script we have in place since the days of xcode 4.x. It seems  
that for some applications or provisioning profiles xcode just crashes…​

The solution is probably to build the application in a rather different way than we current do which might not crash  
xcode but unfortunately this will make this change & migration more complicated than we initially hoped so the  
sensible solution seems to be to move back to the `iphone_new` mode and make builds go to the old servers  
while we experiment.

We will announce details of the second attempt at migration in this blog…​

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
