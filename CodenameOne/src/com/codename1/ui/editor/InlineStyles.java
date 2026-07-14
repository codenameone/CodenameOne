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

import java.util.ArrayList;
import java.util.List;

/// The per character inline style model for the pure rich text editor. It keeps one `TextStyle`
/// reference for every character in the document, kept in lockstep with the text through `#applyEdit`.
/// Runs of identical style are coalesced only at render time, which keeps edits simple and correct at
/// the cost of one reference per character (rich documents are small enough for this to be a non issue).
public final class InlineStyles {
    /// A transform applied to a character's style, used to toggle bold, set a color, etc.
    public interface StyleTransform {
        /// Returns the new style for a character that currently has `current`.
        TextStyle apply(TextStyle current);
    }

    private final List<TextStyle> perChar = new ArrayList<TextStyle>();

    /// Creates a model of the given length, all default style.
    public InlineStyles(int length) {
        reset(length);
    }

    /// Resets the model to the given length, all default style.
    public void reset(int length) {
        perChar.clear();
        for (int i = 0; i < length; i++) {
            perChar.add(TextStyle.DEFAULT);
        }
    }

    /// Returns the number of characters covered.
    public int length() {
        return perChar.size();
    }

    /// Returns the style of the character at `index`, or the default style out of range.
    public TextStyle styleAt(int index) {
        if (index < 0 || index >= perChar.size()) {
            return TextStyle.DEFAULT;
        }
        return perChar.get(index);
    }

    /// Keeps the model in sync with a text replacement: removes `removedLen` styles at `start` and
    /// inserts `insertedLen` copies of `style`.
    ///
    /// #### Parameters
    ///
    /// - `start`: the offset of the edit
    ///
    /// - `removedLen`: number of characters removed
    ///
    /// - `insertedLen`: number of characters inserted
    ///
    /// - `style`: the style applied to the inserted characters
    public void applyEdit(int start, int removedLen, int insertedLen, TextStyle style) {
        start = clamp(start);
        int end = clamp(start + removedLen);
        if (end > start) {
            perChar.subList(start, end).clear();
        }
        if (insertedLen > 0) {
            List<TextStyle> ins = new ArrayList<TextStyle>(insertedLen);
            TextStyle s = style == null ? TextStyle.DEFAULT : style;
            for (int i = 0; i < insertedLen; i++) {
                ins.add(s);
            }
            perChar.addAll(Math.min(start, perChar.size()), ins);
        }
    }

    /// Sets the style of a single character.
    public void setAt(int index, TextStyle style) {
        if (index >= 0 && index < perChar.size()) {
            perChar.set(index, style == null ? TextStyle.DEFAULT : style);
        }
    }

    /// Applies a transform to every character's style in `[start, end)`.
    public void transformRange(int start, int end, StyleTransform t) {
        start = clamp(start);
        end = clamp(end);
        for (int i = start; i < end; i++) {
            perChar.set(i, t.apply(perChar.get(i)));
        }
    }

    /// True when every character in `[start, end)` satisfies the predicate. An empty range checks the
    /// character just before the caret (the style typing would continue with).
    public boolean allInRange(int start, int end, StylePredicate p) {
        if (start >= end) {
            int probe = start - 1;
            return p.test(styleAt(probe < 0 ? 0 : probe));
        }
        start = clamp(start);
        end = clamp(end);
        for (int i = start; i < end; i++) {
            if (!p.test(perChar.get(i))) {
                return false;
            }
        }
        return true;
    }

    /// A predicate over a style, used by `#allInRange`.
    public interface StylePredicate {
        /// Returns true when `style` satisfies the predicate.
        boolean test(TextStyle style);
    }

    private int clamp(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > perChar.size()) {
            return perChar.size();
        }
        return v;
    }
}
