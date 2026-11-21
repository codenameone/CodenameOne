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

/// One thing drawn on a laid out page: a piece of text, a filled or stroked rectangle, a line,
/// a picture or the box of an embedded component. Coordinates are in points from the page's top
/// left corner.
public final class LayoutItem {
    /// A piece of text: `text` drawn in `style` on `baseline`.
    public static final int TEXT = 0;

    /// A filled rectangle in `color`.
    public static final int FILL = 1;

    /// A straight line from (`x`, `y`) to (`x + width`, `y + height`) in `color`.
    public static final int LINE = 2;

    /// A picture filling the item's box.
    public static final int PICTURE = 3;

    /// The box an embedded component occupies.
    public static final int COMPONENT = 4;

    /// What this item is: `TEXT`, `FILL`, `LINE`, `PICTURE` or `COMPONENT`.
    public final int kind;

    /// The left edge.
    public final float x;

    /// The top edge; for text, the top of its line.
    public final float y;

    /// The width.
    public final float width;

    /// The height; for text, the height of its line.
    public final float height;

    /// For text, the y of the baseline it is drawn on.
    public final float baseline;

    /// For text, what is drawn; may be empty.
    public final String text;

    /// For text, its style; otherwise null.
    public final TextStyle style;

    /// For text, the id of the paragraph it belongs to, or null for generated text such as a
    /// list marker.
    public final String paragraphId;

    /// For text, the offset of its first character in the paragraph's text.
    public final int start;

    /// For a picture, the picture.
    public final Picture picture;

    /// For a component, the component.
    public final EmbeddedComponent component;

    /// For a fill or a line, its RGB color.
    public final int color;

    final String field;

    LayoutItem(int kind, float x, float y, float width, float height, float baseline, String text,
            TextStyle style, String paragraphId, int start, Picture picture,
            EmbeddedComponent component, int color, String field) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.baseline = baseline;
        this.text = text;
        this.style = style;
        this.paragraphId = paragraphId;
        this.start = start;
        this.picture = picture;
        this.component = component;
        this.color = color;
        this.field = field;
    }

    static LayoutItem fill(float x, float y, float w, float h, int color) {
        return new LayoutItem(FILL, x, y, w, h, 0, null, null, null, 0, null, null, color, null);
    }

    static LayoutItem line(float x, float y, float w, float h, int color) {
        return new LayoutItem(LINE, x, y, w, h, 0, null, null, null, 0, null, null, color, null);
    }

    LayoutItem moved(float dx, float dy) {
        return new LayoutItem(kind, x + dx, y + dy, width, height, baseline + dy, text, style,
                paragraphId, start, picture, component, color, field);
    }

    LayoutItem withText(String value) {
        return new LayoutItem(kind, x, y, width, height, baseline, value, style, paragraphId,
                start, picture, component, color, field);
    }

    LayoutItem withBox(float nx, float ny, float w, float h) {
        return new LayoutItem(kind, nx, ny, w, h, baseline, text, style, paragraphId, start,
                picture, component, color, field);
    }
}
