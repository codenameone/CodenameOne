# Evidence map

Source: `docs/website/content/blog/rich-text-and-code-editing.md`
Canonical: https://www.codenameone.com/blog/rich-text-and-code-editing/

## Thesis

One editor abstraction with rich-text and syntax-aware code backends

## Supported beats

- **One abstraction, two backends:** Both editors extend AbstractEditorComponent. That base class does not draw anything itself. It speaks a small semantic channel of commands ("apply bold", "set language to java"), queries ("give me the current HTML") and events ("the text changed") to whatever backend is attached.
- **CodeEditor:** CodeEditor highlights eight languages: java, kotlin, js, python, css, xml, json, and c. It draws a line-number gutter, ships light and dark themes, and auto-closes brackets and quotes as you type.
- **RichTextArea:** RichTextArea is the WYSIWYG side. It edits formatted text and gives you HTML in and out. You get bold, italic and underline, ordered and unordered lists, links, foreground and highlight colors, and headings.
- **You only pay for it if you use it:** CodeEditor can be backed by a heavier engine, and CodeMirror assets are not free weight to carry around. So the Android and iOS builders scan your app for use of com.codename1.ui.CodeEditor.
- **The tradeoff:** The default engine is web-view-backed. That is what makes it portable, and it is also the limit: it needs a web view to exist on the platform. iOS, Android, the desktop and the web all have one, so all four are covered.

## Referenced evidence

- https://example.com\

## Independent problem evidence

- MDN contenteditable: https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Global_attributes/contenteditable — The web platform exposes editable content, selection, and input behavior rather than treating formatted editing as plain string entry.
- CodeMirror System Guide: https://codemirror.net/docs/guide/ — CodeMirror documents an extensible editor architecture for language support, editing behavior, and asynchronous integrations.

## Product proof

- `docs/website/static/blog/rich-text-and-code-editing/components-codeeditor.png`
- `docs/website/static/blog/rich-text-and-code-editing/components-codeeditor-dark.png`
- `docs/website/static/blog/rich-text-and-code-editing/components-richtextarea.png`
