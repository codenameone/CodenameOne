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
package com.codename1.wordprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A footnote or endnote, placed in the text where its reference mark goes. The note's own
/// content is a list of blocks, like a table cell.
public final class Note implements Inline {
    /// A note at the bottom of the page its reference is on.
    public static final String FOOTNOTE = "footnote";

    /// A note collected at the end of the document.
    public static final String ENDNOTE = "endnote";

    /// The note's id, unique within the document.
    public final String id;

    /// `FOOTNOTE` or `ENDNOTE`.
    public final String kind;

    /// A custom reference mark such as `*`, or empty for automatic numbering.
    public final String mark;

    /// The note's content.
    public final List<Block> blocks;

    /// Creates a note.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `kind`: `FOOTNOTE` or `ENDNOTE`; anything else means `FOOTNOTE`
    ///
    /// - `mark`: a custom mark, or null for automatic numbering
    ///
    /// - `blocks`: the content; copied
    public Note(String id, String kind, String mark, List<Block> blocks) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Note id is required");
        }
        this.id = id;
        this.kind = ENDNOTE.equals(kind) ? ENDNOTE : FOOTNOTE;
        this.mark = mark == null ? "" : mark;
        List<Block> b = new ArrayList<Block>();
        if (blocks != null) {
            for (Block x : blocks) {
                if (x != null) {
                    b.add(x);
                }
            }
        }
        this.blocks = Collections.unmodifiableList(b);
    }

    /// A copy with different content.
    ///
    /// #### Parameters
    ///
    /// - `value`: the blocks
    ///
    /// #### Returns
    ///
    /// the note
    public Note withBlocks(List<Block> value) {
        return new Note(id, kind, mark, value);
    }

    /// The note's text without formatting.
    ///
    /// #### Returns
    ///
    /// the text
    public String plainText() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < blocks.size(); i++) {
            if (i > 0) {
                b.append('\n');
            }
            Document.appendPlainText(b, blocks.get(i));
        }
        return b.toString();
    }

    @Override
    public String displayText() {
        return mark;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Note)) {
            return false;
        }
        Note n = (Note) o;
        return id.equals(n.id) && kind.equals(n.kind) && mark.equals(n.mark) && blocks.equals(n.blocks);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + blocks.hashCode();
    }
}
