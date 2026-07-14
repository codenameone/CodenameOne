/// The pure Codename One text editing engine that backs
/// `com.codename1.ui.RichTextArea` and `com.codename1.ui.CodeEditor`.
///
/// Overview
///
/// This package renders and edits text entirely with the Codename One
/// graphics pipeline - it owns the document model, caret, selection, undo
/// stack, layout and painting, and binds directly to the platform text input
/// source (soft keyboard, autocorrect and IME composition) rather than
/// embedding a native web view. Because the engine draws every glyph itself it
/// behaves consistently across platforms and supports mixed left-to-right and
/// right-to-left text via the Unicode Bidirectional Algorithm.
///
/// Key types
///
/// - `EditorDocument` - the mutable character buffer with a line index that
/// caret, selection, undo and the platform input source all operate on.
///
/// - `EditorView` - the base `com.codename1.ui.Component` that measures,
/// paints, scrolls and hit-tests the document and implements the text input
/// client contract; `CodeView` and `RichView` extend it for the code and rich
/// text feature layers.
///
/// - `BidiUtil` - the Unicode Bidirectional Algorithm used to resolve
/// embedding levels and reorder mixed-direction runs for measurement and paint.
///
/// - `HtmlImporter` - parses the HTML fragment accepted by `RichTextArea` into
/// the styled block/run model the engine renders.
///
/// These classes are the implementation of the two public editor components;
/// applications interact with `RichTextArea` and `CodeEditor` rather than with
/// the types here.
package com.codename1.ui.editor;
