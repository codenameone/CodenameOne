/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.editor;

/// The plain text backing store shared by the pure Codename One editors. The document keeps the full
/// text in a single buffer addressed by a linear UTF-16 offset space and derives a cached line index
/// (the start offset of every logical line) on demand for rendering and hit testing.
///
/// A single buffer keeps offset arithmetic trivial (offsets are direct buffer indices) which is exactly
/// what the caret, selection, undo stack and the platform text input source all operate on. Line
/// oriented feature layers (syntax highlighting, the gutter) sit on top of the line index.
public class EditorDocument {
    private final StringBuilder buf = new StringBuilder(); // NOPMD - intentional owned buffer
    private String cachedText;
    private int[] lineStarts = new int[]{0};
    private int lineCount = 1;
    private boolean linesDirty = true;

    // The whole text as a String, cached between edits. Reading substrings from a String avoids
    // StringBuilder.substring which the ParparVM (iOS) minimal runtime does not implement.
    private String text() {
        if (cachedText == null) {
            cachedText = buf.toString();
        }
        return cachedText;
    }

    /// Creates an empty document.
    public EditorDocument() {
    }

    /// Creates a document initialized with the supplied text.
    ///
    /// #### Parameters
    ///
    /// - `text`: the initial text, or null for an empty document
    public EditorDocument(String text) {
        if (text != null) {
            buf.append(normalizeText(text));
        }
        linesDirty = true;
        cachedText = null;
    }

    /// Normalizes platform line endings into the document's canonical LF representation.
    public static String normalizeText(String text) {
        // collapse CRLF / CR to LF so the linear offset space matches what the platform delivers
        if (text.indexOf('\r') < 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    continue;
                }
                sb.append('\n');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Returns the total number of UTF-16 characters in the document.
    public int length() {
        return buf.length();
    }

    /// Returns the full document text.
    public String getText() {
        return text();
    }

    /// Returns the character at the given offset.
    ///
    /// #### Parameters
    ///
    /// - `offset`: a valid offset in `[0, length())`
    public char charAt(int offset) {
        return buf.charAt(offset);
    }

    /// Returns the substring in `[start, end)`.
    public String substring(int start, int end) {
        return text().substring(start, end);
    }

    /// Replaces the entire document content.
    ///
    /// #### Parameters
    ///
    /// - `text`: the new text, or null to clear the document
    public void setText(String text) {
        buf.setLength(0);
        if (text != null) {
            buf.append(normalizeText(text));
        }
        linesDirty = true;
        cachedText = null;
    }

    /// Inserts text at the given offset.
    ///
    /// #### Parameters
    ///
    /// - `offset`: the insertion offset in `[0, length()]`
    ///
    /// - `text`: the text to insert
    public void insert(int offset, String text) {
        if (text == null || text.length() == 0) {
            return;
        }
        buf.insert(offset, normalizeText(text));
        linesDirty = true;
        cachedText = null;
    }

    /// Deletes the range `[start, end)`.
    ///
    /// #### Parameters
    ///
    /// - `start`: inclusive start offset
    ///
    /// - `end`: exclusive end offset
    public void delete(int start, int end) {
        if (start >= end) {
            return;
        }
        buf.delete(start, end);
        linesDirty = true;
        cachedText = null;
    }

    /// Clamps an arbitrary offset into the valid range `[0, length()]`.
    public int clamp(int offset) {
        if (offset < 0) {
            return 0;
        }
        if (offset > buf.length()) {
            return buf.length();
        }
        return offset;
    }

    private void ensureLines() {
        if (!linesDirty) {
            return;
        }
        int count = 1;
        int len = buf.length();
        for (int i = 0; i < len; i++) {
            if (buf.charAt(i) == '\n') {
                count++;
            }
        }
        int[] starts = new int[count];
        starts[0] = 0;
        int idx = 1;
        for (int i = 0; i < len; i++) {
            if (buf.charAt(i) == '\n') {
                starts[idx++] = i + 1;
            }
        }
        lineStarts = starts;
        lineCount = count;
        linesDirty = false;
    }

    /// Returns the number of logical lines (always at least one).
    public int getLineCount() {
        ensureLines();
        return lineCount;
    }

    /// Returns the start offset of the given line.
    ///
    /// #### Parameters
    ///
    /// - `line`: a zero based line index in `[0, getLineCount())`
    public int getLineStart(int line) {
        ensureLines();
        return lineStarts[line];
    }

    /// Returns the end offset (exclusive, not counting the trailing newline) of the given line.
    ///
    /// #### Parameters
    ///
    /// - `line`: a zero based line index in `[0, getLineCount())`
    public int getLineEnd(int line) {
        ensureLines();
        if (line + 1 < lineCount) {
            return lineStarts[line + 1] - 1;
        }
        return buf.length();
    }

    /// Returns the text of the given line without its trailing newline.
    ///
    /// #### Parameters
    ///
    /// - `line`: a zero based line index in `[0, getLineCount())`
    public String getLineText(int line) {
        return text().substring(getLineStart(line), getLineEnd(line));
    }

    /// Returns the zero based line index containing the given offset.
    ///
    /// #### Parameters
    ///
    /// - `offset`: an offset in `[0, length()]`
    public int lineOfOffset(int offset) {
        ensureLines();
        int lo = 0;
        int hi = lineCount - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (lineStarts[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo;
    }

    /// Returns the column (offset within its line) for the given absolute offset.
    ///
    /// #### Parameters
    ///
    /// - `offset`: an offset in `[0, length()]`
    public int columnOfOffset(int offset) {
        return offset - getLineStart(lineOfOffset(offset));
    }
}
