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

/// An immutable paragraph: styled text plus the paragraph's own layout. Distances are in points.
///
/// Every `with...` method returns a copy that keeps the id, so the copy replaces the original in
/// a document:
///
/// ```java
/// Paragraph p = new Paragraph("p1", RichText.of("Hello")).withAlign(Paragraph.CENTER);
/// ```
public final class Paragraph implements Block {
    /// Start-aligned text.
    public static final String LEFT = "left";

    /// Centered text.
    public static final String CENTER = "center";

    /// End-aligned text.
    public static final String RIGHT = "right";

    /// Text stretched to both margins.
    public static final String JUSTIFY = "justify";

    /// Not a list item.
    public static final String NO_LIST = "";

    /// A bulleted list item.
    public static final String BULLET = "bullet";

    /// A numbered list item.
    public static final String NUMBERED = "number";

    /// The block id.
    public final String id;

    /// The named paragraph style (`Normal`, `Heading1` ... `Heading6`, `Title`, `Quote`, `Code`
    /// or an application's own).
    public final String style;

    /// One of `LEFT`, `CENTER`, `RIGHT` or `JUSTIFY`.
    public final String align;

    /// One of `NO_LIST`, `BULLET` or `NUMBERED`.
    public final String list;

    /// The list nesting level, from zero.
    public final int listLevel;

    /// The number a numbered list restarts at, or zero to continue the previous list.
    public final int listStart;

    /// Space above the paragraph.
    public final float before;

    /// Space below the paragraph.
    public final float after;

    /// Left indent.
    public final float indent;

    /// Line spacing as a multiple of the single line height.
    public final float lineSpacing;

    /// Whether a page break precedes the paragraph.
    public final boolean pageBreak;

    /// Whether the paragraph stays on the page of the one that follows it.
    public final boolean keepNext;

    /// The paragraph's text.
    public final RichText text;

    /// A `Normal` paragraph with default layout.
    ///
    /// #### Parameters
    ///
    /// - `id`: the block id
    ///
    /// - `text`: the text
    public Paragraph(String id, RichText text) {
        this(id, "Normal", LEFT, NO_LIST, 0, 0, 0, 8, 0, 1.15f, false, false, text);
    }

    /// A paragraph with every attribute given.
    ///
    /// #### Parameters
    ///
    /// - `id`: the block id
    ///
    /// - `style`: the named style
    ///
    /// - `align`: the alignment
    ///
    /// - `list`: the list kind
    ///
    /// - `listLevel`: the list level, 0 to 8
    ///
    /// - `listStart`: the restart number, or zero
    ///
    /// - `before`: space above
    ///
    /// - `after`: space below
    ///
    /// - `indent`: left indent
    ///
    /// - `lineSpacing`: the line spacing multiple
    ///
    /// - `pageBreak`: page break before
    ///
    /// - `keepNext`: keep with next
    ///
    /// - `text`: the text
    public Paragraph(String id, String style, String align, String list, int listLevel,
            int listStart, float before, float after, float indent, float lineSpacing,
            boolean pageBreak, boolean keepNext, RichText text) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Paragraph id is required");
        }
        this.id = id;
        this.style = style == null || style.length() == 0 ? "Normal" : style;
        this.align = CENTER.equals(align) || RIGHT.equals(align) || JUSTIFY.equals(align)
                ? align : LEFT;
        this.list = BULLET.equals(list) || NUMBERED.equals(list) ? list : NO_LIST;
        this.listLevel = Math.max(0, Math.min(8, listLevel));
        this.listStart = Math.max(0, listStart);
        this.before = Math.max(0, before);
        this.after = Math.max(0, after);
        this.indent = indent;
        this.lineSpacing = lineSpacing > 0 ? lineSpacing : 1;
        this.pageBreak = pageBreak;
        this.keepNext = keepNext;
        this.text = text == null ? RichText.EMPTY : text;
    }

    /// The block id.
    ///
    /// #### Returns
    ///
    /// the id
    @Override
    public String id() {
        return id;
    }

    /// The text without formatting.
    ///
    /// #### Returns
    ///
    /// the characters
    public String plainText() {
        return text.plainText();
    }

    /// Whether the paragraph is a heading, that is its style is `Heading1` to `Heading6` or
    /// `Title`.
    ///
    /// #### Returns
    ///
    /// true for a heading
    public boolean isHeading() {
        return headingLevel() > 0;
    }

    /// The heading level: 1 to 6 for `Heading1` to `Heading6`, 1 for `Title`, 0 otherwise.
    ///
    /// #### Returns
    ///
    /// the level
    public int headingLevel() {
        if ("Title".equals(style)) {
            return 1;
        }
        if (style.length() == 8 && style.startsWith("Heading")) {
            char c = style.charAt(7);
            if (c >= '1' && c <= '6') {
                return c - '0';
            }
        }
        return 0;
    }

    /// A copy with different text.
    ///
    /// #### Parameters
    ///
    /// - `value`: the text
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withText(RichText value) {
        return new Paragraph(id, style, align, list, listLevel, listStart, before, after, indent,
                lineSpacing, pageBreak, keepNext, value);
    }

    /// A copy with a different named style.
    ///
    /// #### Parameters
    ///
    /// - `value`: the style
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withStyle(String value) {
        return new Paragraph(id, value, align, list, listLevel, listStart, before, after, indent,
                lineSpacing, pageBreak, keepNext, text);
    }

    /// A copy with a different alignment.
    ///
    /// #### Parameters
    ///
    /// - `value`: `LEFT`, `CENTER`, `RIGHT` or `JUSTIFY`
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withAlign(String value) {
        return new Paragraph(id, style, value, list, listLevel, listStart, before, after, indent,
                lineSpacing, pageBreak, keepNext, text);
    }

    /// A copy with different list membership.
    ///
    /// #### Parameters
    ///
    /// - `kind`: `NO_LIST`, `BULLET` or `NUMBERED`
    ///
    /// - `level`: the nesting level
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withList(String kind, int level) {
        return new Paragraph(id, style, align, kind, level, listStart, before, after, indent,
                lineSpacing, pageBreak, keepNext, text);
    }

    /// A copy whose numbered list restarts.
    ///
    /// #### Parameters
    ///
    /// - `value`: the first number, or zero to continue
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withListStart(int value) {
        return new Paragraph(id, style, align, list, listLevel, value, before, after, indent,
                lineSpacing, pageBreak, keepNext, text);
    }

    /// A copy with different spacing.
    ///
    /// #### Parameters
    ///
    /// - `spaceBefore`: space above
    ///
    /// - `spaceAfter`: space below
    ///
    /// - `lines`: the line spacing multiple
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withSpacing(float spaceBefore, float spaceAfter, float lines) {
        return new Paragraph(id, style, align, list, listLevel, listStart, spaceBefore, spaceAfter,
                indent, lines, pageBreak, keepNext, text);
    }

    /// A copy with a different left indent.
    ///
    /// #### Parameters
    ///
    /// - `value`: the indent
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withIndent(float value) {
        return new Paragraph(id, style, align, list, listLevel, listStart, before, after, value,
                lineSpacing, pageBreak, keepNext, text);
    }

    /// A copy with the page break switched.
    ///
    /// #### Parameters
    ///
    /// - `value`: break before or not
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withPageBreak(boolean value) {
        return new Paragraph(id, style, align, list, listLevel, listStart, before, after, indent,
                lineSpacing, value, keepNext, text);
    }

    /// A copy with keep-with-next switched.
    ///
    /// #### Parameters
    ///
    /// - `value`: keep or not
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withKeepNext(boolean value) {
        return new Paragraph(id, style, align, list, listLevel, listStart, before, after, indent,
                lineSpacing, pageBreak, value, text);
    }

    /// A copy with another id, used when a paragraph is duplicated.
    ///
    /// #### Parameters
    ///
    /// - `value`: the new id
    ///
    /// #### Returns
    ///
    /// the copy
    public Paragraph withId(String value) {
        return new Paragraph(value, style, align, list, listLevel, listStart, before, after, indent,
                lineSpacing, pageBreak, keepNext, text);
    }

    /// Two paragraphs are equal when every attribute, the id included, is.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Paragraph)) {
            return false;
        }
        Paragraph p = (Paragraph) o;
        return id.equals(p.id) && style.equals(p.style) && align.equals(p.align)
                && list.equals(p.list) && listLevel == p.listLevel && listStart == p.listStart
                && Float.floatToIntBits(before) == Float.floatToIntBits(p.before)
                && Float.floatToIntBits(after) == Float.floatToIntBits(p.after)
                && Float.floatToIntBits(indent) == Float.floatToIntBits(p.indent)
                && Float.floatToIntBits(lineSpacing) == Float.floatToIntBits(p.lineSpacing)
                && pageBreak == p.pageBreak && keepNext == p.keepNext && text.equals(p.text);
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return (id.hashCode() * 31 + style.hashCode()) * 31 + text.hashCode();
    }
}
