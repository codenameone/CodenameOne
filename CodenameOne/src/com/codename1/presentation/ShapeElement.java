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

/// A drawn shape -- rectangle, ellipse, arrow, star, line, connector -- with an optional fill,
/// outline and text. A text box is a rectangle with no fill and no outline.
///
/// `geometry` names one of the preset shapes `ShapeGeometry` draws; the names are OOXML's
/// preset geometry names, which ODF and Keynote shapes are mapped to when read. A name the
/// renderer does not know is drawn as a rectangle and kept for writing.
public final class ShapeElement extends SlideElement {
    /// The title placeholder of a layout.
    public static final String TITLE = "title";

    /// The subtitle placeholder.
    public static final String SUBTITLE = "subTitle";

    /// The body placeholder.
    public static final String BODY = "body";

    /// The preset geometry name, never null.
    public final String geometry;

    /// Adjustment values of the geometry (corner radius, arrow head size...), as fractions of
    /// the shorter side; empty for the defaults.
    private final float[] adjust;

    /// The fill.
    public final Fill fill;

    /// The outline.
    public final LineStyle line;

    /// The text, or null for a shape without text.
    public final TextFrame text;

    /// The placeholder role from the slide's layout (`TITLE`, `BODY`...), or null.
    public final String placeholder;

    /// A shape.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `geometry`: the preset name, for example `rect`, `ellipse`, `rightArrow`
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `fill`: the fill, or null for none
    ///
    /// - `line`: the outline, or null for none
    ///
    /// - `text`: the text, or null
    public ShapeElement(String id, String geometry, float x, float y, float width, float height,
            Fill fill, LineStyle line, TextFrame text) {
        this(id, null, geometry, x, y, width, height, 0, false, false, fill, line, text, null,
                null);
    }

    /// A shape with every property.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `name`: a name, or null
    ///
    /// - `geometry`: the preset name
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    ///
    /// - `flipH`: mirrored horizontally
    ///
    /// - `flipV`: mirrored vertically
    ///
    /// - `fill`: the fill, or null
    ///
    /// - `line`: the outline, or null
    ///
    /// - `text`: the text, or null
    ///
    /// - `placeholder`: the placeholder role, or null
    ///
    /// - `adjust`: adjustment values, or null
    public ShapeElement(String id, String name, String geometry, float x, float y, float width,
            float height, float rotation, boolean flipH, boolean flipV, Fill fill,
            LineStyle line, TextFrame text, String placeholder, float[] adjust) {
        super(id, name, x, y, width, height, rotation, flipH, flipV);
        this.geometry = geometry == null || geometry.length() == 0 ? "rect" : geometry;
        this.fill = fill == null ? Fill.NO_FILL : fill;
        this.line = line == null ? LineStyle.NONE : line;
        this.text = text;
        this.placeholder = placeholder;
        if (adjust == null) {
            this.adjust = new float[0];
        } else {
            this.adjust = new float[adjust.length];
            System.arraycopy(adjust, 0, this.adjust, 0, adjust.length);
        }
    }

    /// A text box: text in a rectangle with no fill or outline.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `text`: the text
    ///
    /// #### Returns
    ///
    /// the shape
    public static ShapeElement textBox(String id, float x, float y, float width, float height,
            TextFrame text) {
        return new ShapeElement(id, "rect", x, y, width, height, null, null, text);
    }

    /// A straight line or connector from one point to another.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `x1`: start x
    ///
    /// - `y1`: start y
    ///
    /// - `x2`: end x
    ///
    /// - `y2`: end y
    ///
    /// - `line`: the line style, with its arrowheads
    ///
    /// #### Returns
    ///
    /// the shape
    public static ShapeElement line(String id, float x1, float y1, float x2, float y2,
            LineStyle line) {
        return new ShapeElement(id, null, "line", Math.min(x1, x2), Math.min(y1, y2),
                Math.abs(x2 - x1), Math.abs(y2 - y1), 0, x2 < x1, y2 < y1, null, line, null,
                null, null);
    }

    /// The number of adjustment values.
    ///
    /// #### Returns
    ///
    /// the count
    public int adjustCount() {
        return adjust.length;
    }

    /// An adjustment value, or a default when it is not set.
    ///
    /// #### Parameters
    ///
    /// - `index`: which
    ///
    /// - `fallback`: the default
    ///
    /// #### Returns
    ///
    /// the value
    public float adjust(int index, float fallback) {
        return index >= 0 && index < adjust.length ? adjust[index] : fallback;
    }

    /// True for a line or connector, which has no inside.
    ///
    /// #### Returns
    ///
    /// true for a line
    public boolean isLine() {
        return ShapeGeometry.isLine(geometry);
    }

    @Override
    public SlideElement withTransform(float nx, float ny, float nw, float nh, float rot,
            boolean fh, boolean fv) {
        return new ShapeElement(id, name, geometry, nx, ny, nw, nh, rot, fh, fv, fill, line,
                text, placeholder, adjust);
    }

    @Override
    public SlideElement withId(String value) {
        return new ShapeElement(value, name, geometry, x, y, width, height, rotation, flipH,
                flipV, fill, line, text, placeholder, adjust);
    }

    /// This shape with other text.
    ///
    /// #### Parameters
    ///
    /// - `value`: the text, or null
    ///
    /// #### Returns
    ///
    /// the shape
    public ShapeElement withText(TextFrame value) {
        return new ShapeElement(id, name, geometry, x, y, width, height, rotation, flipH, flipV,
                fill, line, value, placeholder, adjust);
    }

    /// This shape with another fill.
    ///
    /// #### Parameters
    ///
    /// - `value`: the fill
    ///
    /// #### Returns
    ///
    /// the shape
    public ShapeElement withFill(Fill value) {
        return new ShapeElement(id, name, geometry, x, y, width, height, rotation, flipH, flipV,
                value, line, text, placeholder, adjust);
    }

    /// This shape with another outline.
    ///
    /// #### Parameters
    ///
    /// - `value`: the outline
    ///
    /// #### Returns
    ///
    /// the shape
    public ShapeElement withLine(LineStyle value) {
        return new ShapeElement(id, name, geometry, x, y, width, height, rotation, flipH, flipV,
                fill, value, text, placeholder, adjust);
    }

    /// This shape with another geometry.
    ///
    /// #### Parameters
    ///
    /// - `value`: the preset name
    ///
    /// #### Returns
    ///
    /// the shape
    public ShapeElement withGeometry(String value) {
        return new ShapeElement(id, name, value, x, y, width, height, rotation, flipH, flipV,
                fill, line, text, placeholder, null);
    }

    /// This shape with a placeholder role.
    ///
    /// #### Parameters
    ///
    /// - `value`: the role, or null
    ///
    /// #### Returns
    ///
    /// the shape
    public ShapeElement withPlaceholder(String value) {
        return new ShapeElement(id, name, geometry, x, y, width, height, rotation, flipH, flipV,
                fill, line, text, value, adjust);
    }

    @Override
    public String plainText() {
        return text == null ? "" : text.plainText();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ShapeElement)) {
            return false;
        }
        ShapeElement s = (ShapeElement) o;
        if (!sameFrame(s) || !geometry.equals(s.geometry) || !fill.equals(s.fill)
                || !line.equals(s.line) || adjust.length != s.adjust.length) {
            return false;
        }
        for (int i = 0; i < adjust.length; i++) {
            if (adjust[i] != s.adjust[i]) {
                return false;
            }
        }
        return (text == null ? s.text == null : text.equals(s.text))
                && (placeholder == null ? s.placeholder == null
                : placeholder.equals(s.placeholder));
    }

    @Override
    public int hashCode() {
        return frameHash() * 31 + geometry.hashCode() + fill.hashCode() * 7
                + (text == null ? 0 : text.hashCode());
    }

    @Override
    public String toString() {
        return "ShapeElement[" + id + " " + geometry + " " + x + "," + y + " " + width + "x"
                + height + "]";
    }
}
