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
package javafx.scene.shape;

import com.codename1.fxcompat.runtime.FxPath;

import javafx.beans.property.DoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

/// A straight line between two points. It has a black stroke and no fill
/// to begin with, and a fill would have nothing to cover.
///
/// The layout bounds are those of the stroked segment exactly: half the
/// stroke width to either side, and beyond each end only when the cap is
/// not `BUTT`.
public class Line extends Shape {

    private final DoubleProperty startX = geometry("startX", 0);
    private final DoubleProperty startY = geometry("startY", 0);
    private final DoubleProperty endX = geometry("endX", 0);
    private final DoubleProperty endY = geometry("endY", 0);

    /// Creates a line of no length at the origin.
    public Line() {
        super(true);
    }

    /// Creates a line between two points.
    public Line(double startX, double startY, double endX, double endY) {
        super(true);
        this.startX.set(startX);
        this.startY.set(startY);
        this.endX.set(endX);
        this.endY.set(endY);
    }

    /// Returns the x of the start.
    public final double getStartX() {
        return startX.get();
    }

    /// Sets the x of the start.
    public final void setStartX(double value) {
        startX.set(value);
    }

    /// The x of the start.
    public final DoubleProperty startXProperty() {
        return startX;
    }

    /// Returns the y of the start.
    public final double getStartY() {
        return startY.get();
    }

    /// Sets the y of the start.
    public final void setStartY(double value) {
        startY.set(value);
    }

    /// The y of the start.
    public final DoubleProperty startYProperty() {
        return startY;
    }

    /// Returns the x of the end.
    public final double getEndX() {
        return endX.get();
    }

    /// Sets the x of the end.
    public final void setEndX(double value) {
        endX.set(value);
    }

    /// The x of the end.
    public final DoubleProperty endXProperty() {
        return endX;
    }

    /// Returns the y of the end.
    public final double getEndY() {
        return endY.get();
    }

    /// Sets the y of the end.
    public final void setEndY(double value) {
        endY.set(value);
    }

    /// The y of the end.
    public final DoubleProperty endYProperty() {
        return endY;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        p.moveTo(getStartX(), getStartY());
        p.lineTo(getEndX(), getEndY());
        return p;
    }

    private double halfWidth() {
        double w = Math.max(0, getStrokeWidth());
        // A line has no inside, so only the width itself matters.
        return getStrokeType() == StrokeType.CENTERED ? w / 2 : w;
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        if (getStroke() == null) {
            return empty();
        }
        double x1 = getStartX();
        double y1 = getStartY();
        double x2 = getEndX();
        double y2 = getEndY();
        double half = halfWidth();
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        double padX = half;
        double padY = half;
        if (length > 0) {
            // Across the line the stroke reaches half its width; along
            // it only a cap does.
            double ux = Math.abs(dx / length);
            double uy = Math.abs(dy / length);
            double cap = getStrokeLineCap() == StrokeLineCap.BUTT ? 0 : half;
            padX = uy * half + ux * cap;
            padY = ux * half + uy * cap;
        }
        double minX = Math.min(x1, x2) - padX;
        double minY = Math.min(y1, y2) - padY;
        return new BoundingBox(minX, minY, Math.max(x1, x2) + padX - minX, Math.max(y1, y2) + padY - minY);
    }

    @Override
    public boolean contains(double localX, double localY) {
        return getStroke() != null && outline().nearOutline(localX, localY, halfWidth());
    }
}
