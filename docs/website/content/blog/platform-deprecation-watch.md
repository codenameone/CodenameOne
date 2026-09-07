---
title: "We Stopped Waiting for Platform Changes to Find Us"
slug: platform-deprecation-watch
url: /blog/platform-deprecation-watch/
date: '2026-09-08'
author: Shai Almog
description: "A daily Codex task now reads official Apple and Google notices, traces each requirement to Codename One source and build output, and turns verified gaps into fixes before the deadline."
feed_html: '<img src="https://www.codenameone.com/blog/platform-deprecation-watch.jpg" alt="A daily platform notice moving through source checks into a fix" /> A daily Codex task now reads official Apple and Google notices, traces each requirement to Codename One source and build output, and turns verified gaps into fixes before the deadline.'
series: ["release-2026-09-04"]
---

![A daily platform notice moving through source checks into a fix](/blog/platform-deprecation-watch.jpg)

For years, our warning system for Apple and Google changes was a developer whose build failed first. The community gave us excellent reports, but the sequence was backwards. A platform owner announced a deadline, time passed, an application hit the change, and only then did we trace it into the builder.

This week we started a daily scheduled Codex task that reads official notices and checks them against Codename One. Its first useful results include Android 16 back handling in [PR #5673](https://github.com/codenameone/CodenameOne/pull/5673) and the permission-safe Contact Picker in [PR #5680](https://github.com/codenameone/CodenameOne/pull/5680).

For the rest of the release, read the [weekly overview](/blog/voip-vpn-builders/). The Contact Picker has a separate {{< post-link path="/blog/private-contact-picker" text="code-focused article" >}}.

## A notice is the start of an investigation

Scheduled browsing alone would create noise. Platform pages change wording, policies overlap, and a requirement may already be handled in a builder branch that application code never sees. The task must connect the notice to an artifact we produce.

{{< mermaid >}}
flowchart TD
    N[Official Apple or Google notice] --> R[Extract requirement<br/>scope and date]
    R --> P[Trace CN1 API<br/>builder or generated target]
    P --> D{Already handled?}
    D -->|Yes| E[Record source and build evidence]
    D -->|No| T[Reproduce or inspect artifact]
    T --> F[Patch or focused issue]
    F --> V[Run affected tests<br/>and inspect output]
{{< /mermaid >}}

We require a primary source, an applicability test, and a concrete Codename One producer before filing work. A release-note headline is not enough. For native libraries, a Gradle setting is not enough either; the packaged binary may need inspection.

The scheduled task does not merge code or declare support from a search result. It narrows the distance between a platform announcement and an evidence-backed change.

## Android 16 changed the Back contract

Applications targeting Android 16 no longer receive the old `onBackPressed()` path in the situations Codename One depended on. Without a registered predictive-back callback, Back could leave the application instead of navigating its form stack.

Google's [target API requirements](https://developer.android.com/google/play/requirements/target-sdk) moved application updates to Android 16. The fix registers an `OnBackInvokedCallback` bridge and routes it into the existing Codename One back command. The Android builder still compiles against an older SDK surface in parts of the pipeline, so the bridge uses reflection and a proxy rather than linking the newer class directly.

```text
System Back gesture
        |
OnBackInvokedCallback
        |
PredictiveBackBridge
        |
Codename One back command
```

Input methods can emit both a legacy key event and the new callback. The bridge suppresses the duplicate so one gesture does not navigate twice. Predictive progress animation is not part of this change; correctness came first.

This is exactly the kind of regression that can hide until a target-SDK deadline changes which platform path is active. The daily scan connected the policy date to the generated Android activity and its runtime behavior.

## Contact policy became an API design task

Google's [contacts permission policy](https://support.google.com/googleplay/android-developer/answer/16909972) is not solved by changing one manifest line. Many applications ask for an address-book permission only to let the user choose one person. The appropriate response is a narrower interaction, not a more elaborate permission dialog.

That led to `ContactPicker`, which uses the Android system picker on Android 17, a permission-free single-contact fallback on older Android versions, and `CNContactPickerViewController` on iOS. The picker returns the selected snapshot. It does not grant the application a standing right to query the address book.

The {{< post-link path="/blog/private-contact-picker" text="next article" >}} covers requested fields, multi-selection limits, snapshots, fallbacks, and builder permission detection.

## What this changes for release work

The immediate changes matter. The larger gain is a repeatable process:

1. Read Apple and Google primary sources every day.
2. Extract a date, affected target, and technical condition.
3. Search issues and merged code before creating work.
4. Trace the actual producer, including build-server generators and packaged artifacts.
5. Reproduce the gap or prove the current output already satisfies it.
6. Patch and test the narrow affected path.

The developer guide overhaul in this release follows the same idea. Structure, cross-references, missing code listings, links, images, and prose now have CI gates. A problem found by automation becomes a specific repair instead of a vague promise to keep the docs current.

## Ahead of the deadline, with evidence

Automation will miss notices, misread scope, and find changes that do not apply. That is why the result is a source trail rather than an autonomous verdict. We can inspect the notice, the relevant builder, the generated product, the tests, and the merged fix.

Calls, VPN, OTP, contacts, billing, and platform navigation all sit near permissions or operating-system policy. Finding changes earlier gives us time to choose the narrower API instead of making a deadline patch that widens access. It also moves security work into the normal release cycle, where it can be reviewed beside the code it protects.

The next article shows how to {{< post-link path="/blog/private-contact-picker" text="pick one contact without asking for the address book" >}}.

---

## Discussion

_Which platform deadline caused the most avoidable emergency in your release process?_

{{< giscus >}}
