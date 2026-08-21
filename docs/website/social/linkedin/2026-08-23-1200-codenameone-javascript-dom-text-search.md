---
title: "Canvas pixels can behave like browser text"
slug: 2026-08-23-1200-codenameone-javascript-dom-text-search
platform: linkedin
account: codenameone
source_slug: javascript-dom-text-search
publish_at: '2026-08-23T12:00:00'
timezone: Asia/Jerusalem
image: /blog/javascript-dom-text-search.jpg
---

Browser search cannot find text painted into a canvas.

The Codename One JavaScript port now keeps its canvas renderer and promotes eligible visible text into a targeted DOM layer. Codename One still measures every run and decides its exact position. The browser receives text that is already broken and placed, so it cannot reflow the UI.

That narrow split restores the browser features users expect:

• Find in page and keyboard selection
• Stable incremental ARIA nodes
• Autofill and password-manager metadata
• Native text rasterization on high-DPI displays

Transformed, clipped, offscreen, and bitmap-font text stays on the canvas. Drag selection still needs pointer routing because the canvas owns application hit testing.

One renderer keeps application layout consistent. Two small DOM projections let the page behave like a page.

{{canonical}}
