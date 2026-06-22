/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps;

/// A geodesic circle drawn on a map, described by a center and a radius in
/// meters. Add one through [MapSurface#addCircle(Circle)].
public final class Circle extends MapObject {

    private LatLng center;
    private double radiusMeters;
    private int fillColor = 0x402196f3;
    private int strokeColor = 0x2196f3;
    private int strokeWidth = 2;
    private boolean visible = true;

    /// Creates a circle.
    ///
    /// #### Parameters
    ///
    /// - `center`: the geographic center
    ///
    /// - `radiusMeters`: the radius in meters
    public Circle(LatLng center, double radiusMeters) {
        this.center = center;
        this.radiusMeters = radiusMeters;
    }

    /// The circle center.
    public LatLng getCenter() {
        return center;
    }

    /// Moves the circle center.
    public Circle setCenter(LatLng center) {
        this.center = center;
        return this;
    }

    /// The radius in meters.
    public double getRadiusMeters() {
        return radiusMeters;
    }

    /// Sets the radius in meters.
    public Circle setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
        return this;
    }

    /// The fill color as 0xAARRGGBB (alpha in the high byte).
    public int getFillColor() {
        return fillColor;
    }

    /// Sets the fill color as 0xAARRGGBB (alpha in the high byte).
    public Circle setFillColor(int fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /// The stroke color as 0xRRGGBB.
    public int getStrokeColor() {
        return strokeColor;
    }

    /// Sets the stroke color as 0xRRGGBB.
    public Circle setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /// The stroke width in pixels (0 hides the outline).
    public int getStrokeWidth() {
        return strokeWidth;
    }

    /// Sets the stroke width in pixels (0 hides the outline).
    public Circle setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /// Whether the circle is rendered.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the circle.
    public Circle setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
