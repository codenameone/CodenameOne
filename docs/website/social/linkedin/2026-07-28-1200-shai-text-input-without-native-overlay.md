---
title: "Pure Codename One text editing without a native overlay"
slug: 2026-07-28-1200-shai-text-input-without-native-overlay
platform: linkedin
account: shai
source_slug: text-input-without-native-overlay
publish_at: '2026-07-28T12:00:00'
timezone: Asia/Jerusalem
review_by: '2026-07-24'
status: draft
image: /blog/text-input-without-native-overlay.jpg
---

Codename One now supports pure lightweight text editing without placing a native field over the component.

The existing overlay remains the default for `TextField` and `TextArea`. It is mature and appropriate for ordinary forms. The new route is for text surfaces that need syntax colors, inline images, custom masks, or portable selection and painting.

We now have a second route. The OS sends semantic operations such as `commitText`, `setComposingText`, and `deleteSurroundingText`. A pure Codename One document owns the caret, selection, bidirectional layout, undo, and painting.

`EditField`, `RichTextArea`, and `CodeEditor` use the new contract. Each port handles the keyboard session, while Codename One owns the document and the pixels on screen.

{{canonical}}
