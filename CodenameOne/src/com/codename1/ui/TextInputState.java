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

package com.codename1.ui;

/// An immutable snapshot of the editing state shared between a `TextInputClient` and the platform text
/// input source. It carries the surrounding text the platform needs for autocorrect / prediction, the
/// current selection, and the in progress IME composition (marked text) range if any.
///
/// All offsets are UTF-16 character indices into `text`, matching Java `String` indices as well as the
/// `NSString` / Android `Editable` indices used by the native input machinery. A collapsed selection
/// (`selectionStart == selectionEnd`) represents a plain caret. A composing range of `-1, -1` means no
/// IME composition is active.
public final class TextInputState {
    private final String text;
    private final int selectionStart;
    private final int selectionEnd;
    private final int composingStart;
    private final int composingEnd;

    /// Creates a state snapshot without an active composition.
    ///
    /// #### Parameters
    ///
    /// - `text`: the surrounding text (may be a window around the caret for very large documents)
    ///
    /// - `selectionStart`: UTF-16 offset of the selection anchor
    ///
    /// - `selectionEnd`: UTF-16 offset of the selection focus
    public TextInputState(String text, int selectionStart, int selectionEnd) {
        this(text, selectionStart, selectionEnd, -1, -1);
    }

    /// Creates a state snapshot including an IME composition range.
    ///
    /// #### Parameters
    ///
    /// - `text`: the surrounding text
    ///
    /// - `selectionStart`: UTF-16 offset of the selection anchor
    ///
    /// - `selectionEnd`: UTF-16 offset of the selection focus
    ///
    /// - `composingStart`: UTF-16 offset where the marked text begins, or -1 when none
    ///
    /// - `composingEnd`: UTF-16 offset where the marked text ends, or -1 when none
    public TextInputState(String text, int selectionStart, int selectionEnd, int composingStart, int composingEnd) {
        this.text = text == null ? "" : text;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.composingStart = composingStart;
        this.composingEnd = composingEnd;
    }

    /// Returns the surrounding text.
    public String getText() {
        return text;
    }

    /// Returns the UTF-16 offset of the selection anchor.
    public int getSelectionStart() {
        return selectionStart;
    }

    /// Returns the UTF-16 offset of the selection focus.
    public int getSelectionEnd() {
        return selectionEnd;
    }

    /// Returns the UTF-16 offset where the IME marked text begins, or -1 when no composition is active.
    public int getComposingStart() {
        return composingStart;
    }

    /// Returns the UTF-16 offset where the IME marked text ends, or -1 when no composition is active.
    public int getComposingEnd() {
        return composingEnd;
    }

    /// True when the selection is collapsed to a caret.
    public boolean isCaret() {
        return selectionStart == selectionEnd;
    }

    /// True when an IME composition (marked text) is currently active.
    public boolean isComposing() {
        return composingStart >= 0 && composingEnd >= 0;
    }
}
