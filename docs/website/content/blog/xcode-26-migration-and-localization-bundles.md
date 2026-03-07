---
title: Xcode 26 Migration, UIScene Rollout, Initializr Localization Bundles, and More
date: '2026-03-07'
author: Shai Almog
slug: xcode-26-migration-and-localization-bundles
url: /blog/xcode-26-migration-and-localization-bundles/
description: We started the Xcode 26 migration rollout, Initializr now generates localization resource bundles, and there are several useful platform updates you should know about.
feed_html: '<img src="https://www.codenameone.com/blog/xcode-26-migration-and-localization-bundles.jpg" alt="Xcode 26 Migration, UIScene Rollout, Initializr Localization Bundles, and More" /> We started the Xcode 26 migration rollout, Initializr now generates localization resource bundles, and there are several useful platform updates you should know about.'
---

![Xcode 26 Migration, UIScene Rollout, Initializr Localization Bundles, and More](/blog/xcode-26-migration-and-localization-bundles.jpg)

A lot landed since the last update, so I want to give you a practical summary of what changed and what matters.

## Xcode 26 Migration Has Started

We started the Xcode 26 migration process.

This is a staged rollout, which means some iOS builds now run on the newer Xcode and others still run on the previous setup. If you hit a suspicious failure, please report it quickly so we can separate migration fallout from ordinary regressions.

The migration is tracked in issue [#4456](https://github.com/codenameone/CodenameOne/issues/4456).

## UIScene Support: Start Testing with `ios.uiscene`

We also added initial UIScene support and this is one you should start testing now. The `ios.uiscene` flag enables this behavior and we expect UIScene support to become an Apple requirement starting with iOS 27, so we are trying to be ready ahead of time instead of scrambling at the last minute.

## Initializr Now Generates Localization Resource Bundles

The new Initializr now generates localization bundles as part of project creation, including UI support and preview in the wizard.

That means i18n starts as part of the default app flow instead of becoming cleanup work later on. We also fixed follow-up l10n path issues and updated CSS tooling so localization directories are detected and passed correctly to the compiler/watch workflow.

If you haven’t looked at localization recently, we now support property resource bundles as part of the standard workflow. This is documented in the [Developer Guide](/manual/), and it aligns much better with modern project structure.

Another detail that is easy to miss: Codename One has implicit localization behavior. When a resource bundle is installed, `Label` text (and subclasses of [`Label`](/javadoc/com/codename1/ui/Label/)) is localized automatically by key unless you explicitly disable it for a specific component.

For day to day work, the simulator has a very useful mode under **Simulator > Auto Update Default Bundle**. When enabled, missing keys are implicitly added to your default properties bundle while you run the app, which makes it much easier to iterate on UI text and l10n coverage.

## Other Updates Worth Calling Out

Android now has support for adaptive icons, with documentation updates to clarify background image path behavior.

On iOS we fixed local notification replacement behavior, so newer notifications can override previous ones correctly. If you rely on notifications heavily, check the [`LocalNotification`](/javadoc/com/codename1/notifications/LocalNotification/) JavaDoc for the API details.

In the UI layer, [`ImageViewer`](/javadoc/com/codename1/components/ImageViewer/) now supports optional navigation arrows and a thumbnail strip. We also introduced `EditableResources` and [`CSSThemeCompiler`](/javadoc/com/codename1/ui/css/CSSThemeCompiler/), which should make theme and resource workflows easier to automate and evolve.

We also expanded regex support to include nested POSIX character classes, with additional tests around that behavior.

## Looking Ahead: JDK 17 Support

I also want to close with something I have been working on for the past few months: JDK 17 support, tracked in issue [#4577](https://github.com/codenameone/CodenameOne/issues/4577).

This is a big effort. It requires major rework across platforms and targets. Java 9 introduced serious compatibility breaks for us between the module system, removed packages, and invokedynamic implications, so getting this right is not a small patch.

Newer VMs beyond 17 will likely need separate tracking because that transition is another major shift by itself. Android already supports 17, so that is our first target milestone.

Right now CI runs with JDK 11, 17, and 21, but you still can’t build a Codename One application with JDK 17 end-to-end just yet.

Streams are a special case since they are mostly an API layer issue. We may include that as part of this effort, although there is already a cn1lib for streams: [CN1-Stream](https://github.com/diamonddevgroup/CN1-Stream) by [Diamond Mubaarak](https://github.com/diamondobama).

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
