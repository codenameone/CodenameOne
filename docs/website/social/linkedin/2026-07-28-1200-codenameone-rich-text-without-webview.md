---
title: "A lightweight rich text component without a web view"
slug: 2026-07-28-1200-codenameone-rich-text-without-webview
platform: linkedin
account: codenameone
source_slug: rich-text-without-webview
publish_at: '2026-07-28T12:00:00'
timezone: Asia/Jerusalem
image: /blog/rich-text-without-webview.jpg
---

There is a large gap between a `SpanLabel` and a browser.

`RichTextComponent` fills it with a lightweight Codename One component that renders:

• HTML, Markdown, AsciiDoc, and a practical RTF subset
• headings, lists, links, inline images, colors, and highlights
• application-built styled runs
• height-for-width measurement inside ordinary layouts

It shares the document model and run painter with `RichTextArea`, but carries no caret, keyboard session, undo stack, or formatting toolbar.

Links are application actions. Images come from an application resolver. Imported HTML is document data and does not execute scripts.

Use `BrowserComponent` for a real web page. Formatted application content no longer needs one.

{{canonical}}
