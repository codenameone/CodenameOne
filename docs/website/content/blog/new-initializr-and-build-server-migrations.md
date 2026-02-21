---
title: New Initializr and Build Server Migration Plan
date: '2026-02-21'
author: Shai Almog
slug: new-initializr-and-build-server-migrations
url: /blog/new-initializr-and-build-server-migrations/
description: We are launching a new Codename One Initializr and sharing the staged migration plan for Android and iOS build servers.
feed_html: '<img src="https://www.codenameone.com/blog/new-initializr-and-build-server-migrations/new-initializr-and-build-server-migrations.png" alt="New Initializr and build server migrations" /> We are launching a new Codename One Initializr and sharing the staged migration plan for Android and iOS build servers.'
---

![New Initializr and build server migrations](/blog/new-initializr-and-build-server-migrations/new-initializr-and-build-server-migrations.png)

We have a few important updates that affect our tooling and build infrastructure.

The short version:

- The new **Initializr** is now available at [/initializr/](/initializr/).
- We will retire the old Initializr on **February 27, 2026**.
- Android build server upgrades start first, beginning the weekend of **February 27-28, 2026**.
- iOS (Mac) build server upgrades start on **Thursday, March 5, 2026**.

## The New Initializr Is Built with Codename One

The new Initializr is implemented using **Codename One** itself.

This is great dogfooding for us: we use our own stack for one of the most important entry points into the platform.
It also makes maintenance significantly easier for our team.

Another major change is architecture:

- The new Initializr runs on the **client machine**.
- It no longer depends on a backend service for core operation.

That gives us a simpler deployment model, fewer moving parts, and less backend operational overhead.

## UX Improvements and Feature Parity

The new Initializr includes a better user experience while preserving the existing functionality people rely on.

Improvements include:

- Dark mode support.
- Basic UI styling support.
- Existing Initializr features carried forward.

Our plan is to take the original Initializr offline on **February 27, 2026**.

If anything feels off in your workflow, please report it right away so we can fix it quickly.

## Build Server Upgrades: Android First, Then iOS

We are also starting staged infrastructure upgrades for our build servers.

The relevant tracking issues are:

- Mac/iOS build server update: [Issue #4456](https://github.com/codenameone/CodenameOne/issues/4456)
- Android build server update: [Issue #4466](https://github.com/codenameone/CodenameOne/issues/4466)

We will start with Android on the weekend of **February 27-28, 2026**.

The iOS upgrade starts on **March 5, 2026 (Thursday)**.
This part is more challenging because it requires an operating system upgrade on the Mac infrastructure, and unlike Android, it is not something we can trivially roll back once we begin.

We might also need coordination with our hosting provider during this stage, which is another reason we are sequencing the rollout carefully.

## Please Report Problems Immediately

As usual, if you notice anything wrong with builds or any other Codename One service, tell us immediately.

Fast reporting helps us isolate and fix issues before they affect more users.

## Community Support: Use GitHub Discussions

One more important point: **GitHub Discussions** is now the best place to get community support from us:
[https://github.com/codenameone/CodenameOne/discussions](https://github.com/codenameone/CodenameOne/discussions)

It is also the same system used for comments under blog posts, so discussions stay in one place.
For support, this is now better than Reddit and much better than Stack Overflow for Codename One-specific questions.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
