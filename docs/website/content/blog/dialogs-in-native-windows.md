---
title: "A Dialog Can Now Be a Native Desktop Window"
slug: dialogs-in-native-windows
url: /blog/dialogs-in-native-windows/
date: '2026-09-05'
author: Shai Almog
description: "Codename One dialogs can stay inside a secondary window or become native modal windows, while sheets, popups, overlays, tooltips, HTML, and accessibility now resolve the correct top level."
feed_html: '<img src="https://www.codenameone.com/blog/dialogs-in-native-windows.jpg" alt="A modal dialog above a native desktop window" /> Codename One dialogs can stay inside a secondary window or become native modal windows, while sheets, popups, overlays, tooltips, HTML, and accessibility now resolve the correct top level.'
series: ["release-2026-09-04"]
---

![A modal dialog above a native desktop window](/blog/dialogs-in-native-windows.jpg)

Last week's native-window release could open an editor, inspector, and tool palette as separate operating-system windows. Then an ordinary `Dialog.show()` inside the inspector looked for the current `Form` and appeared on the wrong surface.

That bug exposed every component that treated “top level” and “form” as synonyms. [PR #5624](https://github.com/codenameone/CodenameOne/pull/5624) fixes those assumptions and adds something more visible: a `Dialog` can now become a real modal desktop window.

For VoIP, VPN, the AppKit port, OTP, contacts, and the rest of this release, read the [weekly overview](/blog/voip-vpn-builders/).

## Two useful kinds of dialog

The default remains a lightweight dialog painted inside its owner's layered pane. It matches Codename One styling and works on every port. A desktop application can opt one instance into an operating-system window:

```java
Dialog confirm = new Dialog("Confirm");
confirm.add(new Label("Delete the document?"));
confirm.setNativeWindowMode(true);

Command result = confirm.showDialog();
```

![A Codename One dialog dimming its owning AppKit window](/blog/dialogs-in-native-windows/appkit-dialog.png)

_The same Dialog API rendered inside its AppKit owner. Native-window mode moves the dialog into its own operating-system window._

An application can set the default globally with `Dialog.setDefaultNativeWindowMode(true)`, or through the `defaultNativeWindowModeBool` theme constant. The instance setting wins over the static default, which wins over the theme. On a port without native windows, the dialog stays lightweight. There is no second code path to maintain.

Native-window mode is useful when the dialog must participate in desktop window ordering, focus, or task switching. Lightweight mode is usually better for a small prompt that should inherit the exact visual treatment of its owner.

## Ownership was the real bug

`Dialog`, `Sheet`, `ToastBar`, `ComboBox`, `FloatingActionButton`, `InfiniteProgress`, tooltips, and `HTMLComponent` all had paths that asked `Display` for the current form. That question has one answer, even when the event came from another window.

The corrected path starts with the component that caused the action:

{{< mermaid >}}
flowchart TD
    E[Event source component] --> T[getTopLevelContainer]
    T --> F[Form layered pane]
    T --> W[Window layered pane]
    F --> O[Overlay or popup]
    W --> O
    W --> N[Optional native dialog window]
{{< /mermaid >}}

Code that creates a dialog without a source component can bind it explicitly:

```java
Dialog details = new Dialog("Details");
details.setTopLevelHost(inspectorWindow);
details.add(new Label("Selection metadata"));
details.show();
```

The dimming layer, popup position, focus restoration, repaint region, and input routing now belong to that window. Accessibility state is also maintained per top level, so opening an inspector does not replace the main form's accessible root.

## Popups should not become surprise windows

An anchored combo-box popup stays attached to its field. A floating-action submenu stays near its button. Turning either into an independent desktop window would break positioning and keyboard behavior, so the new native mode is limited to dialogs and interaction dialogs.

`Picker` inside a window still uses its lightweight popup. `Toolbar` remains a `Form` concept. A transition cannot animate one operating-system window into another because the two surfaces do not share a graphics context. Change content inside one window when an animated transition matters:

```java
inspectorWindow.setContent(nextPanel, CommonTransitions.createFade(250));
```

Those limits are explicit because a fake cross-window animation would be less predictable than no animation.

## The same contract on Windows and Mac

![A Codename One dialog dimming its owning native Windows window](/blog/dialogs-in-native-windows/windows-dialog.png)

_Native Windows runs the same dialog conformance case._

The test suite opens the same controls in 400 by 300, 900 by 700, and 1000 by 400 windows. The wide case catches code that still reads the main display width. Modal cases verify that the owner is blocked while other event-dispatch work can continue.

This also validates the new {{< post-link path="/blog/native-appkit-mac-port" text="AppKit port" >}} against the existing Windows implementation. A platform port is much easier to trust when it must pass behavior captured by another native port instead of defining success for itself.

## Desktop work without a desktop fork

The Window API is settling into the same pattern as this week's call, VPN, and contact work. The common API owns the behavior application code can rely on. Each port handles the native surface it actually has. Unsupported behavior stays visible instead of becoming a silent approximation.

That consistency matters for security as well as polish. A confirmation must block the window that contains the sensitive operation. A popup must not leak input to another surface. Accessibility focus must remain attached to the content the user is operating. These details are small until the wrong window accepts a command.

Next, read how {{< post-link path="/blog/native-appkit-mac-port" text="Catalyst gave way to a real AppKit application" >}}.

---

## Discussion

_Which dialog in your desktop application needs its own operating-system window, and which should remain an overlay?_

{{< giscus >}}
