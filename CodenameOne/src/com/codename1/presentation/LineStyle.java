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

/// How the outline of a shape, or a line or connector, is drawn.
public final class LineStyle {
    /// A solid line.
    public static final String SOLID = "solid";

    /// A dashed line.
    public static final String DASH = "dash";

    /// A dotted line.
    public static final String DOT = "dot";

    /// No arrowhead.
    public static final String ARROW_NONE = "none";

    /// A filled triangle arrowhead.
    public static final String ARROW_TRIANGLE = "triangle";

    /// An open, two-stroke arrowhead.
    public static final String ARROW_OPEN = "arrow";

    /// A round dot at the end.
    public static final String ARROW_OVAL = "oval";

    /// No outline.
    public static final LineStyle NONE = new LineStyle(0, 0, SOLID, 0, ARROW_NONE, ARROW_NONE);

    /// The color as 0xRRGGBB.
    public final int color;

    /// The width in points; 0 draws nothing.
    public final float width;

    /// `SOLID`, `DASH` or `DOT`.
    public final String dash;

    /// The opacity, 0 to 1.
    public final float alpha;

    /// The arrowhead at the start of a line.
    public final String head;

    /// The arrowhead at the end of a line.
    public final String tail;

    /// An opaque solid outline.
    ///
    /// #### Parameters
    ///
    /// - `color`: 0xRRGGBB
    ///
    /// - `width`: points
    public LineStyle(int color, float width) {
        this(color, width, SOLID, 1, ARROW_NONE, ARROW_NONE);
    }

    /// A line style.
    ///
    /// #### Parameters
    ///
    /// - `color`: 0xRRGGBB
    ///
    /// - `width`: points
    ///
    /// - `dash`: `SOLID`, `DASH` or `DOT`
    ///
    /// - `alpha`: 0 to 1
    ///
    /// - `head`: the start arrowhead
    ///
    /// - `tail`: the end arrowhead
    public LineStyle(int color, float width, String dash, float alpha, String head,
            String tail) {
        this.color = color & 0xffffff;
        this.width = width < 0 ? 0 : width;
        this.dash = DASH.equals(dash) || DOT.equals(dash) ? dash : SOLID;
        this.alpha = alpha < 0 ? 0 : alpha > 1 ? 1 : alpha;
        this.head = arrow(head);
        this.tail = arrow(tail);
    }

    private static String arrow(String s) {
        return ARROW_TRIANGLE.equals(s) || ARROW_OPEN.equals(s) || ARROW_OVAL.equals(s) ? s
                : ARROW_NONE;
    }

    /// This style with arrowheads.
    ///
    /// #### Parameters
    ///
    /// - `start`: the start arrowhead
    ///
    /// - `end`: the end arrowhead
    ///
    /// #### Returns
    ///
    /// the style
    public LineStyle withArrows(String start, String end) {
        return new LineStyle(color, width, dash, alpha, start, end);
    }

    /// This style with a dash pattern.
    ///
    /// #### Parameters
    ///
    /// - `value`: `SOLID`, `DASH` or `DOT`
    ///
    /// #### Returns
    ///
    /// the style
    public LineStyle withDash(String value) {
        return new LineStyle(color, width, value, alpha, head, tail);
    }

    /// True when the line draws something.
    ///
    /// #### Returns
    ///
    /// true for a positive width and opacity
    public boolean isVisible() {
        return width > 0 && alpha > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LineStyle)) {
            return false;
        }
        LineStyle l = (LineStyle) o;
        return color == l.color && width == l.width && dash.equals(l.dash) && alpha == l.alpha
                && head.equals(l.head) && tail.equals(l.tail);
    }

    @Override
    public int hashCode() {
        return (color * 31 + Float.floatToIntBits(width)) * 31 + dash.hashCode()
                + head.hashCode() * 7 + tail.hashCode();
    }
}
