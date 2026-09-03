---
title: "A Mac App, Not an iOS App on a Mac"
slug: native-appkit-mac-port
url: /blog/native-appkit-mac-port/
date: '2026-09-06'
author: Shai Almog
description: "Codename One's macOS build now targets AppKit directly, with NSWindow, NSMenu, NSTextInputClient, NSScreen, and an independent Metal surface for every window."
feed_html: '<img src="https://www.codenameone.com/blog/native-appkit-mac-port.jpg" alt="A Codename One application running as a native AppKit Mac app" /> Codename One&apos;s macOS build now targets AppKit directly, with NSWindow, NSMenu, NSTextInputClient, NSScreen, and an independent Metal surface for every window.'
series: ["release-2026-09-04"]
---

![A Codename One application running as a native AppKit Mac app](/blog/native-appkit-mac-port.jpg)

Mac Catalyst runs an iPad application on macOS. That was a useful bridge for Codename One, but it remained a bridge. Once we added multiple desktop windows, the missing desktop operations and rendering compromises stopped being edge cases.

[PR #5601](https://github.com/codenameone/CodenameOne/pull/5601) changes the default native Mac build to AppKit. The result is an `NSApplication` with `NSWindow`, `NSMenu`, `NSTextInputClient`, `NSScreen`, and a `CAMetalLayer` for each rendered window. It is a Mac application rather than an iOS application asking macOS to translate the experience.

For the complete release, including calls, VPN, OTP, contacts, and Android billing, read the [weekly overview](/blog/voip-vpn-builders/).

## Catalyst was doing expensive work

The Catalyst port rendered a secondary Codename One window into a mutable image and then copied that image into the scene. A 4K BGRA surface is roughly 33 MB before another buffer or copy enters the picture. Moving or repainting several windows multiplies that cost.

AppKit gives each `Window` its own Metal-backed drawable:

{{< mermaid >}}
flowchart LR
    subgraph Catalyst
        C1[Codename One paint] --> B[Mutable image buffer]
        B --> U[UIKit scene copy]
    end
    subgraph AppKit
        A1[Codename One paint] --> M[Per-window CAMetalLayer]
        M --> N[NSWindow presents]
    end
{{< /mermaid >}}

Dirty regions stay attached to one native window. A repaint in an inspector does not need an intermediate image the size of the editor window. Monitor scale comes from the `NSScreen` that owns the window, not from an iOS scene approximation.

![Codename One components inside a native AppKit secondary window](/blog/native-appkit-mac-port/appkit-window.png)

_A Codename One component tree rendered into an AppKit window at 900 by 700._

## Desktop operations are desktop operations again

The AppKit port implements always-on-top, utility windows, minimize, restore, maximize, modality, undecorated windows, and native dirty clipping. Menus use `NSMenu`. Text editing implements `NSTextInputClient`, including composition and input-method interaction. Multiple monitors come through `NSScreen` with their independent work areas and scale factors.

Those details matter to applications such as editors, trading terminals, control rooms, and internal tools. A utility palette that cannot remain above its owner is not a utility palette. A text editor that bypasses the input method works only for the developer who tested it.

The {{< post-link path="/blog/dialogs-in-native-windows" text="dialog work" >}} builds on this port. A dialog may stay in its owner's layered pane or become a native modal window. Both paths now have a real Mac window model underneath them.

## Existing build targets, different native foundation

The normal targets keep their names:

```bash
mvn -B \
  -Dcodename1.platform=ios \
  -Dcodename1.buildTarget=mac-source \
  package

mvn -B \
  -Dcodename1.platform=ios \
  -Dcodename1.buildTarget=mac-os-x-native \
  package
```

`mac-source` produces an Xcode project under the build target directory. `mac-os-x-native` produces the signed native result through the build service. Existing projects do not need a new application API.

Catalyst has no separate build target. An application that depends on it can preserve the previous behavior by setting the existing hint and building an ordinary iOS target:

```properties
codename1.arg.macNative.enabled=true
```

Use `ios-source` for a local Xcode project or `ios-device` for a build-server build. Catalyst is no longer the default definition of “native Mac.”

## A port is a contract, not a screenshot

The pull request ran the full native screenshot suite, 324 cases at merge time, plus platform and input tests. The same window layouts are captured at several aspect ratios. Text input, mouse routing, focus, resizing, modality, and screen scale have dedicated coverage.

Accessibility follows the window model too. `IOSNative.updateAccessibilityTree()` sends the framework semantics tree to `CN1MacAccessibilityUpdateTree()`, which installs a hierarchy of `NSAccessibilityElement` children on the relevant window. Roles, labels, values, states, standard and custom actions, and live-region announcements reach VoiceOver through AppKit instead of stopping at the portable tree.

Platform services with a UIKit-only implementation still need individual AppKit work. Switching the default port does not make those integrations native to macOS automatically.

## Another Tuesday

A new Codename One platform port once deserved an entire release cycle and its own launch. This week the AppKit port arrived beside VoIP, VPN, phone verification, private contact selection, native dialogs, billing, browser density, and a documentation overhaul. A new native platform has become another Tuesday.

The common component model and builder contract carry most of the application unchanged. The Mac port owns native windows, input, menus, text, and rendering. It does not require a second application UI or a Mac-specific navigation architecture.

The same division strengthens secure defaults. Native platform integration belongs in a reviewed port and build pipeline, not in copied project fragments that drift across application repositories. Applications keep their own interface and data model. The builder supplies the narrow native product needed to run them.

Next, the {{< post-link path="/blog/sms-otp-autofill" text="OTP article" >}} removes an inbox permission from a common verification flow.

---

## Discussion

_What Mac behavior has been hardest to preserve when sharing code with mobile?_

{{< giscus >}}
