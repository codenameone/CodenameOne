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
package javafx.scene.layout;

import javafx.beans.NamedArg;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/// One line around a region: its paint, style, corner radii and widths,
/// and how far it is inset from the edges.
///
/// A stroke whose four sides share a paint, a style and a width is drawn
/// as one rounded outline. One whose sides differ is drawn side by side as
/// straight lines, without the corner radii.
public class BorderStroke {

    /// A thin border, one logical pixel.
    public static final BorderWidths THIN = new BorderWidths(1);

    /// A medium border, three logical pixels.
    public static final BorderWidths MEDIUM = new BorderWidths(3);

    /// A thick border, five logical pixels.
    public static final BorderWidths THICK = new BorderWidths(5);

    /// The widths used when none are given.
    public static final BorderWidths DEFAULT_WIDTHS = THIN;

    private final Paint topStroke;
    private final Paint rightStroke;
    private final Paint bottomStroke;
    private final Paint leftStroke;
    private final BorderStrokeStyle topStyle;
    private final BorderStrokeStyle rightStyle;
    private final BorderStrokeStyle bottomStyle;
    private final BorderStrokeStyle leftStyle;
    private final BorderWidths widths;
    private final Insets insets;
    private final CornerRadii radii;

    /// Creates a stroke that is the same on every side.
    public BorderStroke(@NamedArg("stroke") Paint stroke, @NamedArg("style") BorderStrokeStyle style,
            @NamedArg("radii") CornerRadii radii, @NamedArg("widths") BorderWidths widths) {
        this(stroke, stroke, stroke, stroke, style, style, style, style, radii, widths, null);
    }

    /// Creates a stroke that is the same on every side and inset from the
    /// edges.
    public BorderStroke(@NamedArg("stroke") Paint stroke, @NamedArg("style") BorderStrokeStyle style,
            @NamedArg("radii") CornerRadii radii, @NamedArg("widths") BorderWidths widths,
            @NamedArg("insets") Insets insets) {
        this(stroke, stroke, stroke, stroke, style, style, style, style, radii, widths, insets);
    }

    /// Creates a stroke with a paint and a style per side.
    public BorderStroke(@NamedArg("topStroke") Paint topStroke, @NamedArg("rightStroke") Paint rightStroke,
            @NamedArg("bottomStroke") Paint bottomStroke, @NamedArg("leftStroke") Paint leftStroke,
            @NamedArg("topStyle") BorderStrokeStyle topStyle, @NamedArg("rightStyle") BorderStrokeStyle rightStyle,
            @NamedArg("bottomStyle") BorderStrokeStyle bottomStyle,
            @NamedArg("leftStyle") BorderStrokeStyle leftStyle, @NamedArg("radii") CornerRadii radii,
            @NamedArg("widths") BorderWidths widths, @NamedArg("insets") Insets insets) {
        this.topStroke = topStroke == null ? Color.BLACK : topStroke;
        this.rightStroke = rightStroke == null ? this.topStroke : rightStroke;
        this.bottomStroke = bottomStroke == null ? this.topStroke : bottomStroke;
        this.leftStroke = leftStroke == null ? this.rightStroke : leftStroke;
        this.topStyle = topStyle == null ? BorderStrokeStyle.NONE : topStyle;
        this.rightStyle = rightStyle == null ? this.topStyle : rightStyle;
        this.bottomStyle = bottomStyle == null ? this.topStyle : bottomStyle;
        this.leftStyle = leftStyle == null ? this.rightStyle : leftStyle;
        this.radii = radii == null ? CornerRadii.EMPTY : radii;
        this.widths = widths == null ? DEFAULT_WIDTHS : widths;
        this.insets = insets == null ? Insets.EMPTY : insets;
    }

    /// Returns the paint of the top side.
    public final Paint getTopStroke() {
        return topStroke;
    }

    /// Returns the paint of the right side.
    public final Paint getRightStroke() {
        return rightStroke;
    }

    /// Returns the paint of the bottom side.
    public final Paint getBottomStroke() {
        return bottomStroke;
    }

    /// Returns the paint of the left side.
    public final Paint getLeftStroke() {
        return leftStroke;
    }

    /// Returns the style of the top side.
    public final BorderStrokeStyle getTopStyle() {
        return topStyle;
    }

    /// Returns the style of the right side.
    public final BorderStrokeStyle getRightStyle() {
        return rightStyle;
    }

    /// Returns the style of the bottom side.
    public final BorderStrokeStyle getBottomStyle() {
        return bottomStyle;
    }

    /// Returns the style of the left side.
    public final BorderStrokeStyle getLeftStyle() {
        return leftStyle;
    }

    /// Returns the widths of the sides.
    public final BorderWidths getWidths() {
        return widths;
    }

    /// Returns the insets from the edges of the region.
    public final Insets getInsets() {
        return insets;
    }

    /// Returns the corner radii.
    public final CornerRadii getRadii() {
        return radii;
    }

    /// Returns whether all four sides have the same paint and style.
    public final boolean isStrokeUniform() {
        return topStroke.equals(rightStroke) && topStroke.equals(bottomStroke) && topStroke.equals(leftStroke)
                && topStyle.equals(rightStyle) && topStyle.equals(bottomStyle) && topStyle.equals(leftStyle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BorderStroke)) {
            return false;
        }
        BorderStroke that = (BorderStroke) o;
        return topStroke.equals(that.topStroke) && rightStroke.equals(that.rightStroke)
                && bottomStroke.equals(that.bottomStroke) && leftStroke.equals(that.leftStroke)
                && topStyle.equals(that.topStyle) && rightStyle.equals(that.rightStyle)
                && bottomStyle.equals(that.bottomStyle) && leftStyle.equals(that.leftStyle)
                && widths.equals(that.widths) && radii.equals(that.radii) && insets.equals(that.insets);
    }

    @Override
    public int hashCode() {
        return (((topStroke.hashCode() * 31 + topStyle.hashCode()) * 31 + widths.hashCode()) * 31
                + radii.hashCode()) * 31 + insets.hashCode();
    }
}
