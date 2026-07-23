# Evidence map

Source: `docs/website/content/blog/ios-density-scroll-and-accessibility.md`
Canonical: https://www.codenameone.com/blog/ios-density-scroll-and-accessibility/

## Thesis

How stale device-density data distorted modern iPhone rendering and accessibility previews

## Supported beats

- **The iOS PPI Table Was Out of Date:** The iOS port converts between physical millimetres and pixels using a per-device PPI lookup keyed on the screen's pixel dimensions. That table had not been updated in a long time.
- **Previewing Larger Fonts in the Simulator:** A personal aside: my eyes are not what they used to be. Somewhere in the last few years I stopped being able to read small text on my phone/laptop the way I used to, and over time I have found myself quietly cranking the system font size up a notch, then another notch.
- **iOS Scroll, One More Time:** Scroll feel on iOS has been through more iterations in this framework than I can reliably count. Every time I have thought we had it right, somebody would flick a list on a new device and the whole thing would feel half a beat off.
- **Localized App Icons:** The workflow is file-driven. Drop per-locale PNGs into common/src/main/resources using the naming convention cn1_icon_[_].png.
- **UIManager.zoomFonts(factor):** The missing piece alongside the simulator Dynamic Type menu was a programmatic way to apply a scale at runtime. UIManager.zoomFonts(factor) does exactly that. It multiplies every scalable font in the current theme (and the default styles) by the factor you pass.
- **Playground: New UI, More Java:** The short version: it now feels like a modern developer tool. The design vocabulary is closer to the current IntelliJ or Visual Studio Code than to "scripting textarea with a preview pane bolted on." It just feels like a real product now.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues/4770
- https://openjdk.org/jeps/460
