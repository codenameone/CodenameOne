---
title: GET REPEATABLE BUILDS? BUILD AGAINST A CONSISTENT VERSION OF CODENAME ONE?
  USE THE VERSIONING FEATURE?
slug: how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature
url: /how-do-i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-get-repeatable-builds-build-against-a-consistent-version-of-codename-one-use-the-versioning-feature.html
tags:
- pro
description: Versioning allows us to build against a point release and get stability/consistency
  over time
youtube_id: w7xvlw3rI6Y
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-4-1.jpg
---
{{< youtube "w7xvlw3rI6Y" >}}
Repeatable builds matter when you are trying to stabilize a release, investigate a regression, or keep a production app on a known-good Codename One version instead of automatically moving with the latest server-side changes. Versioned builds are the feature designed for that job.

The core idea is simple. Instead of always building against the current Codename One server release, you tell the build system to target a specific Codename One version. That means the native build is performed against the exact server logic and framework state associated with that release. If your application built and ran correctly against that version before, you now have a way to stay there while you validate later updates on your own schedule.

This is especially useful when a build suddenly starts failing or when behavior changes unexpectedly after a platform update. If you can rebuild against an older known-good Codename One version and the problem disappears, you have learned something important: the regression is probably tied to the framework or build-server changes rather than to a recent change in your own app. That turns versioned builds into both a stability feature and a debugging tool.

The original video frames this as a Pro feature with longer retention in the Enterprise tier, and that is still the right way to think about availability. The exact retention window depends on the subscription tier, but the practical lesson is the same: if stable historical build targets matter to your team, versioned builds should be part of your release strategy.

When you use a versioned build, it is usually a good idea to keep your local simulator environment aligned with that same framework version as well. Otherwise you can end up testing one version locally and shipping another remotely. The older settings UI exposed this through the version selection and client library update flow. In current Maven-based projects, the broader goal is still consistency: keep the project, simulator, and native build path aligned so that local testing reflects what the build server is actually producing.

This is not something you need for every build during active day-to-day development. It becomes valuable when predictability matters more than immediately consuming the latest changes. Teams that ship production apps, support older releases, or operate in tightly controlled release windows tend to benefit the most from this feature.

In practice, the healthiest workflow is to use the current Codename One version during normal development, then pin to a specific version when you need a reproducible release train or when you are isolating a regression. Once the newer version is validated, you can move forward deliberately instead of being surprised by it in the middle of a release.

## Further Reading

- [Build Server](/build-server/)
- [Moving To Maven](/blog/moving-to-maven/)
- [Developer Guide](/developer-guide/)
- [How Do I Use Offline Build](/how-do-i/how-do-i-use-offline-build/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
