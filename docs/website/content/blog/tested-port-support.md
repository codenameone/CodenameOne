---
title: "Port Support You Can Trace Back to a Green Test"
slug: tested-port-support
url: /blog/tested-port-support/
date: '2026-07-22'
author: Shai Almog
description: "The Codename One port status page maps 49 feature groups across 10 targets to current conformance reports, environment details, skip explanations, and dated CI evidence instead of a manually maintained support table."
feed_html: '<img src="https://www.codenameone.com/blog/tested-port-support.jpg" alt="Codename One port support generated from CI evidence" /> The Codename One port status page maps 49 feature groups across 10 targets to dated CI evidence and explicit skip explanations.'
series: ["release-2026-07-17"]
---

![Port Support You Can Trace Back to a Green Test](/blog/tested-port-support.jpg)

“Supported on iOS, Android, desktop, and web” sounds useful until you need one method on one target. Does WebSocket work on watchOS? Which Linux architectures do we build? Was the JavaScript media test green this week, or did somebody update a table six months ago and forget it?

[PR #5389](https://github.com/codenameone/CodenameOne/pull/5389) turns those questions into the [Codename One Port Status page](/port-status/). It maps 49 user-facing feature groups across 10 portability targets to current conformance results, environment data, skip reasons, and the date of the run.

## The table is an output, not an opinion

The HelloCodenameOne suite already exercises APIs and screenshot goldens on Android, iOS, tvOS, watchOS, JavaScript, native Linux, native Windows, and Mac Catalyst. The missing part was a contract that translated thousands of test cases into a stable public vocabulary.

The new conformance mapping connects registered tests and screenshots to rows such as networking, media, databases, maps, notifications, input, accessibility, and 3D. CI normalizes each port's result into the same report format. A publishing workflow writes the latest reports to a data-only branch. The website consumes those reports and renders the matrix.

{{< mermaid >}}
flowchart LR
    A["Port CI jobs"] --> B["HelloCodenameOne tests"]
    B --> C["Normalized conformance report"]
    D["Feature-to-test contract"] --> E["49 public feature rows"]
    C --> F["Data-only status branch"]
    E --> F
    F --> G["/port-status/"]
    G --> H["490 target-feature cells"]
    G --> I["Environment and run date"]
    G --> J["Skip and scope explanations"]
{{< /mermaid >}}

The page currently renders 490 feature cells. Ten targets appear because architectures and renderer variants matter. iOS Metal and legacy OpenGL are separate evidence paths. Windows x64 and ARM64 are separate. Linux x64 and ARM64 are separate.

JavaSE is deliberately excluded from the public portability matrix. It is the simulator and development runtime, not one of the deployed native targets the table is meant to prove.

## A green cell has a chain of evidence

Each status report records the commit, environment, registered tests, outcome, duration, and skipped cases. The website data also records the runtime used for browser and platform evidence. For example, the current browser environment file names the Chromium, Firefox, and WebKit engine versions rather than saying “modern browsers.”

The contract itself is validated in CI:

```bash
python3 scripts/hellocodenameone/conformance/port_status.py validate
python3 -m unittest -v \
  scripts/hellocodenameone/conformance/test_port_status.py
node scripts/website/validate_port_status.mjs
```

Validation fails if a registered conformance test or screenshot golden is orphaned from the public mapping, if the mapping points to a test that no longer exists, or if the published reports do not satisfy the schema. That makes the page part of the test system instead of a second table someone has to remember to edit.

## Skipped does not always mean unsupported

Some tests cannot run meaningfully on a target even when the API works. A device-only test may need hardware. A store API may require credentials. A screenshot can be irrelevant on watchOS because the phone-sized fixture is cropped before it tests the intended behavior.

The status page keeps skips visible and attaches a reason. It does not silently convert every skip to “unsupported,” and it does not turn every skip into a green claim either. This distinction is important when a feature is supported but its current CI proof covers only part of the behavior.

The deployment section applies the same standard to minimum versions. It separates the declared floor from the environment CI actually ran. An iOS build may compile with an iOS 14 deployment target while hosted CI runs the current Xcode 26 simulator. The page says both. A compiled floor is evidence, but it is not the same as running on an iOS 14 device.

## Benchmarks use the same application

The page also carries ten common workloads through each generated application: integer and long arithmetic, transcendental math, sequential and random arrays, allocation, map churn, string building, recursion, and quicksort.

```text
3 warm-up runs
5 measured runs
report the minimum measured time
verify the workload checksum
```

These are absolute per-target timings, not a claim that an ARM watch should beat a desktop CPU. Their value is trend detection and a common workload inside the actual generated port application.

Binary size and memory are intentionally absent. The current artifacts mix compressed Android and web packages with unpacked Apple bundles and native executables. The ports also report different memory concepts. Publishing those numbers in one comparison row would look precise while measuring different things. They will return when a dedicated release-mode fixture packages and samples every target consistently.

## What a green test cannot prove

A green status means the mapped tests passed in the named environment at the recorded commit. It does not prove that no application can hit a bug. It does not extend the test to OS versions, devices, drivers, or permissions that the run did not exercise.

That boundary is why the page exposes details instead of collapsing everything to a marketing checkmark. You can inspect the target environment, last run, mapped coverage, and reason for an exception. If the proof is narrower than your requirement, the page should make that visible before you commit to a platform.

This also changes how we review a new API. Adding the Java class is no longer enough. A feature needs a conformance test, a mapping to a public capability, and green results on the ports we claim. If a port intentionally does not implement it, that scope must be explicit.

## One place to start a platform decision

Use the [Port Status page](/port-status/) when you need the current deployment floors, architecture coverage, API evidence, browser engines, or common-workload results. Then follow the linked test detail for the part your application depends on.

This closes the week's series: [measured native-theme fidelity](/blog/pixel-perfect-is-a-test/), a [standalone Settings tool](/blog/standalone-codename-one-settings/), [external surfaces](/blog/widgets-live-activities-dynamic-island/), [portable accessibility semantics](/blog/accessibility-semantics/), and an [MCP server built on that semantic tree](/blog/codename-one-mcp-server/). The common thread is not the number of features. It is turning claims into artifacts you can inspect.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
