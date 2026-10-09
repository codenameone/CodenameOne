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
package com.codename1.fxcompat.runtime;

import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/// The colour of a gradient at a point, worked out from the definition of
/// the gradient and nothing else. [Renderer] uses it wherever the
/// platform has no paint of its own for a gradient: a radial gradient
/// with a focus or a cycle, and any gradient that strokes an outline.
///
/// A gradient is a line of colours and a rule that gives every point a
/// place on that line. The place is a number: 0 is the first colour, 1
/// the last, and a number outside is brought back in by the cycle
/// method.
public final class GradientRaster {

    /// The entries of a colour table.
    static final int TABLE = 256;

    /// The nearest a focus may come to the edge of its circle, as a
    /// fraction of the radius. On the edge itself every ray from the focus
    /// would be the whole gradient at once.
    private static final double FOCUS_LIMIT = 0.98;

    private GradientRaster() {
    }

    /// Returns where on a linear gradient a point is: its foot on the
    /// line from the start to the end, 0 at the start and 1 at the end.
    public static double linearOffset(double x, double y, double startX, double startY, double endX, double endY) {
        double dx = endX - startX;
        double dy = endY - startY;
        double length = dx * dx + dy * dy;
        if (!(length > 0)) {
            return 1;
        }
        return ((x - startX) * dx + (y - startY) * dy) / length;
    }

    /// Returns where on a radial gradient a point is. A ray leaves the
    /// focus through the point and meets the circle; the answer is how far
    /// along that ray the point is, 0 at the focus and 1 on the circle.
    public static double radialOffset(double x, double y, double centerX, double centerY, double radius,
            double focusX, double focusY) {
        if (!(radius > 0)) {
            return 1;
        }
        double dx = x - focusX;
        double dy = y - focusY;
        double ex = focusX - centerX;
        double ey = focusY - centerY;
        double a = dx * dx + dy * dy;
        if (!(a > 0)) {
            return 0;
        }
        double b = 2 * (ex * dx + ey * dy);
        double c = ex * ex + ey * ey - radius * radius;
        double root = b * b - 4 * a * c;
        if (root < 0) {
            return 1;
        }
        double reach = -b + Math.sqrt(root);
        return reach > 0 ? 2 * a / reach : 1;
    }

    /// Returns the focus of a radial gradient: the point that takes the
    /// first colour, at an angle and a share of the radius from the centre.
    /// The first entry is x, the second y.
    public static double[] focus(double centerX, double centerY, double radius, double focusAngle,
            double focusDistance) {
        double d = Math.max(-FOCUS_LIMIT, Math.min(FOCUS_LIMIT, focusDistance));
        double angle = Math.toRadians(focusAngle);
        return new double[] {centerX + d * radius * Math.cos(angle), centerY + d * radius * Math.sin(angle)};
    }

    /// Brings a place on the line of colours back between 0 and 1. Without
    /// a cycle everything before the line is its start and everything past
    /// it its end; a repeat starts over, and a reflection walks back.
    public static double cycle(double offset, CycleMethod method) {
        if (offset != offset) {
            return 0;
        }
        if (method == CycleMethod.REPEAT) {
            return offset - Math.floor(offset);
        } else if (method == CycleMethod.REFLECT) {
            double twice = offset / 2;
            double t = (twice - Math.floor(twice)) * 2;
            return t > 1 ? 2 - t : t;
        }
        return offset < 0 ? 0 : (offset > 1 ? 1 : offset);
    }

    /// Returns the colour of a line of stops at a place between 0 and 1.
    public static Color colorAt(List<Stop> stops, double t) {
        Stop previous = stops.get(0);
        if (t <= previous.getOffset()) {
            return previous.getColor();
        }
        for (int i = 1; i < stops.size(); i++) {
            Stop next = stops.get(i);
            if (t <= next.getOffset()) {
                double span = next.getOffset() - previous.getOffset();
                return span <= 0 ? next.getColor()
                        : previous.getColor().interpolate(next.getColor(), (t - previous.getOffset()) / span);
            }
            previous = next;
        }
        return previous.getColor();
    }

    /// Returns the line of colours as a table of ARGB values, the first
    /// entry at 0 and the last at 1.
    static int[] table(List<Stop> stops) {
        int[] out = new int[TABLE];
        for (int i = 0; i < TABLE; i++) {
            out[i] = colorAt(stops, i / (double) (TABLE - 1)).cn1Argb();
        }
        return out;
    }

    /// Returns the place a point has on a gradient, between 0 and 1, or
    /// -1 for a paint that is not a gradient. The rectangle is the one a
    /// proportional gradient is measured against.
    public static double offsetAt(Paint paint, double x, double y, double bx, double by, double bw, double bh) {
        if (paint instanceof LinearGradient) {
            LinearGradient lg = (LinearGradient) paint;
            boolean p = lg.isProportional();
            return cycle(linearOffset(x, y, p ? bx + lg.getStartX() * bw : lg.getStartX(),
                    p ? by + lg.getStartY() * bh : lg.getStartY(), p ? bx + lg.getEndX() * bw : lg.getEndX(),
                    p ? by + lg.getEndY() * bh : lg.getEndY()), lg.getCycleMethod());
        } else if (paint instanceof RadialGradient) {
            RadialGradient rg = (RadialGradient) paint;
            double px = x;
            double py = y;
            if (rg.isProportional()) {
                // A proportional gradient is a circle in a square of side
                // one that is stretched over the rectangle, so over a
                // rectangle that is not square it is an ellipse. The point
                // is taken into the square instead.
                if (!(bw > 0) || !(bh > 0)) {
                    return 1;
                }
                px = (x - bx) / bw;
                py = (y - by) / bh;
            }
            double[] f = focus(rg.getCenterX(), rg.getCenterY(), rg.getRadius(), rg.getFocusAngle(),
                    rg.getFocusDistance());
            return cycle(radialOffset(px, py, rg.getCenterX(), rg.getCenterY(), rg.getRadius(), f[0], f[1]),
                    rg.getCycleMethod());
        }
        return -1;
    }

    /// Returns the colour a gradient has at a point, `null` for a paint
    /// that is not a gradient.
    public static Color colorAt(Paint paint, double x, double y, double bx, double by, double bw, double bh) {
        double t = offsetAt(paint, x, y, bx, by, bw, bh);
        if (paint instanceof LinearGradient) {
            return colorAt(((LinearGradient) paint).getStops(), t);
        } else if (paint instanceof RadialGradient) {
            return colorAt(((RadialGradient) paint).getStops(), t);
        }
        return null;
    }

    /// Colours a mask. Every entry of `mask` is a pixel whose alpha says
    /// how much of it the shape covers; it is replaced by the colour of
    /// the gradient there, that much of it. `inverse` takes the middle of
    /// a pixel, counted from `left` and `top`, to the coordinates the
    /// gradient is written in.
    static void colour(int[] mask, int width, int height, int left, int top, double[] inverse, Paint paint,
            double bx, double by, double bw, double bh, double opacity) {
        List<Stop> stops;
        if (paint instanceof LinearGradient) {
            stops = ((LinearGradient) paint).getStops();
        } else if (paint instanceof RadialGradient) {
            stops = ((RadialGradient) paint).getStops();
        } else {
            return;
        }
        int[] table = table(stops);
        double share = Math.max(0, Math.min(1, opacity));
        for (int row = 0; row < height; row++) {
            double dy = top + row + 0.5;
            int at = row * width;
            for (int column = 0; column < width; column++, at++) {
                int cover = mask[at] >>> 24;
                if (cover == 0) {
                    mask[at] = 0;
                    continue;
                }
                double dx = left + column + 0.5;
                double x = inverse[0] * dx + inverse[2] * dy + inverse[4];
                double y = inverse[1] * dx + inverse[3] * dy + inverse[5];
                int c = table[(int) (offsetAt(paint, x, y, bx, by, bw, bh) * (TABLE - 1) + 0.5)];
                int alpha = (int) ((c >>> 24) * (cover / 255.0) * share + 0.5);
                mask[at] = (alpha << 24) | (c & 0xffffff);
            }
        }
    }
}
