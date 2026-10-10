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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.NamedArg;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/// How the line of a border is drawn: solid, dashed, dotted or not at
/// all, and how its corners and ends are finished.
public final class BorderStrokeStyle {

    /// No line.
    public static final BorderStrokeStyle NONE = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER,
            StrokeLineCap.BUTT, 0, 0, null);

    /// A line of round dots.
    public static final BorderStrokeStyle DOTTED = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER,
            StrokeLineCap.ROUND, 10, 0, list(0, 2));

    /// A line of dashes.
    public static final BorderStrokeStyle DASHED = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER,
            StrokeLineCap.BUTT, 10, 0, list(2, 1.4));

    /// An unbroken line.
    public static final BorderStrokeStyle SOLID = new BorderStrokeStyle(StrokeType.INSIDE, StrokeLineJoin.MITER,
            StrokeLineCap.BUTT, 10, 0, null);

    private final StrokeType type;
    private final StrokeLineJoin lineJoin;
    private final StrokeLineCap lineCap;
    private final double miterLimit;
    private final double dashOffset;
    private final List<Double> dashArray;

    /// Creates a style. The dash lengths are in multiples of the stroke
    /// width for the predefined styles and in logical pixels otherwise.
    public BorderStrokeStyle(@NamedArg("type") StrokeType type, @NamedArg("lineJoin") StrokeLineJoin lineJoin,
            @NamedArg("lineCap") StrokeLineCap lineCap, @NamedArg("miterLimit") double miterLimit,
            @NamedArg("dashOffset") double dashOffset, @NamedArg("dashArray") List<Double> dashArray) {
        this.type = type != null ? type : StrokeType.CENTERED;
        this.lineJoin = lineJoin != null ? lineJoin : StrokeLineJoin.MITER;
        this.lineCap = lineCap != null ? lineCap : StrokeLineCap.BUTT;
        this.miterLimit = miterLimit;
        this.dashOffset = dashOffset;
        if (dashArray == null) {
            this.dashArray = Collections.<Double>emptyList();
        } else {
            this.dashArray = Collections.unmodifiableList(new ArrayList<Double>(dashArray));
        }
    }

    private static List<Double> list(double a, double b) {
        ArrayList<Double> l = new ArrayList<Double>();
        l.add(Double.valueOf(a));
        l.add(Double.valueOf(b));
        return l;
    }

    /// Returns where the line lies relative to the edge.
    public final StrokeType getType() {
        return type;
    }

    /// Returns how corners are joined.
    public final StrokeLineJoin getLineJoin() {
        return lineJoin;
    }

    /// Returns how dash ends are finished.
    public final StrokeLineCap getLineCap() {
        return lineCap;
    }

    /// Returns the limit beyond which a mitred corner is cut off.
    public final double getMiterLimit() {
        return miterLimit;
    }

    /// Returns where in the dash pattern the line starts.
    public final double getDashOffset() {
        return dashOffset;
    }

    /// Returns the lengths of dashes and gaps; empty for a solid line.
    public final List<Double> getDashArray() {
        return dashArray;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BorderStrokeStyle)) {
            return false;
        }
        BorderStrokeStyle that = (BorderStrokeStyle) o;
        if ((this == NONE) != (that == NONE)) {
            return false;
        }
        return type == that.type && lineJoin == that.lineJoin && lineCap == that.lineCap
                && Double.compare(miterLimit, that.miterLimit) == 0
                && Double.compare(dashOffset, that.dashOffset) == 0 && dashArray.equals(that.dashArray);
    }

    @Override
    public int hashCode() {
        return ((type.hashCode() * 31 + lineJoin.hashCode()) * 31 + lineCap.hashCode()) * 31 + dashArray.hashCode()
                + (this == NONE ? 0 : 1);
    }

    @Override
    public String toString() {
        if (this == NONE) {
            return "BorderStyle.NONE";
        } else if (this == DASHED) {
            return "BorderStyle.DASHED";
        } else if (this == DOTTED) {
            return "BorderStyle.DOTTED";
        } else if (this == SOLID) {
            return "BorderStyle.SOLID";
        }
        return "BorderStyle [" + type + ", " + lineJoin + ", " + lineCap + ", " + miterLimit + ", " + dashOffset
                + ", " + dashArray + "]";
    }
}
