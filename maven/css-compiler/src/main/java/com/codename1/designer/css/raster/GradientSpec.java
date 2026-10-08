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
package com.codename1.designer.css.raster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A CSS gradient: `linear-gradient`, `radial-gradient`, `conic-gradient`
/// and their `repeating-` variants.
///
/// Built by hand with the no-argument constructor (or one of the static
/// factories) and the fluent setters, which carry the field names:
///
/// ```java
/// GradientSpec g = GradientSpec.linear(90)
///         .addStop(0xffff0000)
///         .addStop(0xff0000ff);
/// ```
///
/// The defaults are the CSS ones: a linear gradient `to bottom`, an
/// `ellipse farthest-corner` radial shape, and a centre of `50% 50%`.
/// [BoxStyle] copies the spec it is given, so a spec can be reused and
/// changed after it was handed to a builder.
public final class GradientSpec {
    /// The gradient function.
    public enum Type { LINEAR, RADIAL, CONIC }

    /// The radial ending shape.
    public enum Shape { CIRCLE, ELLIPSE }

    /// The radial size.
    public enum Extent { CLOSEST_SIDE, FARTHEST_SIDE, CLOSEST_CORNER, FARTHEST_CORNER, EXPLICIT }

    /// The unit of a stop position. `NONE` is a stop without a position,
    /// which the CSS fix-up places. `DEGREES` is only valid in a conic
    /// gradient and `PX` only in a linear or radial one.
    public enum Unit { NONE, PERCENT, PX, DEGREES }

    /// A colour stop. Immutable.
    public static final class Stop {
        /// The stop colour as non-premultiplied ARGB.
        public final int color;
        /// The position, in [#unit]; ignored for [Unit#NONE].
        public final double position;
        /// The unit of [#position].
        public final Unit unit;

        /// A stop without a position.
        public Stop(int color) {
            this(color, 0, Unit.NONE);
        }

        public Stop(int color, double position, Unit unit) {
            this.color = color;
            this.position = position;
            this.unit = unit;
        }

        public int getColor() {
            return color;
        }

        public double getPosition() {
            return position;
        }

        public Unit getUnit() {
            return unit;
        }
    }

    private Type type = Type.LINEAR;
    private boolean repeating;
    private double angleDeg = 180;
    private boolean toCorner;
    private int cornerX;
    private int cornerY;
    private Shape shape = Shape.ELLIPSE;
    private Extent extent = Extent.FARTHEST_CORNER;
    private double radiusX;
    private double radiusY;
    private double centerX = 50;
    private double centerY = 50;
    private boolean centerXPercent = true;
    private boolean centerYPercent = true;
    private double fromAngleDeg;
    private List<Stop> stops = new ArrayList<Stop>();

    /// Creates a linear gradient `to bottom` with no stops.
    public GradientSpec() {
    }

    /// Creates a copy of another spec.
    public GradientSpec(GradientSpec o) {
        type = o.type;
        repeating = o.repeating;
        angleDeg = o.angleDeg;
        toCorner = o.toCorner;
        cornerX = o.cornerX;
        cornerY = o.cornerY;
        shape = o.shape;
        extent = o.extent;
        radiusX = o.radiusX;
        radiusY = o.radiusY;
        centerX = o.centerX;
        centerY = o.centerY;
        centerXPercent = o.centerXPercent;
        centerYPercent = o.centerYPercent;
        fromAngleDeg = o.fromAngleDeg;
        stops = o.stops == null ? null : new ArrayList<Stop>(o.stops);
    }

    /// A linear gradient along a CSS angle: 0 is `to top`, 90 is
    /// `to right`, clockwise.
    public static GradientSpec linear(double angleDeg) {
        return new GradientSpec().type(Type.LINEAR).angleDeg(angleDeg);
    }

    /// A linear gradient `to` a side or corner. Each argument is -1, 0 or
    /// 1; `to top right` is `(1, -1)` and `to left` is `(-1, 0)`.
    public static GradientSpec linearTo(int cornerX, int cornerY) {
        return new GradientSpec().type(Type.LINEAR).toCorner(true).cornerX(cornerX).cornerY(cornerY);
    }

    /// A radial gradient of the given shape and extent centred at `50% 50%`.
    public static GradientSpec radial(Shape shape, Extent extent) {
        return new GradientSpec().type(Type.RADIAL).shape(shape).extent(extent);
    }

    /// A conic gradient starting at the given angle, centred at `50% 50%`.
    public static GradientSpec conic(double fromAngleDeg) {
        return new GradientSpec().type(Type.CONIC).fromAngleDeg(fromAngleDeg);
    }

    public GradientSpec type(Type v) {
        type = v;
        return this;
    }

    public GradientSpec repeating(boolean v) {
        repeating = v;
        return this;
    }

    /// Sets the linear angle and clears [#toCorner(boolean)].
    public GradientSpec angleDeg(double v) {
        angleDeg = v;
        toCorner = false;
        return this;
    }

    public GradientSpec toCorner(boolean v) {
        toCorner = v;
        return this;
    }

    public GradientSpec cornerX(int v) {
        cornerX = v;
        return this;
    }

    public GradientSpec cornerY(int v) {
        cornerY = v;
        return this;
    }

    public GradientSpec shape(Shape v) {
        shape = v;
        return this;
    }

    public GradientSpec extent(Extent v) {
        extent = v;
        return this;
    }

    public GradientSpec radiusX(double v) {
        radiusX = v;
        return this;
    }

    public GradientSpec radiusY(double v) {
        radiusY = v;
        return this;
    }

    public GradientSpec centerX(double v) {
        centerX = v;
        return this;
    }

    public GradientSpec centerY(double v) {
        centerY = v;
        return this;
    }

    public GradientSpec centerXPercent(boolean v) {
        centerXPercent = v;
        return this;
    }

    public GradientSpec centerYPercent(boolean v) {
        centerYPercent = v;
        return this;
    }

    /// Sets the centre of a radial or conic gradient in one call.
    public GradientSpec center(double x, boolean xPercent, double y, boolean yPercent) {
        centerX = x;
        centerXPercent = xPercent;
        centerY = y;
        centerYPercent = yPercent;
        return this;
    }

    public GradientSpec fromAngleDeg(double v) {
        fromAngleDeg = v;
        return this;
    }

    /// Replaces the stops with a copy of the given list.
    public GradientSpec stops(List<Stop> v) {
        stops = v == null ? null : new ArrayList<Stop>(v);
        return this;
    }

    /// Appends a stop without a position.
    public GradientSpec addStop(int color) {
        return addStop(new Stop(color));
    }

    /// Appends a positioned stop.
    public GradientSpec addStop(int color, double position, Unit unit) {
        return addStop(new Stop(color, position, unit));
    }

    public GradientSpec addStop(Stop stop) {
        if (stops == null) {
            stops = new ArrayList<Stop>();
        }
        stops.add(stop);
        return this;
    }

    public Type getType() {
        return type;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public double getAngleDeg() {
        return angleDeg;
    }

    public boolean isToCorner() {
        return toCorner;
    }

    public int getCornerX() {
        return cornerX;
    }

    public int getCornerY() {
        return cornerY;
    }

    public Shape getShape() {
        return shape;
    }

    public Extent getExtent() {
        return extent;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public double getRadiusY() {
        return radiusY;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public boolean isCenterXPercent() {
        return centerXPercent;
    }

    public boolean isCenterYPercent() {
        return centerYPercent;
    }

    public double getFromAngleDeg() {
        return fromAngleDeg;
    }

    /// The stops, as an unmodifiable view; `null` when they were set to `null`.
    public List<Stop> getStops() {
        return stops == null ? null : Collections.unmodifiableList(stops);
    }
}
