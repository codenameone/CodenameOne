---
title: "A dialog must know which window asked for it"
slug: 2026-09-05-0400-codenameone-dialogs-native-windows
platform: linkedin
account: codenameone
source_slug: dialogs-in-native-windows
publish_at: '2026-09-05T04:00:00'
timezone: Asia/Jerusalem
image: /blog/dialogs-in-native-windows.jpg
---

Last week's desktop release could open an editor and inspector as separate native windows.

Then a dialog inside the inspector asked for the current Form and appeared on the wrong surface.

The fix reached beyond Dialog. Sheets, toast bars, combo-box popups, floating-action submenus, progress overlays, tooltips, HTML, and accessibility now resolve the top-level container that owns the event.

Dialog also gained an opt-in native-window mode. It can remain a lightweight Codename One overlay or become a real modal operating-system window. Instance configuration wins over the application default, which wins over the theme. Unsupported ports keep the lightweight behavior.

Anchored popups do not become surprise desktop windows. Toolbars remain Form-bound. Transitions stay inside one operating-system surface because two native windows have no shared graphics context for an in-between frame.

Window support becomes useful when ordinary components follow ownership, focus, modality, paint, and accessibility to the correct surface.

{{canonical}}
