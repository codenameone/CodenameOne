---
title: "Versioned Builds Are Back, With Master Builds For Fast Verification"
slug: versioned-builds-master
url: /blog/versioned-builds-master/
date: '2026-07-07'
author: Shai Almog
description: "Versioned builds return with a Maven-era model: pin a cloud build to a released Codename One version, or build against master to verify the current development head without waiting for a nightly package."
feed_html: '<img src="https://www.codenameone.com/blog/versioned-builds-master.jpg" alt="Versioned Builds Are Back" /> Versioned builds return with a Maven-era model: pin a cloud build to a release, or build against master to verify the current development head.'
series: ["release-2026-07-03"]
---

![Versioned Builds Are Back, With Master Builds For Fast Verification](/blog/versioned-builds-master.jpg)

Versioned builds are back.

The old versioned-build story came from the Ant era. Codename One had point releases every few months, and versioning meant "build against that point release." It made sense at the time, but the platform and the build infrastructure moved on. Maven Central, faster releases, more build targets, and a much larger code surface made the old model hard to maintain.

The new model keeps the part developers actually need: a way to stabilize and diagnose builds without being forced onto the current server-side framework every time.

## Pin A Released Version

By default, a cloud build uses the current Codename One release. With a versioned build, you can pin to a specific published version:

```properties
build.cn1Version=7.0.182
```

The build server fetches that version's framework artifacts and builds against them. If you also want the simulator and local compile classpath to match, set the matching `cn1.version` in your Maven project.

This is useful in three situations:

- You are stabilizing a production release and want fewer moving parts.
- A build started failing and you need to know whether the regression is in your app or in the framework/build server.
- You are supporting an older customer app and need to reproduce the environment it last built with.

## Build Against Master

The more interesting part is `master`:

```properties
build.cn1Version=master
```

This builds against the current development head of Codename One. Community developers recently asked for nightly or daily builds, but pushing nightly artifacts through Maven Central and support channels is messier than it sounds. A fixed nightly is also stale the moment the next fix lands.

Building against `master` is a faster developer loop for most verification cases. If a fix landed this morning and you need to verify it on a device, you do not wait for tomorrow's nightly. You ask the cloud build to use the current development head.

{{< mermaid >}}
flowchart TD
    A["A fix lands on master"] --> B["Framework artifacts published"]
    B --> C["Your build sets build.cn1Version=master"]
    C --> D["Cloud build uses current development head"]
    D --> E["Verify on real target"]
    E --> F["Switch back to a release for production"]
{{< /mermaid >}}

Use this for verification, not for shipping production builds. `master` is active development. It gives you speed, not a stability promise.

## Why It Is Tiered

The small overhead of fetching versioned artifacts is noticeable, but that is not the real reason this is limited by account level.

The hard cost is support.

If a free user reports a build issue from six months ago, and another user reports one from yesterday, and an enterprise customer needs a diagnosis against a two-month-old version, support becomes a bisection problem across a moving build system. The further back the community goes, the harder every issue is to understand, reproduce, and fix.

That does not mean versioned builds should only exist at the top. The new approach opens them much further down than before, including limited access for basic/free usage. The longer windows still belong to higher tiers because those teams are the ones that need older reproducible builds and the support time that comes with them.

The principle is simple: the tier buys a support window and capacity, not a right to keep your app revenue.

## A Practical Workflow

When a build starts failing after an update:

```properties
# First, verify the last known good release.
build.cn1Version=7.0.182
```

If that works, the regression is likely in the framework or build server. If it still fails, the issue is probably in your app, dependencies, native configuration, or a platform toolchain change that affects the old version too.

When a fix lands:

```properties
# Then verify the unreleased fix.
build.cn1Version=master
```

Once the fix is released, pin to the release you plan to ship with or remove the hint to follow the current default again.

## What This Does Not Solve

Versioned builds are not a time machine for every external dependency. Apple, Google, OS SDKs, certificates, signing rules, app store requirements, Maven repositories and native tools all move. A six-month-old framework can still be affected by a current Xcode or Android requirement.

It also does not make `master` a nightly release channel. It is a verification channel.

That distinction keeps the feature useful without turning support into archaeology for every build ever made.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
