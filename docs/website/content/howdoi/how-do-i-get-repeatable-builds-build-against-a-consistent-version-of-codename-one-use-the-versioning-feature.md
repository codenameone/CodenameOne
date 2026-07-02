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

The core idea is simple. Instead of always building against the current Codename One release, you tell the build server to target a specific published Codename One version. The native build is then performed using the framework artifacts for that exact release, which the build server fetches from Maven Central. If your application built and ran correctly against that version before, you have a way to stay there while you validate later updates on your own schedule.

## How to enable a versioned build

Set the `build.cn1Version` build hint to the Maven release version you want to target. Versions follow the standard Maven scheme published to Maven Central, for example `7.0.182`.

In a Maven project, add the hint to `codenameone_settings.properties` using the `codename1.arg.` prefix:

```
codename1.arg.build.cn1Version=7.0.182
```

You can also set it from the IDE through the Codename One Settings dialog under Build Hints, using `build.cn1Version` as the key.

To keep your local simulator and compile classpath aligned with the same release, set the matching version in your project `pom.xml`:

```
<cn1.version>7.0.182</cn1.version>
```

Otherwise you can end up testing one version locally and shipping another remotely.

## Availability and the version window

Versioned builds are a Pro and Enterprise feature, and how far back you can target depends on your subscription tier:

- Pro can target any version released within the last two months.
- Enterprise can target any version released within the last six months.

If you request a version that is older than your tier's window, or you are not on a Pro or Enterprise plan, the build fails with a clear message instead of silently falling back to the current release. Requesting a version that was never published also returns an explicit error.

## Building against master

If you want to test an unreleased fix or feature, set the hint to `master` instead of a version number:

```
codename1.arg.build.cn1Version=master
```

Every push to the Codename One `master` branch publishes a fresh set of framework artifacts, and the build server always grabs the latest. This is the quickest way to verify an unreleased change end to end on a device. Because `master` tracks active development it can be less stable than a release, so use it for verification rather than for shipping production builds. Building against `master` is available to Pro and Enterprise subscribers.

## When to use it

This is especially useful when a build suddenly starts failing or when behavior changes after a platform update. If you can rebuild against an older known-good version and the problem disappears, the regression is probably tied to the framework or build server rather than to a recent change in your own app. That makes versioned builds both a stability feature and a debugging tool.

You do not need this for every build during day-to-day development. It becomes valuable when predictability matters more than immediately consuming the latest changes: teams that ship production apps, support older releases, or operate in tightly controlled release windows benefit the most. The healthiest workflow is to develop against the current version, pin to a specific version when you need a reproducible release train or are isolating a regression, then move forward deliberately once the newer version is validated.

## Further Reading

- [Build Server](/build-server/)
- [Moving To Maven](/blog/moving-to-maven/)
- [Developer Guide](/developer-guide/)
- [How Do I Use Offline Build](/how-do-i/how-do-i-use-offline-build/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
