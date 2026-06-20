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

import java.util.ArrayList;
import java.util.List;

/// A filled, optionally stroked polygon drawn on a map. The vertices
/// describe the outer ring; the ring is implicitly closed. Add one through
/// [MapSurface#addPolygon(Polygon)].
public final class Polygon extends MapObject {

    private final List points;
    private int fillColor = 0x402196f3;
    private int strokeColor = 0x2196f3;
    private int strokeWidth = 2;
    private boolean visible = true;

    /// Creates an empty polygon; append outer-ring vertices with
    /// [#addPoint(LatLng)].
    public Polygon() {
        points = new ArrayList();
    }

    /// Creates a polygon with the supplied outer-ring vertices.
    public Polygon(LatLng[] pts) {
        points = new ArrayList();
        if (pts != null) {
            for (int i = 0; i < pts.length; i++) {
                points.add(pts[i]);
            }
        }
    }

    /// Appends an outer-ring vertex.
    public Polygon addPoint(LatLng point) {
        points.add(point);
        return this;
    }

    /// The live list of outer-ring vertices ([LatLng]).
    public List getPoints() {
        return points;
    }

    /// The fill color as 0xAARRGGBB (alpha in the high byte).
    public int getFillColor() {
        return fillColor;
    }

    /// Sets the fill color as 0xAARRGGBB (alpha in the high byte).
    public Polygon setFillColor(int fillColor) {
        this.fillColor = fillColor;
        return this;
    }

    /// The stroke color as 0xRRGGBB.
    public int getStrokeColor() {
        return strokeColor;
    }

    /// Sets the stroke color as 0xRRGGBB.
    public Polygon setStrokeColor(int strokeColor) {
        this.strokeColor = strokeColor;
        return this;
    }

    /// The stroke width in pixels (0 hides the outline).
    public int getStrokeWidth() {
        return strokeWidth;
    }

    /// Sets the stroke width in pixels (0 hides the outline).
    public Polygon setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        return this;
    }

    /// Whether the polygon is rendered.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the polygon.
    public Polygon setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
}
