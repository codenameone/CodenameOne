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

package com.codename1.ui.editor;

import java.util.ArrayList;
import java.util.List;

/// The per paragraph block attribute model for the pure rich text editor. Each logical line (paragraph,
/// delimited by newlines) has a `BlockAttr` describing its block type (paragraph / heading / preformatted
/// / blockquote), text alignment, list membership and indentation. The model is kept in lockstep with the
/// paragraph structure through `#applyEdit`.
public final class RichBlocks {
    /// Block type: a normal paragraph.
    public static final int PARAGRAPH = 0;
    /// Block type: heading level 1 through 6 are `H1`..`H1 + 5`.
    public static final int H1 = 1;
    /// Block type: preformatted text.
    public static final int PRE = 7;
    /// Block type: a block quote.
    public static final int BLOCKQUOTE = 8;

    /// Left alignment.
    public static final int ALIGN_LEFT = 0;
    /// Center alignment.
    public static final int ALIGN_CENTER = 1;
    /// Right alignment.
    public static final int ALIGN_RIGHT = 2;

    /// Not a list item.
    public static final int LIST_NONE = 0;
    /// An ordered (numbered) list item.
    public static final int LIST_ORDERED = 1;
    /// An unordered (bulleted) list item.
    public static final int LIST_UNORDERED = 2;

    /// A single paragraph's block attributes.
    public static final class BlockAttr {
        /// The block type (`PARAGRAPH`, `H1`..`H1+5`, `PRE`, `BLOCKQUOTE`).
        public int type = PARAGRAPH;
        /// The text alignment (`ALIGN_*`).
        public int align = ALIGN_LEFT;
        /// The list membership (`LIST_*`).
        public int listType = LIST_NONE;
        /// The indentation depth (0 or more).
        public int indent;

        BlockAttr copy() {
            BlockAttr b = new BlockAttr();
            b.type = type;
            b.align = align;
            b.listType = listType;
            b.indent = indent;
            return b;
        }
    }

    private final List<BlockAttr> blocks = new ArrayList<BlockAttr>();

    /// Creates a model with the given paragraph count, all default paragraphs.
    public RichBlocks(int paragraphCount) {
        reset(paragraphCount);
    }

    /// Resets the model to the given paragraph count, all default paragraphs.
    public void reset(int paragraphCount) {
        blocks.clear();
        for (int i = 0; i < Math.max(1, paragraphCount); i++) {
            blocks.add(new BlockAttr());
        }
    }

    /// Returns the number of paragraphs.
    public int count() {
        return blocks.size();
    }

    /// Returns the block attributes for the given paragraph (clamped to the valid range).
    public BlockAttr get(int paragraph) {
        if (paragraph < 0) {
            paragraph = 0;
        }
        if (paragraph >= blocks.size()) {
            paragraph = blocks.size() - 1;
        }
        return blocks.get(paragraph);
    }

    /// Keeps the model in sync with a text edit that changed the paragraph structure.
    ///
    /// #### Parameters
    ///
    /// - `startParagraph`: the paragraph containing the edit offset
    ///
    /// - `removedNewlines`: number of newlines in the removed text (paragraphs merged away)
    ///
    /// - `addedNewlines`: number of newlines in the inserted text (paragraphs created)
    public void applyEdit(int startParagraph, int removedNewlines, int addedNewlines) {
        if (startParagraph < 0) {
            startParagraph = 0;
        }
        if (startParagraph >= blocks.size()) {
            startParagraph = blocks.size() - 1;
        }
        for (int i = 0; i < removedNewlines; i++) {
            int idx = startParagraph + 1;
            if (idx < blocks.size()) {
                blocks.remove(idx);
            }
        }
        BlockAttr base = blocks.get(startParagraph);
        for (int i = 0; i < addedNewlines; i++) {
            blocks.add(startParagraph + 1 + i, base.copy());
        }
        if (blocks.isEmpty()) {
            blocks.add(new BlockAttr());
        }
    }
}
