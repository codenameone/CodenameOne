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
package com.codename1.presentation;

import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.PageSpec;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The text inside a shape: word processing blocks, laid out in the shape's box less its
/// insets.
///
/// The text is the same model `com.codename1.wordprocessing` uses for documents -- paragraphs
/// of styled runs, lists, tables and pictures -- and it is laid out, painted and edited by the
/// same code, so a slide's text looks and behaves exactly like a document's.
public final class TextFrame {
    /// Text starts at the top of the box.
    public static final String TOP = "top";

    /// Text is centred vertically.
    public static final String MIDDLE = "middle";

    /// Text sits at the bottom of the box.
    public static final String BOTTOM = "bottom";

    /// Text keeps its size and may overflow the box.
    public static final String FIT_NONE = "none";

    /// Text shrinks until it fits the box.
    public static final String FIT_SHRINK = "shrink";

    /// The box grows to fit the text.
    public static final String FIT_RESIZE = "resize";

    /// The blocks, never null.
    public final List<Block> blocks;

    /// The left inset in points.
    public final float left;

    /// The top inset in points.
    public final float top;

    /// The right inset in points.
    public final float right;

    /// The bottom inset in points.
    public final float bottom;

    /// `TOP`, `MIDDLE` or `BOTTOM`.
    public final String anchor;

    /// `FIT_NONE`, `FIT_SHRINK` or `FIT_RESIZE`.
    public final String autofit;

    /// False when lines do not wrap at the box's width.
    public final boolean wrap;

    /// Text frame with the default insets (7.2pt left and right, 3.6pt top and bottom, as
    /// PowerPoint and Keynote use), anchored at the top.
    ///
    /// #### Parameters
    ///
    /// - `blocks`: the text
    public TextFrame(List<Block> blocks) {
        this(blocks, 7.2f, 3.6f, 7.2f, 3.6f, TOP, FIT_NONE, true);
    }

    /// A text frame.
    ///
    /// #### Parameters
    ///
    /// - `blocks`: the text
    ///
    /// - `left`: inset in points
    ///
    /// - `top`: inset in points
    ///
    /// - `right`: inset in points
    ///
    /// - `bottom`: inset in points
    ///
    /// - `anchor`: `TOP`, `MIDDLE` or `BOTTOM`
    ///
    /// - `autofit`: `FIT_NONE`, `FIT_SHRINK` or `FIT_RESIZE`
    ///
    /// - `wrap`: whether lines wrap
    public TextFrame(List<Block> blocks, float left, float top, float right, float bottom,
            String anchor, String autofit, boolean wrap) {
        List<Block> b = new ArrayList<Block>();
        if (blocks != null) {
            for (Block x : blocks) {
                if (x != null) {
                    b.add(x);
                }
            }
        }
        this.blocks = Collections.unmodifiableList(b);
        this.left = Math.max(0, left);
        this.top = Math.max(0, top);
        this.right = Math.max(0, right);
        this.bottom = Math.max(0, bottom);
        this.anchor = MIDDLE.equals(anchor) || BOTTOM.equals(anchor) ? anchor : TOP;
        this.autofit = FIT_SHRINK.equals(autofit) || FIT_RESIZE.equals(autofit) ? autofit
                : FIT_NONE;
        this.wrap = wrap;
    }

    /// A frame of plain paragraphs, one per line of `text`, in one style.
    ///
    /// #### Parameters
    ///
    /// - `idPrefix`: prefix for the paragraph ids, made unique by a counter
    ///
    /// - `text`: the text; `\n` starts a paragraph
    ///
    /// - `style`: the style of every run
    ///
    /// - `align`: paragraph alignment (`left`, `center`, `right`, `justify`), or null
    ///
    /// #### Returns
    ///
    /// the frame
    public static TextFrame of(String idPrefix, String text, TextStyle style, String align) {
        List<Block> b = new ArrayList<Block>();
        String s = text == null ? "" : text;
        int n = 0;
        int start = 0;
        while (true) {
            int end = s.indexOf('\n', start);
            String line = end < 0 ? s.substring(start) : s.substring(start, end);
            Paragraph p = new Paragraph(idPrefix + (++n), RichText.of(line, style));
            b.add(align == null ? p : p.withAlign(align));
            if (end < 0) {
                break;
            }
            start = end + 1;
        }
        return new TextFrame(b);
    }

    /// The frame's text as a word processing document laid out on a page the size of a box,
    /// with the insets as margins. `DocumentLayout` lays this out, and `DocumentView` edits
    /// it in place.
    ///
    /// #### Parameters
    ///
    /// - `width`: the box width in points
    ///
    /// - `height`: the box height in points
    ///
    /// #### Returns
    ///
    /// the document
    public Document toDocument(float width, float height) {
        float w = wrap ? Math.max(left + right + 1, width) : 100000;
        // one page that never breaks: a text box overflows its shape rather than paging
        float h = Math.max(top + bottom + 1, Math.max(height, 100000));
        PageSpec page = new PageSpec(w, h, top, right, bottom, left);
        return new Document(blocks.isEmpty() ? emptyText() : blocks, page, null, 1);
    }

    private static List<Block> emptyText() {
        List<Block> b = new ArrayList<Block>();
        b.add(new Paragraph("p0", RichText.of("")));
        return b;
    }

    /// The text without formatting, paragraphs separated by `\n`.
    ///
    /// #### Returns
    ///
    /// the text
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            if (b instanceof Paragraph) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(((Paragraph) b).plainText());
            }
        }
        return sb.toString();
    }

    /// This frame with other text.
    ///
    /// #### Parameters
    ///
    /// - `value`: the blocks
    ///
    /// #### Returns
    ///
    /// the frame
    public TextFrame withBlocks(List<Block> value) {
        return new TextFrame(value, left, top, right, bottom, anchor, autofit, wrap);
    }

    /// This frame with another vertical anchor.
    ///
    /// #### Parameters
    ///
    /// - `value`: `TOP`, `MIDDLE` or `BOTTOM`
    ///
    /// #### Returns
    ///
    /// the frame
    public TextFrame withAnchor(String value) {
        return new TextFrame(blocks, left, top, right, bottom, value, autofit, wrap);
    }

    /// This frame with other insets.
    ///
    /// #### Parameters
    ///
    /// - `l`: left
    ///
    /// - `t`: top
    ///
    /// - `r`: right
    ///
    /// - `b`: bottom
    ///
    /// #### Returns
    ///
    /// the frame
    public TextFrame withInsets(float l, float t, float r, float b) {
        return new TextFrame(blocks, l, t, r, b, anchor, autofit, wrap);
    }

    /// This frame with another autofit.
    ///
    /// #### Parameters
    ///
    /// - `value`: `FIT_NONE`, `FIT_SHRINK` or `FIT_RESIZE`
    ///
    /// #### Returns
    ///
    /// the frame
    public TextFrame withAutofit(String value) {
        return new TextFrame(blocks, left, top, right, bottom, anchor, value, wrap);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextFrame)) {
            return false;
        }
        TextFrame f = (TextFrame) o;
        return blocks.equals(f.blocks) && Float.floatToIntBits(left) == Float.floatToIntBits(f.left) && Float.floatToIntBits(top) == Float.floatToIntBits(f.top) && Float.floatToIntBits(right) == Float.floatToIntBits(f.right)
                && Float.floatToIntBits(bottom) == Float.floatToIntBits(f.bottom) && anchor.equals(f.anchor) && autofit.equals(f.autofit)
                && wrap == f.wrap;
    }

    @Override
    public int hashCode() {
        return blocks.hashCode() * 31 + anchor.hashCode() + autofit.hashCode() * 7;
    }
}
