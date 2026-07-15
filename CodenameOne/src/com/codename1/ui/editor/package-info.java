/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

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
