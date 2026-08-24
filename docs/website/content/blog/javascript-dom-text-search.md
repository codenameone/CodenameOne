---
title: "JavaScript Find in Page: DOM Text Above the Codename One Canvas"
slug: javascript-dom-text-search
url: /blog/javascript-dom-text-search/
date: '2026-08-23'
author: Shai Almog
description: "The Codename One JavaScript port now renders visible text into a targeted DOM layer, making it searchable, selectable, accessible, and sharp on high-DPI displays without giving browser layout control."
feed_html: '<img src="https://www.codenameone.com/blog/javascript-dom-text-search.jpg" alt="Searchable browser text layered above a Codename One canvas and accessibility tree" /> The Codename One JavaScript port now renders visible text into a targeted DOM layer, making it searchable, selectable, accessible, and sharp on high-DPI displays without giving browser layout control.'
series: ["release-2026-08-21"]
---

![Searchable browser text layered above a Codename One canvas and accessibility tree](/blog/javascript-dom-text-search.jpg)

Browser search cannot find pixels. That was the JavaScript port's text model: Codename One drew each glyph onto a canvas, so a visible label was invisible to find-in-page, text selection, and ordinary browser text machinery.

[PR #5552](https://github.com/codenameone/CodenameOne/pull/5552) keeps the canvas renderer and promotes eligible visible text into a DOM layer. Codename One still measures and places every run. The browser handles the part it is better at: text rasterization, selection, search, accessibility, input metadata, and device-pixel resolution.

For encrypted SQLite and the rest of this week's work, see the [weekly release overview](/blog/sqlite-portable-encrypted/).

## A DOM component tree was the wrong trade

Replacing each Codename One component with a DOM element would hand layout and paint ordering to the browser. It would also create two UI implementations to keep consistent.

We kept application components on the canvas and projected only the text and accessibility data the browser needs:

{{< mermaid >}}
flowchart TB
    A[Codename One layout and paint] --> B[Canvas<br/>shapes images transformed text]
    A --> C[Text layer<br/>eligible visible text runs]
    A --> D[Accessibility tree<br/>incremental ARIA projection]
    E[Browser input] --> F[Positioned input or textarea]
    B --> G[Composited page]
    C --> G
    D --> G
    F --> G
{{< /mermaid >}}

The page now has a shape similar to this:

```html
<canvas role="presentation" aria-hidden="true"></canvas>
<div id="cn1-text-layer" aria-hidden="true">
    <span style="position:absolute; white-space:pre">Account balance</span>
</div>
<div id="cn1-accessibility-tree" aria-label="Account balance"></div>
```

The text run arrives already broken and positioned. `white-space: pre` prevents the browser from wrapping it. Text measurement stays on the worker's `OffscreenCanvas`, so promoting a run does not alter layout.

## Rebuilding the overlay broke the browser features

The accessibility projection previously cleared its container and recreated every element on each invalidation. Scrolling changes component bounds repeatedly, which meant scrolling also destroyed DOM focus and any active text selection.

The overlay now diffs the semantic tree. Stable nodes keep their element identity and listeners while their bounds or content change. The browser can maintain focus and selection because the application is no longer replacing the element under it.

The real-browser verification tags semantic elements with a JavaScript property, triggers invalidation, then reads the property again. Rebuilding the tree would lose the tag and fail the test. That makes incremental behavior part of the port contract rather than a performance assumption.

## High-DPI displays use their native resolution

The old host code pinned `devicePixelRatio` to one. A 375 by 667 CSS box therefore had a 375 by 667 backing canvas even on a display with a pixel ratio of two. The browser enlarged that bitmap to the physical pixels.

The new path uses a 750 by 1334 backing surface behind the same 375 by 667 CSS box at a device-pixel ratio of two. Coordinates are converted at the DOM boundary, while Codename One continues to address device pixels internally.

```text
CSS layout:      375 x 667
devicePixelRatio:        2
backing surface: 750 x 1334
```

The port also refreshes the ratio when browser zoom changes or a window moves between displays. Screenshot tests can still pin a specific ratio through the existing query parameter.

## Inputs now tell the browser what they mean

Text editing already used a positioned native input. The new work fixes and extends the metadata the browser reads:

- `inputmode` selects the appropriate on-screen keyboard.
- `autocomplete` connects password managers and autofill.
- `autocapitalize` and `spellcheck` follow the Codename One constraints.
- The component name becomes the field name.
- `cn1$autocomplete` can override the autocomplete token.

A password field bug also disappeared. `PASSWORD` is a bit flag, but the old code compared the entire constraint value. `PASSWORD | EMAILADDR` missed the password case and fell through to a clear-text input.

Several browser APIs had silently broken when the port moved into a worker. A `@JSBody` executes in that worker, so direct access to `history`, `matchMedia`, or other window-owned objects fails. Host bindings now restore browser history, dark-mode detection, reduced-motion and forced-color queries, and cursor support.

## Some text stays on the canvas

The DOM layer is targeted. Text stays on the canvas when the DOM cannot reproduce the paint operation without changing its meaning:

- Offscreen targets, transition buffers, `ComponentImage`, and screenshot rendering
- Cell renderers whose single component instance is stamped at several positions
- Text outside the displayed form when a dialog paints another form as its backdrop
- Shape clips, transformed text, and bitmap fonts

Drag selection is also disabled for now. The text layer takes no pointer events so the canvas keeps owning hit testing. Find-in-page and keyboard selection work without that change. Mouse drag selection needs pointer routing that can distinguish text selection from component gestures.

Vertical placement can differ from browser font metrics by roughly one pixel. A sheet covering text inside the same form can also leave that text represented in the DOM until same-form occlusion is modeled.

## Search works without handing layout to the browser

The JavaScript port still ships the Codename One UI. A browser cannot reflow a label, substitute a component, or change a layout after an OS update. The application owns those decisions.

The browser now sees enough structure to do what users expect from a web page. Search finds text. Assistive technology receives stable semantic nodes. Password managers recognize fields. High-DPI screens render at their actual pixel density.

The {{< post-link path="/blog/smart-home-homekit-matter" text="next post maps HomeKit, Matter, and Google Home without flattening their differences" >}}.

---

## Discussion

_Which browser behavior do canvas applications most often lose without realizing it?_

{{< giscus >}}
