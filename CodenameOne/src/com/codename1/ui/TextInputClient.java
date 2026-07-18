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

/// A low level text input client, the counterpart to a platform text input source (soft keyboard, IME,
/// autocorrect and hardware keyboard). A component that wants to own text editing itself - rendering
/// text with its own `Graphics` code instead of a native text widget - implements this interface and
/// binds it through `com.codename1.impl.CodenameOneImplementation#startTextInput`. From then on the
/// platform routes committed text, IME composition (marked text), deletions and key commands directly
/// into the client, and the client answers back with the surrounding text and caret geometry the
/// platform needs to place candidate windows, the selection loupe and predictions.
///
/// This is the same pattern high end custom editors use (a hidden input surface driving a
/// custom rendered document). Codename One's pure `RichTextArea` / `CodeEditor` are the primary
/// consumers, but the API is deliberately general so any component can capture raw text input.
///
/// All offsets exchanged with the platform are UTF-16 character indices. All callbacks are delivered on
/// the EDT.
public interface TextInputClient {
    /// Key command: move / extend to the left.
    int KEY_LEFT = 1;
    /// Key command: move / extend to the right.
    int KEY_RIGHT = 2;
    /// Key command: move / extend up a line.
    int KEY_UP = 3;
    /// Key command: move / extend down a line.
    int KEY_DOWN = 4;
    /// Key command: move / extend to the start of the line.
    int KEY_HOME = 5;
    /// Key command: move / extend to the end of the line.
    int KEY_END = 6;
    /// Key command: page up.
    int KEY_PAGE_UP = 7;
    /// Key command: page down.
    int KEY_PAGE_DOWN = 8;
    /// Key command: delete backward (backspace) when not expressed as a surrounding text deletion.
    int KEY_BACKSPACE = 9;
    /// Key command: delete forward.
    int KEY_DELETE = 10;
    /// Key command: dismiss / cancel (escape).
    int KEY_ESCAPE = 11;
    /// Key command: copy the selection.
    int KEY_COPY = 12;
    /// Key command: cut the selection.
    int KEY_CUT = 13;
    /// Key command: paste the clipboard.
    int KEY_PASTE = 14;
    /// Key command: select all.
    int KEY_SELECT_ALL = 15;
    /// Key command: undo.
    int KEY_UNDO = 16;
    /// Key command: redo.
    int KEY_REDO = 17;
    /// Key command: tab. With `#MOD_SHIFT` this is a dedent (shift-tab); otherwise an indent. A plain
    /// editor with no indentation behavior may treat it as inserting a tab character.
    int KEY_TAB = 18;

    /// Modifier bit: shift held (used to extend a selection).
    int MOD_SHIFT = 1;
    /// Modifier bit: control or command held.
    int MOD_CTRL = 2;
    /// Modifier bit: alt / option held.
    int MOD_ALT = 4;

    /// Commits final text at the caret, replacing the active selection. This is the normal path for a
    /// typed character, an accepted autocorrect suggestion, dictation and paste.
    ///
    /// #### Parameters
    ///
    /// - `text`: the committed text
    void commitText(String text);

    /// Updates the in progress IME composition (marked text) at the caret. The composing text is not yet
    /// final and is typically rendered underlined; it is replaced on the next `#setComposingText` and
    /// finalized by `#finishComposing`.
    ///
    /// #### Parameters
    ///
    /// - `text`: the current marked text
    ///
    /// - `relativeCaret`: caret position relative to the marked text (platform convention)
    void setComposingText(String text, int relativeCaret);

    /// Finalizes any active IME composition, committing the current marked text.
    void finishComposing();

    /// Deletes text around the caret, the primary deletion path used by soft keyboards.
    ///
    /// #### Parameters
    ///
    /// - `before`: number of UTF-16 units to delete before the caret
    ///
    /// - `after`: number of UTF-16 units to delete after the caret
    void deleteSurroundingText(int before, int after);

    /// Delivers a non text key command (navigation, deletion, clipboard, undo). Text producing keys are
    /// delivered through `#commitText` instead.
    ///
    /// #### Parameters
    ///
    /// - `command`: one of the `KEY_*` constants
    ///
    /// - `modifiers`: a bit mask of the `MOD_*` constants
    void onKeyCommand(int command, int modifiers);

    /// Delivers the keyboard return key action (done / next / search / send) configured through
    /// `TextInputConfig`.
    ///
    /// #### Parameters
    ///
    /// - `action`: one of the `TextInputConfig` `ACTION_*` constants
    void onEditorAction(int action);

    /// Returns the current editing state (surrounding text, selection and composition) so the platform
    /// can seed autocorrect and prediction. May return a window around the caret for very large
    /// documents.
    TextInputState getEditingState();

    /// Returns the caret rectangle in absolute screen pixels `{x, y, width, height}`, used by the
    /// platform to anchor the IME candidate window and the selection loupe near the rendered caret.
    int[] getCaretRect();

    /// Returns the configuration describing the desired keyboard type and input behavior.
    TextInputConfig getConfig();

    /// Notifies the client that it gained the platform input focus (became first responder).
    void inputFocusGained();

    /// Notifies the client that it lost the platform input focus.
    void inputFocusLost();

    // ---- geometry + direct editing, used by richer platform input protocols (iOS UITextInput) to draw
    // the selection loupe / handles and to drive edits by range ----

    /// Returns the total number of UTF-16 characters in the document.
    int getTextLength();

    /// Returns the text in `[start, end)`.
    String getTextRange(int start, int end);

    /// Returns the caret rectangle for an arbitrary offset in absolute screen pixels `{x, y, w, h}`.
    int[] rectForOffset(int offset);

    /// Returns the document offset nearest an absolute screen point (pixels).
    int offsetAtPoint(int x, int y);

    /// Returns the selection rectangles covering `[start, end)` as a flat array of absolute-pixel
    /// rectangles `{x, y, w, h, x, y, w, h, ...}` (one entry per visual line the range spans).
    int[] selectionRects(int start, int end);

    /// Replaces the range `[start, end)` with `text` (a direct, range based edit from the platform).
    void replaceRange(int start, int end, String text);

    /// Sets the selection to `[start, end)` (a caret when equal).
    void setSelectionRange(int start, int end);
}
