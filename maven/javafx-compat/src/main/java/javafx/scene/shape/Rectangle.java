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
import javafx.scene.paint.Paint;

/// A rectangle, with rounded corners when `arcWidth` and `arcHeight` are
/// above zero: they are the diameters of the ellipse a corner is a
/// quarter of.
public class Rectangle extends Shape {

    private final DoubleProperty x = geometry("x", 0);
    private final DoubleProperty y = geometry("y", 0);
    private final DoubleProperty width = geometry("width", 0);
    private final DoubleProperty height = geometry("height", 0);
    private final DoubleProperty arcWidth = geometry("arcWidth", 0);
    private final DoubleProperty arcHeight = geometry("arcHeight", 0);

    /// Creates an empty rectangle at the origin.
    public Rectangle() {
    }

    /// Creates a rectangle of a size at the origin.
    public Rectangle(double width, double height) {
        this.width.set(width);
        this.height.set(height);
    }

    /// Creates a rectangle of a size at the origin with a fill.
    public Rectangle(double width, double height, Paint fill) {
        this.width.set(width);
        this.height.set(height);
        fillProperty().set(fill);
    }

    /// Creates a rectangle at a position.
    public Rectangle(double x, double y, double width, double height) {
        this.x.set(x);
        this.y.set(y);
        this.width.set(width);
        this.height.set(height);
    }

    /// Returns the x of the top left corner.
    public final double getX() {
        return x.get();
    }

    /// Sets the x of the top left corner.
    public final void setX(double value) {
        x.set(value);
    }

    /// The x of the top left corner.
    public final DoubleProperty xProperty() {
        return x;
    }

    /// Returns the y of the top left corner.
    public final double getY() {
        return y.get();
    }

    /// Sets the y of the top left corner.
    public final void setY(double value) {
        y.set(value);
    }

    /// The y of the top left corner.
    public final DoubleProperty yProperty() {
        return y;
    }

    /// Returns the width.
    public final double getWidth() {
        return width.get();
    }

    /// Sets the width.
    public final void setWidth(double value) {
        width.set(value);
    }

    /// The width.
    public final DoubleProperty widthProperty() {
        return width;
    }

    /// Returns the height.
    public final double getHeight() {
        return height.get();
    }

    /// Sets the height.
    public final void setHeight(double value) {
        height.set(value);
    }

    /// The height.
    public final DoubleProperty heightProperty() {
        return height;
    }

    /// Returns the horizontal diameter of the corners.
    public final double getArcWidth() {
        return arcWidth.get();
    }

    /// Sets the horizontal diameter of the corners.
    public final void setArcWidth(double value) {
        arcWidth.set(value);
    }

    /// The horizontal diameter of the corners.
    public final DoubleProperty arcWidthProperty() {
        return arcWidth;
    }

    /// Returns the vertical diameter of the corners.
    public final double getArcHeight() {
        return arcHeight.get();
    }

    /// Sets the vertical diameter of the corners.
    public final void setArcHeight(double value) {
        arcHeight.set(value);
    }

    /// The vertical diameter of the corners.
    public final DoubleProperty arcHeightProperty() {
        return arcHeight;
    }

    private static FxPath build(double x, double y, double w, double h, double radiusX, double radiusY) {
        FxPath p = new FxPath();
        p.addRoundRect(x, y, w, h, radiusX, radiusY);
        return p;
    }

    @Override
    protected FxPath cn1CreatePath() {
        return build(getX(), getY(), Math.max(0, getWidth()), Math.max(0, getHeight()), getArcWidth() / 2,
                getArcHeight() / 2);
    }

    @Override
    FxPath offsetOutline(double distance) {
        double w = Math.max(0, getWidth()) + 2 * distance;
        double h = Math.max(0, getHeight()) + 2 * distance;
        if (!(w > 0) || !(h > 0)) {
            return null;
        }
        boolean round = getArcWidth() > 0 && getArcHeight() > 0;
        return build(getX() - distance, getY() - distance, w, h, round ? getArcWidth() / 2 + distance : 0,
                round ? getArcHeight() / 2 + distance : 0);
    }
}
