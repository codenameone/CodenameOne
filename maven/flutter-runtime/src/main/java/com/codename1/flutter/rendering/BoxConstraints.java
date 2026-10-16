/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.rendering;

import com.codename1.flutter.EdgeInsets;

/**
 * Flutter's box constraints: a min/max range for each axis.
 * Constraints flow down the render tree, sizes flow back up.
 * {@code Double.POSITIVE_INFINITY} marks an unbounded max.
 *
 * <p>The framework treats instances as immutable values; the no-arg
 * constructor and the void setters exist only for transpiled Dart code
 * ({@code BoxConstraints(minWidth: ..., maxWidth: ...)} becomes allocate +
 * setter calls). Values arriving from Dart are logical pixels — widgets that
 * consume them (ConstrainedBox) convert to device pixels.</p>
 */
public final class BoxConstraints {

    private double minWidth;
    private double maxWidth;
    private double minHeight;
    private double maxHeight;

    /**
     * Dart-facing constructor: all named parameters optional, defaulting to
     * the unconstrained range (0..&#8734; on both axes).
     */
    public BoxConstraints() {
        this(0, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY);
    }

    public BoxConstraints(double minWidth, double maxWidth, double minHeight, double maxHeight) {
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    /** Named parameter setter for the Dart {@code minWidth:} parameter. */
    public void minWidth(double v) {
        this.minWidth = v;
    }

    /** Named parameter setter for the Dart {@code maxWidth:} parameter. */
    public void maxWidth(double v) {
        this.maxWidth = v;
    }

    /** Named parameter setter for the Dart {@code minHeight:} parameter. */
    public void minHeight(double v) {
        this.minHeight = v;
    }

    /** Named parameter setter for the Dart {@code maxHeight:} parameter. */
    public void maxHeight(double v) {
        this.maxHeight = v;
    }

    /**
     * Constraints that force exactly the given size.
     */
    public static BoxConstraints tight(double width, double height) {
        return new BoxConstraints(width, width, height, height);
    }

    /**
     * Constraints that allow any size up to the given maximums.
     */
    public static BoxConstraints loose(double maxWidth, double maxHeight) {
        return new BoxConstraints(0, maxWidth, 0, maxHeight);
    }

    public double minWidth() {
        return minWidth;
    }

    public double maxWidth() {
        return maxWidth;
    }

    public double minHeight() {
        return minHeight;
    }

    public double maxHeight() {
        return maxHeight;
    }

    public boolean hasBoundedWidth() {
        return maxWidth != Double.POSITIVE_INFINITY;
    }

    public boolean hasBoundedHeight() {
        return maxHeight != Double.POSITIVE_INFINITY;
    }

    public boolean hasTightWidth() {
        return minWidth >= maxWidth;
    }

    public boolean hasTightHeight() {
        return minHeight >= maxHeight;
    }

    public boolean isTight() {
        return hasTightWidth() && hasTightHeight();
    }

    public double constrainWidth(double width) {
        return clamp(width, minWidth, maxWidth);
    }

    public double constrainHeight(double height) {
        return clamp(height, minHeight, maxHeight);
    }

    /**
     * The size closest to {@code size} that satisfies these constraints.
     */
    public Size constrain(Size size) {
        return new Size(constrainWidth(size.width()), constrainHeight(size.height()));
    }

    /**
     * Constrains a size while keeping its aspect ratio, as Flutter's
     * {@code constrainSizeAndAttemptToPreserveAspectRatio} does.
     *
     * <p>{@link #constrain} clamps the two axes independently, which throws the
     * ratio away: a picture whose natural size is wider than the box comes back
     * with the box's width and its own height. That is how an image laid out
     * under a width-driven fit ended up in a box taller than its content, with
     * the artwork centred in the slack and everything below it pushed down.</p>
     *
     * <p>Each clamp here carries the other axis with it, and the order matters:
     * width, then height, then the minimums, so a later clamp corrects an
     * earlier one rather than being overwritten by it. A tight box has only one
     * answer and keeps no ratio.</p>
     */
    public Size constrainSizeAndAttemptToPreserveAspectRatio(Size size) {
        if (isTight()) {
            return smallest();
        }
        double width = size.width();
        double height = size.height();
        if (width <= 0 || height <= 0) {
            return constrain(size);
        }
        double aspectRatio = width / height;
        if (width > maxWidth()) {
            width = maxWidth();
            height = width / aspectRatio;
        }
        if (height > maxHeight()) {
            height = maxHeight();
            width = height * aspectRatio;
        }
        if (width < minWidth()) {
            width = minWidth();
            height = width / aspectRatio;
        }
        if (height < minHeight()) {
            height = minHeight();
            width = height * aspectRatio;
        }
        return new Size(constrainWidth(width), constrainHeight(height));
    }

    public Size smallest() {
        return new Size(constrainWidth(0), constrainHeight(0));
    }

    public Size biggest() {
        return new Size(constrainWidth(Double.POSITIVE_INFINITY), constrainHeight(Double.POSITIVE_INFINITY));
    }

    /**
     * New constraints with the given edges removed, never going below zero
     * (Flutter's BoxConstraints.deflate). The insets are interpreted in the
     * same unit as these constraints.
     */
    public BoxConstraints deflate(EdgeInsets edges) {
        double horizontal = edges.left() + edges.right();
        double vertical = edges.top() + edges.bottom();
        double deflatedMinWidth = Math.max(0, minWidth - horizontal);
        double deflatedMinHeight = Math.max(0, minHeight - vertical);
        return new BoxConstraints(
                deflatedMinWidth,
                Math.max(deflatedMinWidth, maxWidth - horizontal),
                deflatedMinHeight,
                Math.max(deflatedMinHeight, maxHeight - vertical));
    }

    /**
     * These constraints (the "additional" ones, e.g. a ConstrainedBox's) with
     * every value clamped into the given bounds — Flutter's
     * {@code BoxConstraints.enforce}: the result respects {@code bounds} while
     * getting as close to these constraints as possible.
     */
    public BoxConstraints enforce(BoxConstraints bounds) {
        return new BoxConstraints(
                clamp(minWidth, bounds.minWidth, bounds.maxWidth),
                clamp(maxWidth, bounds.minWidth, bounds.maxWidth),
                clamp(minHeight, bounds.minHeight, bounds.maxHeight),
                clamp(maxHeight, bounds.minHeight, bounds.maxHeight));
    }

    /**
     * New constraints with the minimum extents removed.
     */
    public BoxConstraints loosen() {
        return new BoxConstraints(0, maxWidth, 0, maxHeight);
    }

    /**
     * New constraints with the given dimensions (when non-null) tightened as
     * close to the requested value as these constraints allow (Flutter's
     * BoxConstraints.tighten).
     */
    public BoxConstraints tighten(Double width, Double height) {
        double minW = width == null ? minWidth : clamp(width, minWidth, maxWidth);
        double maxW = width == null ? maxWidth : clamp(width, minWidth, maxWidth);
        double minH = height == null ? minHeight : clamp(height, minHeight, maxHeight);
        double maxH = height == null ? maxHeight : clamp(height, minHeight, maxHeight);
        return new BoxConstraints(minW, maxW, minH, maxH);
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) {
            return min;
        }
        if (v > max) {
            return max;
        }
        return v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoxConstraints)) {
            return false;
        }
        BoxConstraints c = (BoxConstraints) o;
        return c.minWidth == minWidth && c.maxWidth == maxWidth
                && c.minHeight == minHeight && c.maxHeight == maxHeight;
    }

    @Override
    public int hashCode() {
        long bits = com.codename1.flutter.ValueHash.bits(minWidth);
        bits = bits * 31 + com.codename1.flutter.ValueHash.bits(maxWidth);
        bits = bits * 31 + com.codename1.flutter.ValueHash.bits(minHeight);
        bits = bits * 31 + com.codename1.flutter.ValueHash.bits(maxHeight);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public String toString() {
        return "BoxConstraints(" + minWidth + "<=w<=" + maxWidth + ", " + minHeight + "<=h<=" + maxHeight + ")";
    }
}
